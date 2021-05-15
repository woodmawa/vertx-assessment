package actor

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import org.codehaus.groovy.runtime.MethodClosure

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream

@Slf4j
class StandardActor extends AbstractVerticle implements Actor {

    private Optional<String> name = Optional.of ("Un-Named")
    //private address = "actor.${->getName()}".toString()
    String deploymentId = ""

    List<MessageConsumer> consumers = []

    def defaultAction (def args) {
        log.info "default untyped args Actor.action invoked with $args"

        args
    }

    def defaultAction (Integer arg) {
        log.info "default Integer Actor.action invoked with $arg"

        arg
    }

    def defaultAction (BigInteger arg) {
        log.info "default BigInteger Actor.action invoked with $arg"

        arg
    }

    def defaultAction (Float arg) {
        log.info "default Float Actor.action invoked with $arg"

        arg
    }

    def defaultAction (Double arg) {
        log.info "default Double Actor.action invoked with $arg"

        arg
    }

    def defaultAction (BigDecimal arg) {
        log.info "default BigDecimal Actor.action invoked with $arg"

        arg
    }

    def defaultAction (String arg) {
        log.info "default String Actor.action invoked with $arg"

        arg
    }

    //dynamic dispatch logic handled by groovy
    MethodClosure action = this::defaultAction

    //each actor has a default message bus address which is "actor.<name>"
    String getAddress () {
        "actor.${->getName()}".toString()
    }

    String getName () {
        name.orElse("Un-Named")
    }

    void setName (String name) {
        this.name = Optional.ofNullable(name)
    }

    //constructors

    StandardActor (String name ) {
        this.name = Optional.ofNullable(name)
    }

    StandardActor (Closure action) {
        assert action
        Closure clone = (Closure) action.clone()
        clone.delegate = this

        MethodClosure mc = new MethodClosure (clone, "call")
        mc.delegate = this

        this.action = mc
    }

    StandardActor (String name,  Closure action) {
        assert action
        this.name = Optional.ofNullable(name)

        Closure clone = action.clone()
        clone.delegate = this

        //have to set the delegate of the closure 'action' before we create the MethodClosure as this just invokes the former via the call() method
        //so if delegate is not set it uses the initial and not the required context as the delegate for resolving methods/variables
        MethodClosure mc = new MethodClosure (clone, "call")
        mc.delegate = this
        this.action = mc
   }

    // code that takes 1 arg, and returns a result
    StandardActor (Function actionFunction) {
        assert action
        //todo is this reasonable

        MethodClosure mc = new MethodClosure (actionFunction, "apply")
        mc.delegate = this

        this.action = mc
    }

    // code that just returns a result, takes no args
    StandardActor (Supplier actionAsSupplier) {
        assert action
        //todo is this reasonable

        MethodClosure mc = new MethodClosure (actionAsSupplier, "get")
        mc.delegate = this

        this.action = mc
    }


    //verticle start and stop methods
    void start(Promise<Void> promise) {

        log.debug "start: register listeners on [$address]"

        //see page 56
        //register the default listener for the default address
        consumers << vertx.eventBus().<JsonObject>consumer (getAddress(), this::executeAction )

        promise?.complete()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: # of consumers is currently ${consumers.size()},  unregister all the listeners "

        consumers.each {it.unregister()}
        consumers.clear()

        promise?.complete()

    }

    MessageConsumer addConsumer (Address from, consumer) {
       MessageConsumer mc = vertx.eventBus().consumer (from.address, consumer) //consumer as Closure)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( consumer) {
        MessageConsumer mc = vertx.eventBus().consumer (new Address (this::getAddress()), consumer)
        consumers << mc
        mc
    }

    boolean removeConsumer (consumer) {
        consumers.remove(consumer as MessageConsumer)
    }

    boolean removeAllConsumersFromAddress (Address postTo) {
        removeAllConsumersFromAddress (postTo.address)
    }

    boolean removeAllConsumersFromAddress (String address) {
        def matched = consumers.findAll {it.address() == address}
        matched.each {it.unregister()}
        consumers.removeAll(matched)
     }


    //runOnVertx context
    void run (code) {
        Context ctx = vertx.getOrCreateContext()
        Future future = ctx.executeBlocking(code)

        future.onSuccess((arg) -> "println completed run with [$arg]; arg")
                //.onFailure((ex) -> "println run failed with $ex.message")
        assert future.isComplete()
    }


    //post and publish actions can be chained on the returned event bus
    EventBus post (def args, DeliveryOptions options=null) {
        publish (new Address (this.getAddress()), args, options)
    }

    EventBus post (Address postTo, def args, DeliveryOptions options=null) {
        publish (postTo, args, options)
    }

    EventBus publish (def args, DeliveryOptions options=null) {
        publish (new Address (this.getAddress()), args, options)
    }

    EventBus publish (Address postTo, def args, DeliveryOptions options=null) {
        log.debug ("publish: [$args] sent to [${postTo.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().publish(postTo.address, argsMessage, options ?: new DeliveryOptions ())
    }

    def requestAndReply(def args, DeliveryOptions options = null) {
        log.debug ("request&reply: [$args] sent to [${address}]")

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

    /**
     * returns a future which can be chained with onSuccess() and onError() actions
     * @param args
     * @param options
     * @return  vertx Future
     */
    Future requestAndAsyncReply (def args, DeliveryOptions options = null) {
        log.debug ("request&reply: [$args] sent to [${address}]")

        BlockingQueue results = new LinkedBlockingQueue()

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        // see also https://github.com/vert-x3/wiki/wiki/RFC:-Future-API

        //use new promise/future model - resuest is expecting a message.reply()
        Future response = vertx.eventBus().request(address, argsMessage, options ?: new DeliveryOptions())
    }

    /**
     * post a message using groovy << operator
     * @param args
     * @param options
     * @return
     */
    EventBus leftShift (def args, DeliveryOptions options = null) {
        send (args, options)
    }

    EventBus leftShift (Stream streamOfArgs, DeliveryOptions options = null) {

        streamOfArgs.forEach(this::send)
    }

    /**
     * effectivel calls async request, and when reply comes back invokes the appropriate handler
     * @param args
     * @param onComplete
     * @param onFail
     * @return
     */
    Future<Void> rightShift (def args, Closure onComplete, Closure onFail) {
        Future future = requestAndAsyncReply(args)
        .onSuccess(onComplete)  //expects the completed value
        .onFailure(onFail)  //expects a throwable
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

        vertx.eventBus().send(postTo.address, argsMessage, options ?: new DeliveryOptions())
    }


    /**
     * invoked action from a send but no reply
     *
     * @param message
     */
    void executeAction(Message<JsonObject> message) {

        JsonObject body = message.body()
        Map bodyMap = body.getMap()

        log.debug ("executeAction: got message with body $body and isSend() set to : ${message.isSend()}")

        def args = bodyMap.args

        //closure that executes the action closure and stores the result in the Promise
        //using a closure as need to reference the args in context, as executeBlocking only passes a Promise as arg
        Closure doBlockingAction = {Promise<Object> promise ->
            try {
                def result
                def actDelegate = action.delegate
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

        vertx.executeBlocking(doBlockingAction)
        .onComplete(ar -> {
            JsonObject json = new JsonObject()
            if (ar.succeeded()) {
                if (message.replyAddress() && message.isSend()) {
                    def result = ar.result()
                    json.put("reply", result.toString())

                    message.reply (json)
                    log.debug("executeAction(): replying with  [$json] to reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")

                } else  {
                    def result = ar.result()
                    json.put("no return, with", result.toString())

                    log.debug("executeAction(): got  [$json] from action()  and reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")

                }
            } else {
                json.put("Exception", ar.cause().message)
                message.reply(json)
            }
        })
    }

    void reply (args) {
        assert args
        JsonObject json = new JsonObject()

        if (args.size > 1) {

        }

    }

}
