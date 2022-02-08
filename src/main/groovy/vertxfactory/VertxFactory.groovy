package vertxfactory

import actor.ClusteredStartupCondition
import actor.LocalStartupCondition
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import jakarta.inject.Named

@Factory
@Slf4j
class VertxFactory {

    static Vertx vertx

    static Future futureServer

    /*
     * incase in clustered mode we have to check if the future succeeded or not
     * if not complete yet - just return the pending future
     */
    /*@Bean
    Future createVertx() {
        if (futureServer == null && !vertx) {
            throw new ExceptionInInitializerError("Actors context has not been initialised, use localInit() or clusteredInit() first  ")
        } else if (futureServer.isComplete()){
            if (futureServer.succeeded()) {
                //ok to return the vertx
                return vertx
            } else {
                //if failed return the cause
                return futureServer.cause()
            }
        } else
            futureServer
    }*/

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
        Promise startPromise = Promise.promise()

        vertx = Vertx.vertx()
        startPromise.complete(vertx)
        log.info ("local vertx started successfully")

        futureServer = startPromise.future()
    }

}
