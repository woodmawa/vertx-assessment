package vertxfactory

import com.softwood.actor.Actor
import com.softwood.actor.Actors
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import jakarta.inject.Named
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

//need this to get the DI container started before the tests are run
@MicronautTest
class ActorsFactoryTest extends Specification {

    //injected actor should have been deployed during bean creation in factory
    @Inject @Named ("DefaultActor") Actor actor1
    @Inject @Named ("DefaultActor") Actor actor2


    def "test injection " () {
        setup:
        actor1.setName("will")
        actor2.setName("maz")

        def retVal = actor1.requestAndReply(actor2, "hello world")
        sleep (1000)

        expect:
        actor1 != null
        actor1.name == "will"
        actor1.address.address == "actor.will"
        Actors.vertx
        retVal == "hello world"
    }

    def "defaultDispatchAction test " () {
        setup:
        def ddActor = Actors.dynamicDispatchActor('will', {println "\t>> $it"; it})
        def conditions = new PollingConditions(timeout: 10)
        def result

        when:
        result = ddActor.requestAndReply(ddActor, "hello world")

        then  :
        conditions.within (3) {
            result == "hello world"
        }

    }

    def "test send with circuit breaker  test " () {
        setup:
        def ddActor = Actors.dynamicDispatchActor('will', {println "\t>> $it"; it})
        def conditions = new PollingConditions(timeout: 10)
        def result

        when:
        result = ddActor.send(ddActor, "hello world")

        then  :
        conditions.within (3) {
            true
        }

    }
}
