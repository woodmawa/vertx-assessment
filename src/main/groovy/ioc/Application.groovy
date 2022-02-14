package ioc

import com.softwood.actor.Actor
import com.softwood.actor.Actors
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx

class Application {

    static main (args) {

        println "running main app "
        ApplicationContext context = ApplicationContext.run()
        //ApplicationContext context =  Micronaut.run (Application)  //starts local web server

        assert context
        Environment env = context.getEnvironment()

        ConfigObject appConfig = context.getBean(ConfigObject)
        assert appConfig

        Future<Vertx> futVertx = context.getBean(Future<Vertx>, Qualifiers.byName("Vertx"))

        Vertx vertx
        futVertx.onComplete { AsyncResult ar ->
            if (ar.succeeded()) {
                vertx = ar.result()

                println "[${appConfig.framework.serverMode}] vertx started, with $vertx"

                Actor actorByLookup = context.getBean(Actor)
                actorByLookup.setName("william")
                assert actorByLookup

                TestActorDI testActor = new TestActorDI()       //create class that has @inject in it
                def actorBean = testActor.tryBeanLookup()  //get bean by direct lookup - works
                assert actorBean

                //although TestActorDI is not a bean itself, it has a bean dependency - try and request getting a  TestActorDI
                //from context and see what happens
                TestActorDI testActorByLookup = context.getBean(TestActorDI)
                assert testActorByLookup.actor  // injection now  works!

                println "app stopping"
                Actors.shutdown()

            } else {
                ar.cause().printStackTrace()
            }
        }
    }

}
