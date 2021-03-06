package ioc

import com.softwood.actor.Actor
import io.micronaut.context.ApplicationContext

import jakarta.inject.Inject

/*not a bean it its own right - but see if you can ask the bean context for one
* it has a bean dependency however
*/

class TestActorDI {
    @Inject Actor actor //should inject a prototype unnamed actor - fails with NPE when used

    ApplicationContext context = ApplicationContext.run()

    //doing direct lookup generates the bean !
    def tryBeanLookup(){
        def bean = context.getBean(Actor)
        bean
    }
}
