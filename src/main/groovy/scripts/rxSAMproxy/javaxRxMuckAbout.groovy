package scripts.rxSAMproxy


import java.util.concurrent.Flow

import java.util.concurrent.SubmissionPublisher



List consumedElements = new LinkedList<>()
Flow.Subscription subscription

/*
//proxy SAM impl using map, with methods as key names, and closure implementations
proxy = [onSubscribe:{subscrip -> subscription = subscrip; subscription.request(1) },
             onNext:{item -> println "got next item $item"; consumedElements << item; subscription.request(1)},
             onError:{Throwable::printStackTrace},
             onComplete:{println "all done processing from publisher"}]

  */

//use this from java.util.concurrent as an impl
SubmissionPublisher publisher = new SubmissionPublisher<>()

public class MySubscriber implements Flow.Subscriber {
    private Flow.Subscription subscription;
    public List consumedElements = new LinkedList<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        println "\t>>Subscriber::onSubscribe:  called with subscription $subscription"
        this.subscription = subscription
        subscription.request(1)
    }

    @Override
    void onNext(Object item) {
        println "\t>>Subscriber::onNext: got next item $item"
        consumedElements << item
        subscription.request(1)
    }

    @Override
    void onError(Throwable throwable) {
        println "\t>>Subscriber::onError: got $throwable"

    }

    @Override
    void onComplete() {
        println "\t>>Subscriber::onComplete: all done "

    }
}

MySubscriber subscriber = new MySubscriber()

publisher.subscribe(subscriber)

println "subscribers : " + publisher.getNumberOfSubscribers()

println "-- publisher submit data "
publisher.submit("hello")
publisher.submit("world")
println "-- publisher close()"
publisher.close()


sleep(1000)
/*
Flow.Subscriber subs = {
    def onSubscribe (Flow.Subscription subscrip) {
        return {Flow.Subscription subscr -> subscription = subscr; subscription.request(1)}
    }
}
*/

//fails with pre processing
/*
def map
map = [
        i: 10,
        hasNext: { map.i > 0 },
        next: { map.i-- },
]
def iter = map as Iterator
iter.each {println it}


 */

/*
interface Thing {
    def onSubscribe (subs)
    def onNext (value)
}

Thing thing = proxy as Thing

def receiver (Thing anyold) {
    anyold.onNext (10)
}
*/
//Flow.Subscriber subscriber = proxy.asType (Flow.Subscriber)

