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
    String deploymentId = ""

    List<MessageConsumer> consumers = []

    Closure action = {
        log.info "Actor.action invoked with $it"

        "actionReturn : $it"
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

        log.debug "start: register listener on [actor.${getName()}]"

        consumers << vertx.eventBus().<JsonObject>consumer (address, this::reply )

        promise?.complete()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: unregister any listeners on 'actor.$name'"

        consumers.each {it.unregister()}

        promise?.complete()

    }

    def publish (def args, DeliveryOptions options=null) {
        vertx.eventBus().publish("address", args, options ?: new DeliveryOptions ())
    }

    def sendAndReply (def args, DeliveryOptions options = null) {
        log.debug ("send&reply: [$args] sent to [$address]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        vertx.eventBus().request("actor.${getName()}", argsMessage, options ?: new DeliveryOptions() ) /*, this::reply)*/
        //returns eventBus
    }

    def send (def args, DeliveryOptions options = null) {
        vertx.eventBus().send("address", "message", options ?: new DeliveryOptions())
    }

    void reply (Message<JsonObject> message) {

        JsonObject body = message.body()
        Map bodyMap = body.getMap()

        log.info ("reply: got message with body $body")

        def args = body.getString("args")

        def result = void

        if (action.maximumNumberOfParameters == 0) {
            result = action()
        } else if (action.maximumNumberOfParameters == 1) {
            result = action (args)
        } else if (action.maximumNumberOfParameters > 1) {
            result = action (*args)
        }

        JsonObject replMessage = new JsonObject ()
        replMessage.put ("reply", result.toString())

        log.info ("reply: replying with  [$replMessage] to reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")

        message.reply (replMessage)
    }
}
