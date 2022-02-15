package com.softwood.vertxfactory


import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.inject.qualifiers.Qualifiers
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

/**
 * factory class to conditionally generate either a local, or clustered
 * vertx based on env state
 * this is used by the Actors vertx for creating new actors
 *
 */
@Factory
@Slf4j
class VertxFactory {

    @Inject ApplicationContext context
    @Inject ConfigObject appConfig

    static Vertx vertx
    static Future<Vertx> futureServer
    static ServerMode serverMode = ServerMode.Indeterminate

    /*
     * incase in clustered mode we have to check if the future succeeded or not
     * if not complete yet - just return the pending future
     */
    static Optional<Vertx> vertx() {
        ApplicationContext ctx = ApplicationContext.run()
        vertx(ctx)
    }

    static Optional<Vertx> vertx(ApplicationContext ctx) {

        //force lookup to generate the bean
        Future<Vertx> fv = ctx.getBean (Future<Vertx>,Qualifiers.byName("Vertx"))
        ConfigObject appConf = ctx.getBean (ConfigObject)

        if (futureServer == null) {
            futureServer = fv
        }

         if (futureServer.isComplete()){
            if (futureServer.succeeded()) {
                //ok to return the vertx
                if (appConf.actor.framework.serverMode == 'local')
                    serverMode = ServerMode.Local
                return Optional.of (vertx)
            } else (futureServer.failed()) {
                serverMode = ServerMode.Indeterminate
                //if failed return the cause
                log.error ("clustered vertx failed to start successfully, ${futureServer.cause().message} ")
                return Optional.empty()
            }
         } else {
            Future fut = futureServer.onComplete { ar -> Optional.of(ar.result()) }
                    .onFailure { println "failed ${it.cause().message}" }
            Optional.ofNullable(fut.otherwise(null))
         }
    }



    //shutdown hook when the vertx is closed
    static void vertxShutdown() {
        log.info "vertxShutdown: responding to vertx closure "
        serverMode = ServerMode.Shutdown
    }

    static void vertxShutdown(Closure closing) {
        log.info "vertxShutdown: responding to vertx closure, call closure  "
        serverMode = ServerMode.Shutdown
        Closure closWork = closing.clone()
        closWork.delegate = this

        closWork()
    }

    //micronaught expects instance methods when using the StartupCondition as shown
    @Bean
    @Singleton
    @Named ('Vertx')
    @Requires(condition = ClusteredStartupCondition)
    Future<Vertx> clusterInit () {

        if (futureServer) {
            log.info "clustered vertx already being started, returning its Future "
            return futureServer
        }

        Map voptions = appConfig?.framework.vertxOptions ?: [:]
        VertxOptions clusterOptions = new VertxOptions(voptions)

        //todo read some options from the environment here
        Promise clusterStartPromise = Promise.promise()
        //this takes some time to start - dont block and wait for callback
        Vertx.clusteredVertx(clusterOptions, ar -> {
            if (ar.succeeded()) {
                vertx = ar.result()
                vertx.addShutdownHook (this::vertxShutdown)

                clusterStartPromise.complete(vertx)
                serverMode = ServerMode.Clustered
                log.info ("clusterInit: clustered vertx started successfully")
            } else {
                clusterStartPromise.fail(ar.cause())
                log.error("clusterInit: couldn't start clustered vertx, reason : ${ar.cause().message}")
            }
        })

        futureServer = clusterStartPromise.future()
    }

    @Bean
    @Singleton
    @Named ('Vertx')
    @Requires(condition = LocalStartupCondition)
    Future<Vertx> localInit () {

        if (futureServer) {
            log.info "local vertx already being started, returning its Future "
            return futureServer
        }

        Map voptions = appConfig?.framework.vertxOptions ?: [:]
        VertxOptions clusterOptions = new VertxOptions(voptions)

        Promise startPromise = Promise.promise()

        vertx = Vertx.vertx()
        vertx.addShutdownHook (this::vertxShutdown)

        startPromise.complete(vertx)
        serverMode = ServerMode.Local

        log.info ("localInit: local vertx started successfully")

        futureServer = startPromise.future()
    }

}

enum ServerMode {
    Local,
    Clustered,
    Shutdown,
    Indeterminate
}