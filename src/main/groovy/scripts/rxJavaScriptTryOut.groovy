package scripts

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

import java.util.concurrent.atomic.AtomicInteger

println ">> observe flowable with map and filter"
Flowable<Integer> flow = Flowable.range(1, 5)
        .map(v -> v * v)
        .filter(v -> v % 3 == 0)

flow.subscribe (System.out::println, Throwable::printStackTrace)

println ">> observe concurrently on schedulers"
Flowable.range(1, 10)
        .observeOn(Schedulers.computation())
        .map(v -> v * v)
        .blockingSubscribe(System.out::println)

println ">> observe parallel stream  "
Flowable.range(1, 10)
        .parallel()
        .runOn(Schedulers.computation())
        .map(v -> v * v)
        .sequential()
        .blockingSubscribe(System.out::println)

println ">> try flow dependency - defer eval till runtime  "
AtomicInteger count = new AtomicInteger()

def res = Flowable.range(1, 10)
        .doOnNext(ignored -> count.incrementAndGet())
        .ignoreElements()
        .andThen(Single.defer(() -> Single.just(count.get())))

res.subscribe(System.out::println, Throwable::printStackTrace)