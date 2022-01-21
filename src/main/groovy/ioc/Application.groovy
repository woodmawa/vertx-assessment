package ioc

import actor.Actors
import actor.StartupCondition
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import io.vertx.core.Future
import jdk.internal.jimage.ImageReader
import org.apache.tools.ant.types.selectors.TypeSelector
import groovy.yaml.YamlSlurper

import javax.inject.Inject
import javax.inject.Qualifier
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Qualifier
@Retention (RetentionPolicy.RUNTIME)
@interface Server {}

class Application {

    static main (args) {


        ApplicationContext context = ApplicationContext.run()

        Environment env

        env = context.getEnvironment()

            ConfigObject appConfig = context.run().getBean(ConfigObject)
        Future server  = context.run().getBean(Future, Qualifiers.byName('server'))

        Vehicle v = context.run().getBean(Vehicle)

        println v.start()

        server.onComplete{ar ->
            if (ar.succeeded()) {
                def vertx = ar.result()
                println "future, clustered vertx started, with $vertx"

                println "app stopping"
                Actors.shutdown()

            } else {
                println "clustered vertx failed ${ar.cause()}"
            }
        }

        v
    }
}
