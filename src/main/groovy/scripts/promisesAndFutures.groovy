package scripts

import io.vertx.core.AsyncResult
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Future

Vertx vertx = Vertx.vertx()

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