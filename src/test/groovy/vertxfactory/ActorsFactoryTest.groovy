package vertxfactory

import com.softwood.actor.Actor
import com.softwood.actor.Actors
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import spock.lang.Specification

//need this to get the DI container started before the tests are run
@MicronautTest
class ActorsFactoryTest extends Specification {

    //injected actor should have been deployed during bean creation in factory
    @Inject def Actor actor1
    @Inject def Actor actor2


    def "test injection " () {
        setup:
        actor1.setName("will")
        actor2.setName("maz")

        JsonObject retVal = actor1.requestAndReply(actor2, "hello world")
        sleep (1000)

        expect:
        actor1 != null
        actor1.name == "will"
        actor1.address == "actor.will"
        Actors.vertx
        retVal.getString("reply") == "hello world"
    }
}
