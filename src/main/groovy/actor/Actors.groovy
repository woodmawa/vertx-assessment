package actor

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Verticle
import io.vertx.core.VertxOptions
import io.vertx.core.Future

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Named

import java.util.concurrent.ConcurrentHashMap

@Factory
@Slf4j
class Actors {

    static Vertx vertx

    static Future futureServer

    /*
     * incase in clustered mode we have to check if the future succeeded or not
     * if not complete yet - just return the pending future
     */
    static vertx() {
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
     }

    //micronaught expects instance methods when using the StartupCondition as shown
    @Bean
    @Named ('Actors-Vertx')
    @Requires(condition = ClusteredStartupCondition)
    Future clusterInit () {
        VertxOptions clusterOptions = new VertxOptions()

        //todo read some options from the environment here
        Promise clusterStartPromise = Promise.promise()
        //this takes some time to start - dont block and wait for callback
        Vertx.clusteredVertx(clusterOptions, ar -> {
            if (ar.succeeded()) {
                println  "clustered vertx started successfully "
                vertx = ar.result()
                clusterStartPromise.complete(vertx)
            } else {
                println("couldn't start clustered vertx, reason : ${ar.cause().message}")
                clusterStartPromise.fail(new RuntimeException("clusteredVertx failed to start with ${ar.cause().message}"))
            }
        })

        futureServer = clusterStartPromise.future()
    }

    @Bean
    @Named ('Actors-Vertx')
    @Requires(condition = LocalStartupCondition)
    Future localInit () {
        Promise startPromise = Promise.promise()

        vertx = Vertx.vertx()
        startPromise.complete(vertx)
        futureServer = startPromise.future()
    }


    static HashMap deployedActors = new ConcurrentHashMap<>()

    static List<String> activeActors () {
        vertx.deploymentIDs().collect()
    }

    /**
     * actor with name
     * @param String name, can be null
     * @param Closure action,  for action - default provided which just returns it
     * @return
     */
    static Actor actor (String name, Closure action=null) {
        StandardActor actor
        if (action)
            actor = new StandardActor (name, action)
        else
            actor = new StandardActor (name)

        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx().deployVerticle(v, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                deployedActors.put(ar.result(), actor)

                log.debug ("Actors.actor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor

    }


    static Actor actor (Closure action=null) {
        StandardActor actor = new StandardActor (action)
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx().deployVerticle(v, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                deployedActors.put(ar.result(), actor)

                log.debug ("Actors.actor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    @Bean
    //@Prototype
    Actor actorGenerator () {
        StandardActor actor = new StandardActor ()
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx().deployVerticle(v, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                deployedActors.put(ar.result(), actor)

                log.debug ("Actors.actor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    static shutdown () {
        log.debug ">>Actors.shutdown(): ${deployedActors.size()} to be shutdown "
        vertx().close(ar -> {
            if (ar.succeeded()) {
                log.debug ">>Actors.shutdown(): close handler: actor network closed successfully "
                deployedActors.clear()
            } else  {
                log.debug ">>Actors.shutdown(): close handler:couldnt close actor network,  reason ${ar.cause().message}"
            }
        })
    }
}
