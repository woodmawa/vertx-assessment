package scripts

import io.micronaut.core.async.publisher.AsyncSingleResultPublisher
import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.Future

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit

Vertx vertx = Vertx.vertx()

//use metaclass DSL
Future.metaClass{
    then = {closure ->
        Future current =  delegate
        Future next

        /*
         * used by executeBlocking which will call this with a promise
         */
        Closure blockingWrapper = {Promise promise ->
            current.onComplete {ar ->
                def currentResult
                if (ar.succeeded()) {
                    try {
                        Context ctx = Vertx.vertx().getOrCreateContext()
                        currentResult = ar.result()
                        promise.complete(closure.call(currentResult))
                    } catch (InterruptedException ex) {
                        promise.fail (ex)
                    }
                } else {
                    promise.fail(ar.cause())
                }
             }
        }

        //execute blocking wrapper on worker thread and  return new future
        next = vertx.executeBlocking(blockingWrapper)  //runs new closure on worker thread, returns new future

        println "\t>>then: current (${current.hashCode()}), next(${next.hashCode()})"
        next
    }

    rightShift << {closure ->
        //println "rightShift has closure [${closure.hashCode()}], call then "
        then (closure)
    }

    getValue <<  {
        Reference ref = new Reference ()
        BlockingQueue result = new ArrayBlockingQueue<>(10)
        delegate.onComplete {ar ->
            if (ar.succeeded()) {
                result.put (ar.result()) //blocking put
            } else {
                throw ar.cause()
            }
        }
        //blocking get
        println "blocking get on queue size ${result.size()}"
        def value = result.take ()
        result.remove(value)
        value
    }

    getValue <<  {timeout, units ->
        Reference ref = new Reference ()
        BlockingQueue result = new ArrayBlockingQueue<>(5)
        delegate.onComplete {ar ->
            if (ar.succeeded()) {
                result.put (ar.result())
            } else {
                throw ar.cause()
            }
        }

        println "polling get on queue $timeout, $units,  size ${result.size()}"
        def value  = result.poll (timeout, units)
        if (value)
            result.remove(value)
        value
    }
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
    println "script: original future.onComplete(): got result : ${ar.result()}"
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

//Future then = future.then {it.toUpperCase()}.then {it.toLowerCase()}
Future then = future >> {it.toUpperCase()} >> {it.toLowerCase()}


//sleep (300 )

/*then.flatMap {ar ->
    println "then flatmap result " + ar
}*/
then.onSuccess {
    //cant see this get run
    println ">then onSuccess result: " + then.result()
}
then.onFailure{
    //cant see this get run
    println "got exception $it"
}

/*then.onComplete {ar ->
    println "script: next future.onComplete(): got result : ${ar.result()}"
}*/

def value = then.getValue(300, TimeUnit.MILLISECONDS)
println "script: then result " + then.result() +  "  blocking get : ${value}"  // with blocking poll got getResult(300, TimeUnit.MILLISECONDS)

vertx.close()