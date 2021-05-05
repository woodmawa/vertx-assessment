package actor

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject

import java.util.function.Function

@Slf4j
class StandardActor extends AbstractVerticle implements Actor {

    private Optional<String> name = Optional.of ("Un-Named")
    private address = "actor.${getName()}"

    List<MessageConsumer> consumers = []

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
        consumers << vertx.eventBus().<JsonObject>consumer (address, this::reply )

        promise.complete()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: unregister any listeners on 'actor.$name'"

        consumers.each {it.unregister()}

        promise.complete()

    }

    def publish (def args, DeliveryOptions options=null) {
        vertx.eventBus().publish("address", args, options ?: new DeliveryOptions ())
    }

    def sendAndReply (def args, DeliveryOptions options = null) {
        vertx.eventBus().request("actor.${getName()}", args, options ?: new DeliveryOptions(), this::reply)
    }

    def send (def args, DeliveryOptions options = null) {
        vertx.eventBus().send("address", "message", options ?: new DeliveryOptions())
    }


    void reply (Message<JsonObject> message) {
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
