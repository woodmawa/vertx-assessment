package actor

import groovy.transform.InheritConstructors
import io.micronaut.context.annotation.Prototype

@InheritConstructors
class MyActor extends StandardActor {
    def onMessage (String mess) {
        println "overidden onMessage got $mess"
        mess
    }
}
