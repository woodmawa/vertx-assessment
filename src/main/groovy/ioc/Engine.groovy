package ioc

import jakarta.inject.Inject
import jakarta.inject.Named

import javax.inject.*

interface Engine {
    int getCylinders()
    String start()
}

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

    @Inject Vehicle (@Named('v8') Engine engine) {
        this.engine = engine
    }

    String start () {
        engine.start()
    }
}