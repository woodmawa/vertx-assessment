package actor

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject

@Slf4j
class StandardActor extends AbstractVerticle implements Actor {

    String name

    StandardActor () {

    }

    void start(Promise<Void> promise) {

        log.debug "start: register listener on 'actor.$name'"
        vertx.eventBus().<JsonObject>consumer ("actor.$name", this::getRecord )

        promise.complete()
    }

    void stop (Promise<Void> promise) {
        
    }
}
