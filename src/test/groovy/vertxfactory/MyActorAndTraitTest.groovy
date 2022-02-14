package vertxfactory

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
        }

    }

    def "get MyActor from Actors " () {
        setup:
        def conditions = new PollingConditions(timeout: 10)
        MyActor actor
        def actors = Actors::new()  //forces constructor injection

        when:
        conditions.within (2) {
            assert Actors.vertx
            actor = Actors.myActor("will")

        }

        then:
        conditions.within(4) {

            actor.name == "will"
            actor.deploymentId != ""
            //actor.vertx == Actors.vertx
            //Actors.vertx.isClustered() == false
        }


    }

}

