package ioc

import actor.Actor
import actor.Actors
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import io.vertx.core.Future

import jakarta.inject.Qualifier
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


class Application {

    static main (args) {


        ApplicationContext context = ApplicationContext.run()

        Environment env = context.getEnvironment()

        ConfigObject appConfig = context.run().getBean(ConfigObject)
        Future actorVertx  = context.run().getBean(Future, Qualifiers.byName('Actors-Vertx'))

        Vehicle v = context.run().getBean(Vehicle)

        println v.start()

        actorVertx.onComplete{ar ->
            if (ar.succeeded()) {
                def vertx = ar.result()
                println "future, ${appConfig.framework.serverMode} vertx started, with $vertx"

                Actor actorByLookup = context.run().getBean(Actor)
                assert actorByLookup

                TestActorDI testActor = new TestActorDI()       //create class that has @inject in it
                def actorBean = testActor.tryBeanLookup()  //get bean by direct lookup - works
                assert actorBean

                //although TestActorDI is not a bean itself, it has a bean dependency - try and request getting a  TestActorDI
                //from context and see what happens
                TestActorDI testActorByLookup = context.run().getBean(TestActorDI)
                assert testActorByLookup.actor  // injection now  works!

                println "app stopping"
                Actors.shutdown()

            } else {
                println "clustered vertx failed ${ar.cause()}"
            }
        }

        v
    }
}
