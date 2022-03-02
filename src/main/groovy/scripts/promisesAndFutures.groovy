package scripts

import io.micronaut.core.async.publisher.AsyncSingleResultPublisher
import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Future

import java.util.concurrent.Flow

Vertx vertx = Vertx.vertx()

Future.metaClass.then = {closure ->
    Future future =  delegate

    future.onComplete {ar ->
        Context ctx = Vertx.vertx().getOrCreateContext()
        def ordered = true
        println "\treceived result [${ar.result()}]"
        composeResult = ctx.executeBlocking({closure(ar.result()) }, ordered)  //returns new future
    }

   // Promise promise = Promise.promise()

    //run the closure on another thread
    //AsyncSingleResultPublisher arp = new AsyncSingleResultPublisher<>(closure)
    //Flow.Subscriber sub = Promise::handle
    //arp.subscribe(sub)


}

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
/*
Future then = future.compose {String res ->
    //compose must return a new future, with result of compose action
    println "\tres : $res"

    Promise p = Promise.promise()
    p.complete(res.toUpperCase() )
    p.future()
}*/

Future then = future.then {it.toUpperCase()}


sleep (300 )
println "then result " + then.result()
vertx.close()