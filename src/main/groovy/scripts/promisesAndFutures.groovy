package scripts

import io.vertx.core.AsyncResult
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Future

Vertx vertx = Vertx.vertx()

Future handler (Promise p) {
    def work = {
        return "delayed result, OK"
    }

    def result = work()

    println "\thandler completes promise with the result of work() "
    p.complete(result)
}

Future wrapper (vertx) {

    Promise promise = Promise.promise()

     long tid = vertx.setTimer(20, {

        println "vertx timer calling handler with promise "
        handler(promise)
    })

    promise.future()
}

Future future  = wrapper(vertx)

future.onComplete({AsyncResult ar ->
    println "future.onComplete(): got result : ${ar.result()}"
})

sleep (300)
vertx.close()