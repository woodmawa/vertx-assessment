package ioc

import actor.Actor
import actor.Actors
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import io.vertx.core.Future

import javax.inject.Qualifier
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Qualifier
@Retention (RetentionPolicy.RUNTIME)
@interface vertx {}

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
                TestActorDI testActor = new TestActorDI()
                assert testActor.actor  // injection should work

                println "app stopping"
                Actors.shutdown()

            } else {
                println "clustered vertx failed ${ar.cause()}"
            }
        }

        v
    }
}
