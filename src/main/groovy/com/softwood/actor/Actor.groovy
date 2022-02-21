package com.softwood.actor

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import org.codehaus.groovy.runtime.MethodClosure

import java.time.Duration
import java.util.concurrent.TimeUnit

interface  Actor {

    void setName(String name)
    String getName()
    Address getAddress()
    String getDeploymentId()
    ActorState getStatus()

    //manage subscriptions
    MessageConsumer addConsumer (Address from, Handler<Message<Object>> consumer)
    MessageConsumer addConsumer ( Closure<Message<Object>> consumer)
    MessageConsumer addConsumer (MethodClosure consumer)
    MessageConsumer addConsumer ( Handler<Message<Object>> consumer)

    boolean removeConsumer (MessageConsumer consumer)
    boolean removeAllConsumersFromAddress (Address postTo)
    boolean removeAllConsumersFromAddress (String address)

    //no response expected
    Actor send(Address postTo, args)
    Actor send(Actor anotherActor, args)
    Actor send(Address postTo, args, DeliveryOptions options)
    Actor send(Actor anotherActor, args, DeliveryOptions options)

    //pseudo sync, blocking,  call
    def requestAndReply(Actor actor, args)
    def requestAndReply(Actor actor, args, DeliveryOptions options)
    def requestAndReply(Actor actor, args, long waitFor, TimeUnit tu)
    def requestAndReply(Actor actor, args, long waitFor, TimeUnit tu, DeliveryOptions options)

    def requestAndReply(Address sendTo, args, long waitFor, TimeUnit tu, DeliveryOptions options)

    //async request, get a future as response
    Future requestAndAsyncReply(Address address, args)
    Future requestAndAsyncReply(Address address, args, DeliveryOptions options)
    Future requestAndAsyncReply(Actor actor, args)
    Future requestAndAsyncReply(Actor actor, args, DeliveryOptions options)

    //pub-sub
    Actor post (Actor actor, args)
    Actor post (Actor actor, args, DeliveryOptions options)
    Actor post (Address postTo, args)
    Actor post (Address postTo, args, DeliveryOptions options)
    Actor publish (Actor actor, args)
    Actor publish (Actor actor, args, DeliveryOptions options)
    Actor publish (Address postTo, args)
    Actor publish (Address postTo, args, DeliveryOptions options)

    //timers
    Timer periodicTimer(long delay, Closure scheduledWork)
    Timer periodicTimer (Duration delay, Closure scheduledWork)
    Timer timer (long delay, Closure scheduledWork )
    Timer timer (Duration delay, Closure scheduledWork )

    boolean cancelTimer (long id)
    boolean cancelTimer (Timer tid)

    //run code block on vertx thread and return result as future
    Future run (code)

    //stop and remove actor verticle
    void close()
}