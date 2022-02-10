package actor

import io.vertx.core.Future
import io.vertx.core.eventbus.DeliveryOptions

interface  Actor {
    void setName (String name)
    String getName ()
    String getAddress ()
    Actor send (Address postTo, args)
    Actor send (Actor anotherActor,  args)
    Actor send (Address postTo,  args, DeliveryOptions options)
    Actor send (Actor anotherActor,  args, DeliveryOptions options)

    def requestAndReply( Actor actor, args)
    def requestAndReply( Actor actor, args, DeliveryOptions options)
    Future requestAndAsyncReply( Actor actor, args)
    Future requestAndAsyncReply(Actor actor, args, DeliveryOptions options)

    void start()
    void stop()

}