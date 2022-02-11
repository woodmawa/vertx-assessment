package actor

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import org.codehaus.groovy.runtime.MethodClosure

import java.time.Duration

interface  Actor {
    void setName(String name)

    String getName()
    String getAddress()

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
    Actor send(String address, args)
    Actor send(String address, args, DeliveryOptions options)

    //pseudo sync, blocking,  call
    def requestAndReply(Actor actor, args)
    def requestAndReply(Actor actor, args, DeliveryOptions options)
    def requestAndReply(Address sendTo, args)
    def requestAndReply(Address sendTo, args, DeliveryOptions options)
    def requestAndReply(String address, args)
    def requestAndReply(String address, args, DeliveryOptions options)

    //async request, get a future as response
    Future requestAndAsyncReply(Address address, args)
    Future requestAndAsyncReply(Address address, args, DeliveryOptions options)
    Future requestAndAsyncReply(Actor actor, args)
    Future requestAndAsyncReply(Actor actor, args, DeliveryOptions options)
    Future requestAndAsyncReply(String address, args)
    Future requestAndAsyncReply(String address, args, DeliveryOptions options)

    //pub-sub
    Actor post (Actor actor, def args)
    Actor post (Actor actor, def args, DeliveryOptions options)
    Actor post (Address postTo, def args)
    Actor post (Address postTo, def args, DeliveryOptions options)
    Actor publish (Actor actor, def args)
    Actor publish (Actor actor, def args, DeliveryOptions options)
    Actor publish (Address postTo, def args)
    Actor publish (Address postTo, def args, DeliveryOptions options)

    //manual start and stop options for verticles
    void start()
    void stop()

    //timers
    Timer periodicTimer(long delay, Closure scheduledWork)
    Timer periodicTimer (Duration delay, Closure scheduledWork)
    Timer timer (long delay, Closure scheduledWork )
    Timer timer (Duration delay, Closure scheduledWork )

    boolean cancelTimer (long id)
    boolean cancelTimer (Timer tid)


    //run code block on vertx thread and return result as future
    Future run (code)
}