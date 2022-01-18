package ioc

import jakarta.inject.Inject
import jakarta.inject.Named

import javax.inject.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

interface Engine {
    int getCylinders()
    String start()
}

@Qualifier
@Retention (RetentionPolicy.RUNTIME)
@interface V6 {}

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

class Vehicle {
    final Engine engine
    final Engine smallerEngine

    @Inject Vehicle (@Named('v8') Engine engine) {
        this.engine = engine
    }

    /*@Inject Vehicle (@V6  Engine smallerEngine) {
        this.engine = smallerEngine
    }*/

    String start () {
        engine.start()
    }
}