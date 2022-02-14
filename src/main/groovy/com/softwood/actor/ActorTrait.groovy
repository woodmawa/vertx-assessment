package com.softwood.actor

import groovy.util.logging.Slf4j
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import jakarta.annotation.PreDestroy
import org.codehaus.groovy.runtime.MethodClosure

import javax.validation.constraints.NotNull
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Slf4j
trait ActorTrait {

    protected Vertx vertx

    protected ActorState status = ActorState.Created
    protected Optional<String> name = Optional.of ("${getClass().simpleName}@${Integer.toHexString(System.identityHashCode(this)) }")
    protected String deploymentId = ""
    protected List<MessageConsumer> consumers = []
    protected MethodClosure action = { it }  //identity closure



    Vertx getVertx() {vertx}
    ActorState getStatus () {status}
    void setStatus (@NotNull ActorState state) {status = state}

    String getDeploymentId () {deploymentId}
    void setDeploymentId (@NotNull String did) {deploymentId = did }

    List<MessageConsumer> getConsumers() {consumers}


    @PreDestroy
    void finalise () {
        log.debug "finalise: remove this actor [$name] from list of deployed actors in Actors"
        Actors.removeDeployedActor (this)
    }

    //each actor has a default message bus address which is "actor.<name>"
    Address getAddress () {
        new Address(address: "actor.${->getName()}".toString())
    }

    String getName () {
        name.orElse("Un-Named")
    }

    void setName (@NotNull String name) {
        //need to reset the listener to new change of address
        log.debug "setName: number of listeners is ${consumers.size()}"
        MessageConsumer defaultConsumer = consumers.find{mc ->
            log.debug "setName: checking existing consumers for default listener"
            String defaultAddress = "actor.${owner.getClass().simpleName}@${Integer.toHexString(System.identityHashCode(this)) }".toString()
            mc.address() == defaultAddress }
        if (defaultConsumer){
            log.debug "setName: removed default listener"
            consumers.remove(defaultConsumer)
        }
        this.name = Optional.of(name)
        addConsumer(this::executeAction)

    }

    void setAction (@NotNull Closure work) {
        Closure action = work.clone()
        action.delegate = this

    }

    def getSelf () {this}

    //verticle start and stop methods - when start is running the deploymentId has not yet been generated
    Future<Void> start(Promise<Void> promise) {

        log.debug "start: register listeners on [$address]"

        //see page 56
        //register the default listener for the default address
        String address = getAddress()
        String called = getName()
        consumers << vertx.eventBus().<JsonObject>consumer (getAddress(), this::executeAction )
        status = ActorState.Running

        promise?.complete()
        promise.future()
    }

    Future<Void> stop (Promise<Void> promise) {
        log.debug "stop: # of consumers registered on address [$address] is currently ${consumers.size()},  unregister all the listeners "

        consumers.each {it.unregister()}
        consumers.clear()
        status = ActorState.Stopped

        promise?.complete()
        promise.future()

    }


    MessageConsumer addConsumer (Address from, Handler<Message<Object>> consumer) {
        log.debug "added Handler as listener on specified adrress ${from.address}"
        MessageConsumer mc = vertx.eventBus().consumer (from.address, consumer) //consumer as Closure)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( Handler<Message<Object>> consumer) {
        log.debug "added Handler as listener on address ${getAddress()}"
        MessageConsumer mc = vertx.eventBus().consumer (this::getAddress(), consumer)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( Closure<Message<Object>> consumer) {
        log.debug "added Closure as listener on address ${getAddress()}"
        MessageConsumer mc = vertx.eventBus().consumer (this::getAddress(), consumer)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( MethodClosure consumer) {
        log.debug "added MethodClosure as listener on address ${getAddress()}"

        MessageConsumer mc = vertx.eventBus().consumer (this::getAddress(), consumer)
        consumers << mc
        mc
    }

    boolean removeConsumer (MessageConsumer consumer) {
        Future<Void> fut = consumer.unregister()
        consumers.remove(consumer as MessageConsumer)
    }

    boolean removeAllConsumersFromAddress (Address postTo) {
        removeAllConsumersFromAddress (postTo.address)
    }

    boolean removeAllConsumersFromAddress (String address) {
        log.debug "removedAll listeners from address ${getAddress()}"

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
     * gets a Promise that should be written at the end of the work
     * @param code
     */
    Future run (code) {
        Context ctx = vertx.getOrCreateContext()
        Future future = ctx.executeBlocking(code)

        future.onComplete({ arg ->
            if (arg.succeeded()) {
                def result = arg.result()
                log.debug "ran code block on worker thread and returned [$result]"

            } else {
                log.debug "completed blockingCode run and failed with ${arg.cause().message}"
            }
        })

        future
    }


    /*
     * point to point send message to address
     */
    Actor send (Actor anotherActor, args, DeliveryOptions options = null) {
        send (anotherActor.address, args, options)
    }

    Actor send (Address sendTo, args, DeliveryOptions options = null) {
        //send (postTo.address, args, options)
        log.debug ("send: [$args] sent to [${sendTo.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().send(address, argsMessage, options ?: new DeliveryOptions())
        this
    }

    /*
     * pub-sub: - publish to some address
     */
    Actor publish (Actor actor, def args, DeliveryOptions options=null) {
        publish (actor.address, args, options)
    }

    Actor publish (Address postTo, def args, DeliveryOptions options=null) {
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        vertx.eventBus().publish (postTo.address, argsMessage, options ?: new DeliveryOptions())
        this
    }

    /*
     * request with reply  response : - publish to some address
     * using blocking queue to simulate synchronous RPC call
     */
    def requestAndReply(Actor actor,  args, DeliveryOptions options = null) {
        requestAndReply(actor.address, args, options)
    }

    def requestAndReply(Address address,  args, DeliveryOptions options = null) {
        log.debug ("request&reply: [$args] sent to [${address.address}]")

        BlockingQueue results = new LinkedBlockingQueue()

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        // see also https://github.com/vert-x3/wiki/wiki/RFC:-Future-API

        //use new promise/future model - resuest is expecting a message.reply()
        Future response = vertx.eventBus().request(address.address, argsMessage, options ?: new DeliveryOptions())

        //get response and add to blocking Queue
        response.onComplete(ar -> {
            log.debug "in requestsAndReply, future handler with [${ar.result().body()}]"
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

    Future requestAndAsyncReply (Actor actor, args, DeliveryOptions options = null) {
        requestAndAsyncReply (actor.address, args, options)
    }

    Future requestAndAsyncReply (Address address, args, DeliveryOptions options = null) {
        //requestAndAsyncReply (address.address, args, options)
        log.debug ("request&asyncReply: [$args] sent to [${address.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        // see also https://github.com/vert-x3/wiki/wiki/RFC:-Future-API

        //use new promise/future model - resuest is expecting a message.reply()
        Future response = vertx.eventBus().request(address.address, argsMessage, options ?: new DeliveryOptions())

    }

    /**
     * one off execution timer
     * @param Long delay in ms
     * @param Closure scheduledWork, work to do
     * @return Long timer id
     */

    Timer timer (Duration delay, Closure scheduledWork ) {
        timer (delay.toMillis(), schWorkWithPromise)

    }

    Timer timer (long delay, Closure scheduledWork ) {
        assert scheduledWork
        Promise promise = Promise.promise()

        Closure schWork = scheduledWork.clone()
        schWork.delegate = this

        Closure schWorkWithPromise = { promise.complete(schWork ()) }

        Timer timer = new Timer (tid: vertx.setTimer(delay, schWorkWithPromise ))
        timer.future = promise.future()
        timer
    }

    /**
     * periodically repeat this action on a timer
     * @param Long delay in ms
     * @param Closure scheduledWork, work to do
     * @return Long timer id
     */

    Timer periodicTimer (Duration delay, Closure scheduledWork) {
        periodicTimer (delay.toMillis(), scheduledWork)
    }

    Timer periodicTimer (long delay, Closure scheduledWork) {
        assert scheduledWork
        Promise promise = Promise.promise()

        Closure schWork = scheduledWork.clone()
        schWork.delegate = this

        //todo this may not work with repeated timer ticks
        Timer timer = new Timer (tid:vertx.setPeriodic (delay, schWork))
        timer.future = promise.future()
        timer
    }

    boolean cancelTimer (long tid) {
        vertx.cancelTimer(tid )
    }

    boolean cancelTimer (Timer timer) {
        assert timer
        long tid = timer.timerId
        //boolean cancelled = vertx.cancelTimer(tid)
        boolean cancelled = timer.cancel()
        cancelled
    }

    /**
     * invoked action from a send but no reply
     *
     * @param message
     */
    void executeAction(Message<JsonObject> message) {

        MultiMap headers = message.headers()
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

}