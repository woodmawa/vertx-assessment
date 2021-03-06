package scripts

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.reactivestreams.Subscription

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import io.reactivex.rxjava3.functions.Predicate
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.IntStream

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

/*  this works ok
Flowable<Long> timedFlow = Flowable.timer(1, TimeUnit.SECONDS)
timedFlow.blockingSubscribe({println it}, Throwable::printStackTrace)
Iterable iter = timedFlow.blockingIterable(5)
println "${iter.toList()}"
*/


Flowable.just(*['a','b','c']).subscribe{println "from spread list, char $it"}

Flowable.fromStream (["a", "b", "c"].stream()).subscribe {println "from steam $it"}
Flowable.fromStream (["a", "b", "c"].stream())
        .doOnSubscribe { Subscription subscription -> subscription.request(1)}
        .subscribe{println "with subscription $it"}

def computeFunction = {println "\tcompute integer $it"}

//doesnt complete to 10 before it stops, need to use blockingSubscribe to get to end of list
Flowable.range(1, 10)
        .observeOn(Schedulers.computation())
        .blockingSubscribe(computeFunction)


PublishSubject<Integer> source = PublishSubject.<Integer>create()
source.subscribe({println "1st subscriber received from publishSubject  $it"})
source.subscribe({println "2nd subscriber received from publishSubject  $it"})
IntStream.range(1, 3).forEach(source::onNext)
[5..7].each source::onNext
//source.onNext(1)
//source.onNext(2)
//source.onNext(3)
source.onComplete()

source.test().assertNoValues()//test().assertEmpty()

PublishSubject<Integer> source2 = PublishSubject.<Integer>create()

source2.buffer(1024)//  window(2)//
        .observeOn(Schedulers.computation())
        .subscribe(computeFunction, Throwable::printStackTrace)

source2.onNext(15)
source2.onNext(16)
source2.onNext(17)
source2.onNext(18)
source2.onNext(19)
source2.onComplete()
//nothing is printed for window()!

/*
PublishSubject<Integer> source = PublishSubject.<Integer>create{ ObservableEmitter emitter -> emitter.onNext(value)}
IntStream.range(1, 10).forEach(source::onNext)
        source.subscribe(computeFunction, Throwable::printStackTrace)
*/

