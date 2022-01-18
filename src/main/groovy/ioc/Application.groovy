package ioc

import io.micronaut.context.ApplicationContext

class Application {

    static main (args) {
        println "app starting"

        ApplicationContext context = ApplicationContext.run()


        Vehicle v = context.run().getBean(Vehicle)

        println v.start()
        //println v.v4Engine.start()


        println "app stopping"

    }
}
