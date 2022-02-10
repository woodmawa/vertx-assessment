package vertxfactory


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

@Factory
@Slf4j
class VertxFactory {

    @Inject ApplicationContext context
    @Inject ConfigObject appConfig

    static Vertx vertx
    static Future<Vertx> futureServer
    static ServerMode serverMode = ServerMode.indeterminate

    /*
     * incase in clustered mode we have to check if the future succeeded or not
     * if not complete yet - just return the pending future
     */
    //@Bean
    //@Requires(beans=[Future<Vertx>])
    static Optional<Vertx> vertx(ApplicationContext ctx) {

        //force lookup to generate the bean
        Future<Vertx> fv = ctx.getBean (Future<Vertx>,Qualifiers.byName("Vertx"))

        if (futureServer == null && !vertx) {
            throw new ExceptionInInitializerError("Actors context has not been initialised, use localInit() or clusteredInit() first  ")
        } else if (futureServer.isComplete()){
            if (futureServer.succeeded()) {
                //ok to return the vertx
                return Optional.of (vertx)
            } else (futureServer.failed()) {
                //if failed return the cause
                log.error ("clustered vertx failed to start successfully, ${futureServer.cause().message} ")
                return Optional.empty()
            }
        } else {
            Future fut = futureServer.onComplete { ar -> Optional.of(ar.result()) }
                    .onFailure { println "failed ${it.cause().message}" }
            Optional.ofNullable(fut.otherwise(void))
        }
    }

    static Optional<Vertx> vertx() {
        assert futureServer
        if (futureServer.isComplete())
            Optional.ofNullable(vertx)
        else
            Optional.empty()
    }

    //shutdown hook when the vertx is closed
    static void vertxShutdown() {
        log.info "vertxShutdown: responding to vertx closure "
        serverMode = ServerMode.indeterminate
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
                serverMode = ServerMode.clustered
                log.info ("clusterInit: clustered vertx started successfully")
            } else {
                log.error("clusterInit: couldn't start clustered vertx, reason : ${ar.cause().message}")
                clusterStartPromise.fail(new RuntimeException("clusteredVertx failed to start with ${ar.cause().message}"))
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
        serverMode = ServerMode.local

        log.info ("localInit: local vertx started successfully")

        futureServer = startPromise.future()
    }

}

enum ServerMode {
    local,
    clustered,
    indeterminate
}