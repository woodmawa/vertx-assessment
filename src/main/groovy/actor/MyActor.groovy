package actor

import groovy.transform.InheritConstructors

@InheritConstructors
class MyActor extends StandardActor {
    def onMessage (String mess) {
        println "overidden onMessage got $mess"
        mess
    }
}
