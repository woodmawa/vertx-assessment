package actor

import groovy.util.logging.Slf4j
import io.vertx.core.Vertx
import io.vertx.core.Verticle

@Slf4j
class Actors {

    static Vertx vertx =  Vertx.vertx()

    static Actor actor (Closure action=null) {
        StandardActor actor = new StandardActor (action)
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx.deployVerticle(v, {ar ->
            if (ar.succeeded()) {
                def result = ar.result()
                log.info ("actor(): started verticle $this and got id $result")
                actor.deploymentId = result

                //whoopee
            } else {
                log.debug ("actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }
}
