package ioc

import io.micronaut.context.annotation.Prototype
import jakarta.inject.Inject
import jakarta.inject.Named

import javax.annotation.PostConstruct
import jakarta.inject.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

interface Engine {
    int getCylinders()
    String start()
}

@Qualifier
@Retention (RetentionPolicy.RUNTIME)
@interface V6 {}

@Qualifier
@Retention (RetentionPolicy.RUNTIME)
@interface V4 {}

@Qualifier
@Retention (RetentionPolicy.RUNTIME)
@interface V2 {}

@Singleton
class V8Engine implements Engine {
    int cylinders = 8

    String start() {
        "Starting V8 Engine"
    }
}

@Singleton
class V6Engine implements Engine {
    int cylinders = 6

    String start() {
        "Starting V6 Engine"
    }
}

@Singleton
class V4Engine implements Engine {
    int cylinders = 4

    String start() {
        "Starting V4 Engine"
    }
}

@Prototype
class CrankShaft {

    String toString() {
        "metal crankshaft"
    }
}

//see engineFactory where this is created as a Bean
//using Constructor injection for crankShaft bean
class V2Engine implements Engine {
    int cylinders = 4
    CrankShaft shaft

    @Inject V2Engine (CrankShaft crankShaft) {
        println "V2Engine gets an injected crankshft $crankShaft"
        shaft = crankShaft
    }

    String start() {
        "Starting V2 Engine, with crankShaft = $shaft"
    }

    @PostConstruct
    void initialise() {}

}

class Vehicle {
    final Engine engine
    Engine smallerEngine
    @Inject @V4  Engine v4Engine
    @Inject @V2  Engine v2Engine

    //constructor injection - singl public constructor or
    //single constructor annoted with @inject
    @Inject Vehicle (@Named('v8') Engine engine) {
        this.engine = engine
    }

    //method bean property injection
    @Inject
    void setSmallerEngine (@V6  Engine smallerEngine) {
        this.smallerEngine = smallerEngine
    }

    String start () {
        println "start: " + engine.start()
        println "start: " + smallerEngine.start()
        println "start: " + v4Engine.start()
        println "start: " + v2Engine.start()
        "Vehicle all OK"
    }
}