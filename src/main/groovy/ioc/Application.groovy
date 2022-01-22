package ioc

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

        Environment env

        env = context.getEnvironment()

        ConfigObject appConfig = context.run().getBean(ConfigObject)
        Future server  = context.run().getBean(Future, Qualifiers.byName('vertx'))

        Vehicle v = context.run().getBean(Vehicle)

        println v.start()

        server.onComplete{ar ->
            if (ar.succeeded()) {
                def vertx = ar.result()
                println "future, ${appConfig.framework.serverMode} vertx started, with $vertx"

                println "app stopping"
                Actors.shutdown()

            } else {
                println "clustered vertx failed ${ar.cause()}"
            }
        }

        v
    }
}
