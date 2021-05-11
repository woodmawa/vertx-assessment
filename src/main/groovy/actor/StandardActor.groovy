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
import java.util.function.Consumer
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
        //consumers << vertx.eventBus().<JsonObject>consumer (getAddress(), this::reply )
        consumers << vertx.eventBus().<JsonObject>consumer (getAddress(), this::executeAction )

        promise?.complete()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: # of consumers is currently ${consumers.size()},  unregister all the listeners on $address"

        consumers.each {it.unregister()}
        consumers.clear()

        promise?.complete()

    }

    void addConsumer (Address from, Closure consumer) {
        consumers << vertx.eventBus().consumer (from.address, consumer)
    }

    void addConsumer (Consumer consumer) {
        consumers << vertx.eventBus().consumer()
    }

    boolean removeConsumer (consumer) {
        consumers.remove(consumer as MessageConsumer)
    }

    def publish (def args, DeliveryOptions options=null) {
        publish (new Address (this.getAddress()), args, options)
    }

    def publish (Address postTo, def args, DeliveryOptions options=null) {
        log.debug ("publish: [$args] sent to [${postTo.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().publish(postTo.address, argsMessage, options ?: new DeliveryOptions ())
    }

    def sendAndReply (def args, DeliveryOptions options = null) {
        log.debug ("send&reply: [$args] sent to [${address}]")

        BlockingQueue results = new LinkedBlockingQueue()

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        // see also https://github.com/vert-x3/wiki/wiki/RFC:-Future-API

        //use new promise/future model - resuest is expecting a message.reply()
        Future response = vertx.eventBus().request(address, argsMessage, options ?: new DeliveryOptions())

        //get response and add to blocking Queue
        response.onComplete(ar -> {
            println "in requests, response handler with [${ar.result().body()}"
            results.put (ar.result().body())
        })

        //blocking wait for result to become available then return it
        def result = results.take()
    }

    // can be chained
    EventBus send (def args, DeliveryOptions options = null) {

        send (new Address(this.getAddress()), args, options)

    }

    EventBus send (Address postTo, def args, DeliveryOptions options = null) {
        assert postTo
        log.debug ("send: [$args] sent to [${postTo.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().send(address, argsMessage, options ?: new DeliveryOptions())
    }

    /**
     * invoked action from a send but no reply
     *
     * @param message
     */
    void executeAction(Message<JsonObject> message) {

        JsonObject body = message.body()
        Map bodyMap = body.getMap()

        log.info ("executeAction: got message with body $body and isSend() set to : ${message.isSend()}")

        def args = body.getString("args")

        //closure that executes the action closure and stores the result in the Promise
        //using a closure as need to reference the args in context, as executeBlocking only passes a Promise as arg
        Closure doBlocking = {Promise<Object> promise ->
            try {
                def result
                if (action.maximumNumberOfParameters == 0) {
                    result = action()
                } else if (action.maximumNumberOfParameters == 1) {
                    result = action(args)
                } else if (action.maximumNumberOfParameters > 1) {
                    result = action(*args)
                }
                //println "in doBlocking closure,  returning promise with $result"
                promise.complete (result)
            } catch (Throwable ex) {
                promise.fail(ex)
            }
        }

        vertx.executeBlocking(doBlocking)
        .onComplete(ar -> {

            JsonObject json = new JsonObject()
            if (ar.succeeded()) {
                if (message.replyAddress() && message.isSend()) {
                    def result = ar.result()
                    json.put("reply", result.toString())

                    message.reply(json)
                    log.info("executeAction(): replying with  [$json] to reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")

                } else  {
                    def result = ar.result()
                    json.put("no return, with", result.toString())

                    log.info("executeAction(): got  [$json] from action()  and reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")

                }
            } else {
                json.put("Exception", ar.cause().message)
                message.reply(json)
            }
        })
    }


}
