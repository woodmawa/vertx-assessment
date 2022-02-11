package vertxfactory

import actor.Actor
import actor.Actors
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.junit5.VertxTestContext
import jakarta.inject.Inject
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions
import spock.util.concurrent.PollingConditions

@MicronautTest
class StandardActorTest extends Specification {

    @Inject Actor initiator  //start already been called for inject targets
    @Inject Actor responder
    @Inject ApplicationContext context

    def "send and receive reply " () {
        setup:
        initiator.name = "will"
        responder.name = "maz"

        when:
        initiator.send(responder, "ping")
        //responder.reply (initiator, "pong")

        then :
        responder
    }

    def "basic vertx event bus send and respond " () {
        setup :
        Vertx vertx = Actors.vertx
        VertxTestContext vertxTestContext = new VertxTestContext()
        Context vctx = vertx.getOrCreateContext()
        String salutation = "hello will"
        def async = new AsyncConditions()
        def response
        def conditions = new PollingConditions(timeout: 3)


        when:
        //set listener on 'address'
        vertx.eventBus().consumer("address") { message ->
            vertxTestContext.verify {
                assert salutation == message.body()
            }
            vertxTestContext.completeNow()

            def body = message.body()
            println "got message body : " + body

            println "return address is : " + message.replyAddress()
            message.reply("..and good day to you to")


        }

        //then send message to that address
        //EventBus eventBus

        Future result = vertx.eventBus().request("address", salutation)

        result.onComplete { ar ->
            if (ar.succeeded()) {
                println "..received a response  : " + ar.result().body()
                response = ar.result().body()
            } else  {
                response = ar.cause().message
                println ar.cause()
            }
        }


        then:
        conditions.within(2) {
            assert response == "..and good day to you to"
        }
    }
}
