package com.softwood.actor

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.core.Promise
import io.vertx.core.Verticle
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
import java.util.concurrent.TimeUnit

@Slf4j
trait ActorTrait implements Verticle, Actor {

    //getVertx() is defined in Verticle interface

    ActorState getStatus () {_status}
    void setStatus (ActorState state) {_status = state}

    String getDeploymentId () {_deploymentId}
    void setDeploymentId (String did) {
        _deploymentId = did
    }

  List<MessageConsumer> getConsumers() {_consumers}


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
        _name.orElse("Un-Named")
    }

    void setName (@NotNull String name) {
        //need to reset the listener to new change of address
        log.debug "setName: number of listeners is currently  ${consumers.size()}"
        MessageConsumer defaultConsumer = consumers.find{mc ->
            log.debug "setName: checking existing consumers for default listener"
            String defaultAddress = "actor.${owner.getClass().simpleName}@${Integer.toHexString(System.identityHashCode(this)) }".toString()
            mc.address() == defaultAddress }
        if (defaultConsumer){
            log.debug "setName: removed default listener"
            consumers.remove(defaultConsumer)
            setSelfConsumer(null)
        }
        this._name = Optional.of(name)

        //if already deployed, add this as consumer for the new name
        if (getVertx()) {
            defaultConsumer = addConsumer(this::executeAction)
            setSelfConsumer(defaultConsumer)
        }
        else {
            log.info "changed name of actor to [${getName()}] to this verticle, but it is not yet deployed "
        }

    }

    Closure getAction () {
        println "abstract action returned "
        _action}

    void setAction (@NotNull Closure work) {
        log.debug "just updating the action closure for actor [${getName()}] "
        _action = work.clone()
        _action.delegate = this

    }

    def getSelf () {this}

    void setSelfConsumer(MessageConsumer selfConsumer ) {
        _selfConsumer = selfConsumer
    }

    MessageConsumer getSelfConsumer () {
        _selfConsumer
    }

    //verticle start and stop methods - when start is running the deploymentId has not yet been generated
    void start(Promise<Void> promise) {

        log.debug "start: register listeners on [${getAddress().address}]"

        //see page 56
        //register the default listener for the default address
        String address = getAddress().address
        Vertx vertx  = getVertx()
        assert vertx

        MessageConsumer consumer = getVertx().eventBus().<JsonObject>consumer (address, this::executeAction )
        setSelfConsumer(consumer)
        status = ActorState.Running

        promise?.complete()
        promise?.future()
    }

    void stop (Promise<Void> promise) {
        log.debug "stop: #(${consumers.size()}) of consumers registered on address [${getAddress().address}] is currently ${consumers.size()},  unregister all the listeners "


        consumers.each {it.unregister()}
        consumers.clear()
        status = ActorState.Stopped


        promise?.complete(null)
        promise?.future()

    }

    //remove this actor from running list of Actor.deployedActors
    void close () {
        Actors.undeploy(this)
    }

    /*
     * add a consumer for events on the bus
     */
    MessageConsumer addConsumer (Address from, Handler<Message<Object>> consumer) {
        log.debug "added Handler as listener on specified adrress ${from.address}"
        MessageConsumer mc = getVertx().eventBus().consumer (from.address, consumer) //consumer as Closure)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( Handler<Message<Object>> consumer) {
        log.debug "added Handler as listener on address ${getAddress()}"
        MessageConsumer mc = getVertx().eventBus().consumer (this::getAddress().address, consumer)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( Closure<Message<Object>> consumer) {
        log.debug "added Closure as listener on address ${getAddress()}"
        MessageConsumer mc = getVertx().eventBus().consumer (this::getAddress().address, consumer)
        consumers << mc
        mc
    }

    MessageConsumer addConsumer ( MethodClosure consumer) {
        log.debug "added MethodClosure as listener on address ${getAddress()}"

        def vertx = getVertx()
        MessageConsumer mc = getVertx().eventBus().consumer (this::getAddress().address, consumer)
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
        log.debug "removedAll listeners from address ${getAddress().address}"

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
        Context ctx = getVertx().getOrCreateContext()
        Future future = ctx.executeBlocking(code)

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

        //use the circuit breaker setup in the AbstractActor
        this.breaker.execute { Promise promise  ->
            Future result = getVertx().eventBus().send(sendTo.address, argsMessage, options ?: new DeliveryOptions())
            result.onComplete { AsyncResult ar ->
                if (ar.succeeded())  {
                    promise.complete(ar.result())
                } else {
                    promise.fail(ar.cause())
                }
            }
        }
        this

    }

    /*
     * pub-sub: - publish to some address
     */
    Actor publish (Actor actor, args, DeliveryOptions options=null) {
        publish (actor.address, args, options)
    }

    Actor publish (Address postTo, args, DeliveryOptions options=null) {
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)

        //use the circuit breaker setup in the AbstractActor
        this.breaker.execute { Promise promise  ->
            Future result = getVertx().eventBus().send(sendTo.address, argsMessage, options ?: new DeliveryOptions())
            result.onComplete { AsyncResult ar ->
                if (ar.succeeded())  {
                    promise.complete(ar.result())
                } else {
                    promise.fail(ar.cause())
                }
            }
        }
        this
    }

    //alternate pub-sub method names for publish
    Actor post (Actor actor, args, DeliveryOptions options=null) {
        publish (actor.address, args, options)
    }

    Actor post (Address postTo, args, DeliveryOptions options=null) {
        publish (postTo, args, options)
    }

    /*
     * request with reply  response : - publish to some address
     * using blocking queue to simulate synchronous RPC call
     */
    def requestAndReply(Actor actor,  args, DeliveryOptions options = null) {
        long waitFor = this.appConfig.actor.framework.circuitBreaker.timeout * this.appConfig.actor.framework.circuitBreaker.retries
        requestAndReply(actor.address, args, 3, TimeUnit.MILLISECONDS, options)
    }

    def requestAndReply(Actor actor,  args, long waitFor, TimeUnit tu,  DeliveryOptions options = null) {
        requestAndReply(actor.address, args, waitFor, tu, options)
    }


    def requestAndReply(Address requestAddress,  args, long waitFor, TimeUnit tu, DeliveryOptions options = null) {
        log.debug ("request&reply: [$args] sent to [${address.address}]")

        BlockingQueue results = new LinkedBlockingQueue()

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        // see also https://github.com/vert-x3/wiki/wiki/RFC:-Future-API

        //use new promise/future model - resuest is expecting a message.reply()
        //use the circuit breaker setup in the AbstractActor
        Future<Message> response

        this.breaker.execute { Promise promise  ->
            response = getVertx().eventBus().request(requestAddress.address, argsMessage, options ?: new DeliveryOptions())

            //get response and add to blocking Queue
            response.onComplete(ar -> {
                if (response.succeeded()) {
                    log.debug "in requestsAndReply, future handler with [${ar.result().body()}]"
                    results.put(ar.result().body())
                    promise.complete(ar.result().body())
                } else {
                    log.error "in requestsAndReply, future handler with error [${ar.cause()}]"
                    String errMessage = "with error: ${ar.cause().message()} "
                    results.put (new JsonObject ('reply', errMessage ) )
                    promise.fail(ar.cause())
                }
            })


            response
        }

        //blocking wait for result to become available then return it
        JsonObject jsonObjectReturn = results.poll(waitFor, tu)

        jsonObjectReturn?.getValue('reply')
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

    Future requestAndAsyncReply (Address requestAddress, args, DeliveryOptions options = null) {
        //requestAndAsyncReply (address.address, args, options)
        log.debug ("request&asyncReply: [$args] sent to [${requestAddress.address}]")

        //wrap args in jsonObject
        JsonObject argsMessage = new JsonObject()
        argsMessage.put("args", args)
        // see also https://github.com/vert-x3/wiki/wiki/RFC:-Future-API

        //use new promise/future model - request is expecting a message.reply()
        Future response
        this.breaker.execute { Promise promise ->
            response = getVertx().eventBus().request(requestAddress.address, argsMessage, options ?: new DeliveryOptions())

            //when complete update the circuitBreaker promise
            response.onComplete {ar ->
                if (ar.succeeded()) {
                    promise.complete(ar.result().body())
                } else {
                    promise.fail (ar.cause())
                }
            }
        }
        response

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

        //dont use blocking code for schWork
        Closure schWorkWithPromise = { promise.complete(schWork ()) }

        Timer timer = new Timer (tid: getVertx().setTimer(delay, schWorkWithPromise ))
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
        Timer timer = new Timer (tid:getVertx().setPeriodic (delay, schWork))
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

        Closure action = getAction()

        //closure that executes the action closure and stores the result in the Promise
        //using a closure as need to reference the args in context, as executeBlocking only passes a Promise as arg
        Closure doBlockingAction = {Promise<Object> promise ->
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

        vertx.executeBlocking(doBlockingAction)
                .onComplete(ar -> {
                    JsonObject json = new JsonObject()
                    if (ar.succeeded()) {
                        if (message.isSend() && message.replyAddress()  ) {
                            def result = ar.result()

                            if (message.replyAddress()) {
                                json.put("reply", result.toString())

                                message.reply(json)
                                log.debug("executeAction(): replying with  [$json] to reply@: ${message.replyAddress()}, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")
                            } else {
                                log.debug("executeAction(): no reply address provided, orig message sent to ${message.address()}, isSend() : ${message.isSend()}")
                            }

                        } else  {
                            def result = ar.result()
                            json.put("pub-sub message output", result.toString())

                            log.debug("executeAction(): received async publish message, got  [$json] from action(), orig message sent to ${message.address()}")

                        }
                    } else {
                        json.put("Exception", ar.cause().message)
                        message.reply(json)
                    }
                })
    }

    String toString() {
        "actor ${this.getClass().name} (${getName()}:[${getDeploymentId()}])"
    }
}