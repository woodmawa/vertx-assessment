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

        //println "\t>>then: current (${current.hashCode()}), next(${next.hashCode()})"
        next
    }

    rightShift << {closure ->
        //println "rightShift has closure [${closure.hashCode()}], call then "
        then (closure)
    }

    getValue <<  {
        Reference ref = new Reference ()
        BlockingQueue result = new ArrayBlockingQueue<>(5)
        delegate.onComplete {ar ->
            if (ar.succeeded()) {
                //println "\t>getValue() completed future with " + ar.result()
                result.put (ar.result()) //blocking put
            } else {
                throw ar.cause()
            }
        }
        //blocking get
        def value = result.take ()
        println "\t>getValue() blocking get(),  read $value"
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

        //println "polling get on queue $timeout, $units,  size ${result.size()}"
        def value  = result.poll (timeout, units)
        if (value)
            result.remove(value)
        value
    }

    /*
     * transform this future using the supplied closure
     */
    trans << {Closure clos ->
        assert clos, "trans: closure cannot be null"
        Closure clone = clos.clone()
        Future future = delegate
        Promise promise = Promise.promise()

        assert future, "trans: delegate shouldn be null"
        def inputValue

        if (future.succeeded()) {
            inputValue = future.result()
            try {
                def result = clone(inputValue)
                promise.complete(result)
            } catch (Throwable exc) {
                promise.fail(exc)
            }
        } else {
            promise.fail(future.cause())
        }

        promise.future()
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

Promise promise = Promise.promise()
Future sf = promise.future()
//sf.onSuccess{Object res -> println "-> script: got object $res" }
//sf.onFailure(Throwable::printStackTrace)
sf.onComplete{ar ->
    println "sf completed with ${ar.result()}"
}
sleep (300)
promise.complete("object completed with OK")


Future tf = vertx.executeBlocking{it.complete("\t> hello william") }
tf.onSuccess{it-> println "tf onSuccess: ${it.call()}" }
println "script: read tf value as " + tf.getValue()

Future tf2 = tf.map{it -> println "map called with $it"; it.toUpperCase()}
tf2.onSuccess{Closure it ->
    println "future map then onSuccess: " + it.call()
}
tf2.onFailure{it.printStackTrace()}



Promise p3 = Promise.promise()
Future tf3 = p3.future()

p3.complete ("lower")
/*Future mappedTf3 = tf3.map { String val ->
    Future f = Future.future()
    f.map "lower -> UPPER"
}*/
Future mappedTf3 = tf3.compose{String str -> def mapped = str.toUpperCase()
def p = Promise.promise()
p.complete (mapped)
p.future()
}

Future mappedTf5 = mappedTf3.trans {String val ->
    val.reverse()

}

Future mappedTf4 = mappedTf3.flatMap {String str ->
    def mapped = str.reverse()
    def p = Promise.promise()
    p.complete (mapped)
    p.future()
}

mappedTf4.onComplete {ar ->
    //def resClos = ar.result()
    def res = ar.result()
    println "\t>tf4: final mapped result was $res "
}


mappedTf5.onComplete {ar ->
    //def resClos = ar.result()
    def res = ar.result()
    println "\t>tf5: trans() final mapped result was $res "
}

//sleep (300)
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



/*then.flatMap {ar ->
    println "then flatmap result " + ar
}*/


def value = then.getValue () //or timedout version (300, TimeUnit.MILLISECONDS)
println "script: then result " + then.result() +  "  blocking get : ${value}"  // with blocking poll got getResult(300, TimeUnit.MILLISECONDS)

vertx.close()