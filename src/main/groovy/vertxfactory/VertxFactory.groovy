package vertxfactory

import actor.ClusteredStartupCondition
import actor.LocalStartupCondition
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

@Factory
@Slf4j
class VertxFactory {

    @Inject ApplicationContext context

    static Vertx vertx

    static Future futureServer

    /*
     * incase in clustered mode we have to check if the future succeeded or not
     * if not complete yet - just return the pending future
     */
    //@Bean
    Optional<Vertx> vertx() {

        //force lookup to generate the bean
        //Future<Vertx> fv = context.getBean (Future<Vertx>,Qualifiers.byName("Vertx"))

        //fv
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

    //micronaught expects instance methods when using the StartupCondition as shown
    @Bean
    @Named ('Vertx')
    @Requires(condition = ClusteredStartupCondition)
    Future<Vertx> clusterInit () {
        VertxOptions clusterOptions = new VertxOptions()

        //todo read some options from the environment here
        Promise clusterStartPromise = Promise.promise()
        //this takes some time to start - dont block and wait for callback
        Vertx.clusteredVertx(clusterOptions, ar -> {
            if (ar.succeeded()) {
                 vertx = ar.result()
                clusterStartPromise.complete(vertx)
                log.info ("clustered vertx started successfully")
            } else {
                log.error("couldn't start clustered vertx, reason : ${ar.cause().message}")
                clusterStartPromise.fail(new RuntimeException("clusteredVertx failed to start with ${ar.cause().message}"))
            }
        })

        futureServer = clusterStartPromise.future()
    }

    @Bean
    @Named ('Vertx')
    @Requires(condition = LocalStartupCondition)
    Future<Vertx> localInit () {
        VertxOptions clusterOptions = new VertxOptions()

        Promise startPromise = Promise.promise()

        vertx = Vertx.vertx()
        startPromise.complete(vertx)
        log.info ("local vertx started successfully")

        futureServer = startPromise.future()
    }

}
