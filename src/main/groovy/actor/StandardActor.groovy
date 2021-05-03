package actor

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

import java.util.function.Function

@Slf4j
class StandardActor extends AbstractVerticle implements Actor {

    private Optional<String> name = Optional.of ("Un-Named")
    private address = "actor.${getName()}"

    Closure action = {
        log.info "Actor.action invoked with $it"
    }

    String getName () {
        name.orElse("Un-Named")
    }

    void setName (String name) {
        this.name = Optional.ofNullable(name)
    }

    //constructor
    StandardActor (Closure action) {
        assert action
        this.action = action
    }

    StandardActor (Function actionFunction) {
        assert action
        //todo is this reasonable
        this.action = actionFunction as Closure
    }

    void start(Promise<Void> promise) {

        log.debug "start: register listener on 'actor.$name'"
        vertx.eventBus().<JsonObject>consumer (address, this::reply )

        promise.complete()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: unregister listener on 'actor.$name'"

    }

    def sendAndReply (def args) {
        vertx.eventBus().request("actor.${getName()}", args, this::reply)
    }

    private void reply (Message<JsonObject> message) {
        JsonObject body = message.body()
        Map bodyMap = body.getMap()

        if (action.maximumNumberOfParameters == 0) {
            message.reply(action())
            return
        } else if (action.maximumNumberOfParameters == 1) {
            message.reply (action (bodyMap))
            return
        } else if (action.maximumNumberOfParameters > 1) {
            message.reply (action (*body))
            return
        }

    }
}
