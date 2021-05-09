package actor

import groovy.util.logging.Slf4j
import io.vertx.core.Vertx
import io.vertx.core.Verticle

import java.util.concurrent.ConcurrentHashMap

@Slf4j
class Actors {

    static Vertx vertx =  Vertx.vertx()
    static HashMap deployedActors = new ConcurrentHashMap<>()

    static List<String> activeActors () {
        vertx.deploymentIDs().collect()
    }

    static Actor actor (Closure action=null) {
        StandardActor actor = new StandardActor (action)
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx.deployVerticle(v, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                deployedActors.put(ar.result(), actor)

                log.info ("Actors.actor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    static shutdown () {
        vertx.close(ar -> {
            if (ar.succeeded()) {
                println ">>Actors.shutdown(): close handler: actor network closed successfully "
                deployedActors.clear()
            } else  {
                println ">>Actors.shutdown(): close handler:couldnt close actor network,  reason ${ar.cause().message}"
            }
        })
    }
}
