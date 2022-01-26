package ioc

import actor.Actor
import io.micronaut.context.ApplicationContext

import javax.inject.Inject

class TestActorDI {
    @Inject Actor actor //should inject a prototype unnamed actor

    ApplicationContext context = ApplicationContext.run()

    def tryBeanLookup(){
        def bean = context.run().getBean(Actor)
        bean
    }
}
