package actor

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import org.codehaus.groovy.runtime.MethodClosure

import java.time.Duration
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

    def onMessage (def args) {
        log.info "default untyped args Actor.action invoked with $args"

        args
    }

    def onMessage (Integer arg) {
        log.info "default Integer Actor.action invoked with $arg"

        arg
    }

    def onMessage (BigInteger arg) {
        log.info "default BigInteger Actor.action invoked with $arg"

        arg
    }

    def onMessage (Float arg) {
        log.info "default Float Actor.action invoked with $arg"

        arg
    }

    def onMessage (Double arg) {
        log.info "default Double Actor.action invoked with $arg"

        arg
    }

    def onMessage (BigDecimal arg) {
        log.info "default BigDecimal Actor.action invoked with $arg"

        arg
    }

    def onMessage (String arg) {
        log.info "default String Actor.action invoked with $arg"

        arg
    }

    //dynamic dispatch logic handled by groovy
    MethodClosure action = this::onMessage

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

    StandardActor () {}

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

    //synchronously deploy and start this actor
    void start() {
        log.debug ("${this.getClass().name}.start() : manually starting  actor  ${this.getName()} ")

        //get reference to static Actors.vertx and use that
        Vertx vertx = Actors.vertx

        // deploy action then invokes start(Promise) method asynchronously which registers the default listener
        Future future = vertx.deployVerticle(this )
        future.onComplete({ar ->
            if (ar.succeeded()) {
                this.deploymentId = ar.result()
                Actors.deployedActors.put(ar.result(), this)

                log.debug ("${this.getClass().name} actor: started actor  $this successfully and got deploymentId ${ar.result()}, #listners = ${consumers.size()}")
            } else {
                log.debug ("${this.getClass().name} actor : failed to start actor  $this, encountered a problem ${ar.cause().message}")

            }
        })
    }

    //undeploy this specific actor from the network
    void stop () {
        log.debug "stop: # of consumers is currently ${consumers.size()},  unregister all the listeners "

        consumers.each {it.unregister()}
        consumers.clear()

        Vertx vertx = Actors.vertx
        Future future = vertx.undeploy(this.deploymentId)
        future.onComplete({ar ->
            if (ar.succeeded()) {
                def undeployed = Actors.deployedActors.remove(deploymentId)
                assert undeployed == this
                log.debug ("${this.getClass().name} actor: stop actor  ${getName()}[dep:$deploymentId] successfully undeployed ")
            } else {
                log.debug ("${this.getClass().name} actor: stop actor  failed to undeploy, reason ${ar.cause().message} ")
            }
        })
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


    /**
     * run method expects a handler code block which should have 1 argument which is a Promise
     *
     * the handler should complete the promise in the code block
     *
     * this will run the block asynchronously on a worker thread
     * @param code
     */
    Actor run (code) {
        Context ctx = vertx.getOrCreateContext()
        Future future = ctx.executeBlocking(code)

         future.onComplete({ arg ->
            if (arg.succeeded()) {
                "println completed run with [${arg.result()}]"
                arg.result()
            } else {
                "println completed run failed with ${arg.cause().message}"
            }
        })
        this
    }

    /**
     * one off execution timer
     * @param Long delay in ms
     * @param Closure scheduledWork, work to do
     * @return Long timer id
     */
    long timer (long delay, Closure scheduledWork ) {
        long tid = vertx.setTimer(delay, scheduledWork)
    }

    long timer (Duration delay, Closure scheduledWork ) {
        long tid = vertx.setTimer(delay.toMillis(), scheduledWork)
    }

    /**
     * periodically repeat this action on a timer
     * @param Long delay in ms
     * @param Closure scheduledWork, work to do
     * @return Long timer id
     */
    long periodicTimer (long delay, Closure scheduledWork) {
        long tid = vertx.setPeriodic (delay, scheduledWork)
    }

    long periodicTimer (Duration delay, Closure scheduledWork) {
        long tid = vertx.setPeriodic (delay.toMillis(), scheduledWork)
    }

    boolean cancelTimer (long tid) {
        vertx.cancelTimer(tid )
    }

    //post and publish actions can be chained on the returned event bus
    Actor post (def args, DeliveryOptions options=null) {
        publish (new Address (this.getAddress()), args, options)
        this
    }

    Actor post (Address postTo, def args, DeliveryOptions options=null) {
        publish (postTo, args, options)
        this
    }

    Actor publish (def args, DeliveryOptions options=null) {
        publish (new Address (this.getAddress()), args, options)
        this
    }

    Actor publish (Address postTo, def args, DeliveryOptions options=null) {
        log.debug ("publish: [$args] sent to [${postTo.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().publish(postTo.address, argsMessage, options ?: new DeliveryOptions ())
        this
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
    Actor leftShift (def args, DeliveryOptions options = null) {
        send (args, options)
        this
    }

    Actor leftShift (Stream streamOfArgs, DeliveryOptions options = null) {

        streamOfArgs.forEach(this::send)
        this
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
    Actor send (def args, DeliveryOptions options = null) {
        send (new Address(this.getAddress()), args, options)
        this
    }

    Actor send (Address postTo, def args, DeliveryOptions options = null) {
        assert postTo
        log.debug ("send: [$args] sent to [${postTo.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().send(postTo.address, argsMessage, options ?: new DeliveryOptions())
        this
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
