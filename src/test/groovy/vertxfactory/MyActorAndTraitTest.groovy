package vertxfactory

import com.softwood.actor.ActorState
import com.softwood.actor.Actors
import com.softwood.actor.MyActor
import com.softwood.vertxfactory.VertxFactory
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import jakarta.inject.Inject
import jakarta.inject.Named
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@MicronautTest
class MyActorAndTraitTest extends Specification {

    @Inject @Named ("Vertx") Future<Vertx> futureServer

    def "manually deploy MyActor verticle" () {
        setup:
        def conditions = new PollingConditions(timeout: 10)
        Vertx vertx

        MyActor actor = new MyActor()

        Optional<Vertx> optv = VertxFactory.vertx()
        vertx = optv.get()

        when :

        //do a manual deployment
        assert vertx.isClustered() == false
        Future future = vertx.deployVerticle(actor)


        then:
        //true
        conditions.within(2) {
            assert future.succeeded()
            actor.deploymentId = future.result()
            assert future.result() == actor.deploymentId
            assert actor.vertx == vertx
            actor.close()
        }

    }

    def "get MyActor from Actors " () {
        setup:
        def conditions = new PollingConditions(timeout: 10)
        MyActor actor

        when:
        actor = Actors.myActor("will")


        then:
        conditions.within(2) {

            actor.name == "will"
            actor.deploymentId != ""
            actor.vertx == Actors.vertx
            Actors.vertx.isClustered() == false
            actor.status == ActorState.Running
            actor.close()
        }

    }

    def "check self actor " () {
        setup:
        def conditions = new PollingConditions(timeout: 10)
        MyActor actor

        when:
        actor = Actors.myActor("will")

        then:
        actor.self === actor
        actor.close()

    }

    def "process message sent to self " () {
        setup:
        def conditions = new PollingConditions(timeout: 10)
        MyActor actor
        def res = ""

        when:
        actor = Actors.myActor("will")
        actor.action = {res = "$it"; println "processed $it";  it}

        actor.send (actor.self, "hello william")

        then:
        conditions.within(4) {
            res == "hello william"
            actor.close()
        }
    }

    def "pub sub message from one actor to another " () {

        setup:
        def conditions = new PollingConditions(timeout: 10)
        MyActor actorWill, actorMaz
        def res = ""
        def depActors

        when:
        actorWill  = Actors.myActor("will")

        actorMaz  = Actors.myActor("maz")
        actorMaz.action = {res = "$it";  it}


        conditions.within (1) {
            actorWill.publish (actorMaz, "hello william")
        }

        then:
        conditions.within(4) {
            res == "hello william"
            depActors = Actors.findDeployedActorsByName("will")
            assert depActors.contains(actorWill)

        }
        println "actors named 'will' " +depActors
    }
}

