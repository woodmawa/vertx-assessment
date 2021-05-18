package actor

import groovy.util.logging.Slf4j
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Verticle
import io.vertx.core.VertxOptions
import io.vertx.core.Future

import java.util.concurrent.ConcurrentHashMap

@Slf4j
class Actors {

    static Vertx vertx

    static vertx() {
        if (!vertx) {
            throw new ExceptionInInitializerError("Actors context has not been initialised, use localInit() or clusteredInit() first  ")
        }
        vertx
    }

    static Future clusterInit () {
        VertxOptions clusterOptions = new VertxOptions()

        //todo read some options from the environment here
        Promise clusterStartPromise = Promise.promise()
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

        clusterStartPromise.future()
    }

    static void localInit () {
        vertx = Vertx.vertx()
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
