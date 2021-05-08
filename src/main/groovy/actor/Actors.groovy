package actor

import groovy.util.logging.Slf4j
import io.vertx.core.Vertx
import io.vertx.core.Verticle

@Slf4j
class Actors {
    static Actor actor (Closure action=null) {
        StandardActor actor = new StandardActor (action)
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        Vertx.vertx().deployVerticle(v, {complHandler ->
            if (complHandler.succeeded()) {
                def result = complHandler.result()
                log.info ("started verticle $this and got id $result")
                actor.deploymentId = result
                //whoopee
            } else {
                log.debug ("deployVerticle $this encountered a problem ${complHandler.cause().message}")
            }
        })

        actor
    }
}
