package scripts

import io.vertx.core.AsyncResult
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Future

Vertx vertx = Vertx.vertx()

Promise scrPromise = Promise.promise()
assert scrPromise.future().isComplete() == false

Timer timer = new Timer() // Instantiate Timer Object

// Start running the task on Monday at 15:40:00, period is set to 8 hours
// if you want to run the task immediately, set the 2nd parameter to 0
Closure task = {
    println "\ttask: in closure task on "
    assert scrPromise.future().isComplete() == false
    if (scrPromise.future().isComplete() == false) {
        println "\ttask: set the scrPromise to OK"
        scrPromise.complete("OK from task ")
    }
}
timer.schedule(task , 20, 10_000)

sleep (50)
println "script: slept 50ms "
assert scrPromise.future().isComplete() == true
scrPromise.future().onComplete(ar -> println "scrFuture is complete with :  ${ar.result()}")

void handler (Promise p) {
    def work = {
        return "delayed result, OK"
    }

    assert p.future().isComplete() == false

    println "\thandler is setting the promise with result"
    p.complete(work())
}

Future wrapper (vertx) {

    Promise promise = Promise.promise()
    assert promise.future().isComplete() == false

    println "set timer for 50ms second "
    long tid = vertx.setTimer(50, {

        assert promise.future().isComplete() == false

        println "timer calling handler with promise "
        handler(promise)
    })

    promise.future()
}

Future future  = wrapper(vertx)
assert future.isComplete() == false


future.onSuccess({AsyncResult ar ->
    println "got result : ${ar.result()}"
})

sleep (1000)
vertx.close()