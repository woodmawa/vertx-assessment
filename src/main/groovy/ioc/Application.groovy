package ioc

import io.micronaut.context.ApplicationContext
import jdk.internal.jimage.ImageReader
import org.apache.tools.ant.types.selectors.TypeSelector
import groovy.yaml.YamlSlurper

import javax.inject.Inject

class Application {

    static main (args) {


        ApplicationContext context = ApplicationContext.run()

        ConfigObject appConfig = context.run().getBean(ConfigObject)
        Vehicle v = context.run().getBean(Vehicle)

        println v.start()
        //println v.v4Engine.start()


        println "app stopping"

    }
}
