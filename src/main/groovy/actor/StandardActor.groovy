package actor

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Function

@Slf4j
class StandardActor extends AbstractVerticle implements Actor {

    private Optional<String> name = Optional.of ("Un-Named")
    //private address = "actor.${->getName()}".toString()
    String deploymentId = ""

    List<MessageConsumer> consumers = []

    Closure action = {
        log.info "Actor.action invoked with $it"

        "actionReturn : $it"
    }

    String getAddress () {
        "actor.${->getName()}".toString()
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
        this.action = (Closure) action?.clone()
        this.action?.delegate = this

    }

    StandardActor (Function actionFunction) {
        assert action
        //todo is this reasonable
        this.action = actionFunction as Closure
        this.action?.delegate = this

    }

    StandardActor (String name,  Closure action) {
        assert action
        this.name = Optional.ofNullable(name)
        this.action = action?.clone()
        this.action?.delegate = this
   }

    //verticle start and stop methids
    void start(Promise<Void> promise) {

        log.debug "start: register listeners on [$address]"

        //see page 56
        consumers << vertx.eventBus().<JsonObject>consumer (getAddress(), this::reply )
        consumers << vertx.eventBus().<JsonObject>consumer (getAddress(), this::executeAction )

        promise?.complete()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: # of consumers is currently ${consumers.size()},  unregister all the listeners on $address"

        consumers.each {it.unregister()}
        consumers.clear()

        promise?.complete()

    }

    def publish (def args, DeliveryOptions options=null) {
        log.debug ("publish: [$args] sent to [${address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().publish(address, argsMessage, options ?: new DeliveryOptions ())
    }

    def sendAndReply (def args, DeliveryOptions options = null) {
        log.debug ("send&reply: [$args] sent to [${address}]")

        BlockingQueue results = new LinkedBlockingQueue()

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        //vertx.eventBus().request("actor.${getName()}", argsMessage, options ?: new DeliveryOptions() , this::reply)
        //see page 59
        //do a request & reply cycle
        vertx.eventBus().request(address, argsMessage, options ?: new DeliveryOptions(), {reply ->
            if (reply.succeeded()) {
                results.put (reply.result().body())
            } else {
                results.put (reply.cause().message)
            }
        })

        //blocking wait for result
        def result = results.take()
    }

    EventBus send (def args, DeliveryOptions options = null) {
        log.debug ("send: [$args] sent to [${address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().send(address, argsMessage, options ?: new DeliveryOptions())
    }

    /**
     * expecting to reply on specific auto generated back address to the sender with reply() method
     * @param message
     */
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

        JsonObject json = new JsonObject ()
        json.put ("reply", result.toString())

        log.info ("reply: replying with  [$json] to reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")

        message.reply (json)
    }

    /**
     * invoked action from a send but no reply
     *
     * @param message
     */
    void executeAction (Message<JsonObject> message) {

        JsonObject body = message.body()
        Map bodyMap = body.getMap()

        log.info ("executeAction: got message with body $body and isSend() set to : ${message.isSend()}")

        def args = body.getString("args")

        Future future

        //cant use as i want to use the args, //vertx.executeBlocking(this::executeBlocking, this::blockingResultHandler)

        if (action.maximumNumberOfParameters == 0) {
            future = vertx.executeBlocking({ it.complete(action()) }, this::blockingResultHandler)
        } else if (action.maximumNumberOfParameters == 1) {
            future = vertx.executeBlocking({ it.complete(action(args)) }, this::blockingResultHandler)
        } else if (action.maximumNumberOfParameters > 1) {
            future = vertx.executeBlocking({ it.complete(action(*args)) }, this::blockingResultHandler)
        }

    }

    /**
     * if not required to take any parameters use vertx.executeBlocking(this::executeBlocking, this::blockingResultHandler)
     * @param promise
     */
    void executeBlocking (Promise promise) {

        def result

        //todo at the mo this is blocking on the main thread
        try {
            promise.complete(action())
        } catch (Throwable ex) {
            promise.fail (ex)
        }
    }

    void blockingResultHandler (AsyncResult ar) {
        if (ar.succeeded()){
            JsonObject json = new JsonObject ()
            json.put ("blockingResultHandler result", ar.result().toString())

            log.info ("blockingResultHandler: result of blocking action as json is  [$json]")

        } else {
            log.debug ("blockingResultHandler: result of blocking action failed with   [${ar.cause().message}]")

        }
    }
}
