package ioc

import actor.Actor
import io.micronaut.context.ApplicationContext

import javax.inject.Inject

class TestActorDI {
    @Inject Actor actor //should inject a prototype unnamed actor - fails with NPE when used 

    ApplicationContext context = ApplicationContext.run()

    //doing direct lookup generates the bean !
    def tryBeanLookup(){
        def bean = context.run().getBean(Actor)
        bean
    }
}
