package datastore

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

//Simple service to receive a message on thread and store the result with index key
@Slf4j
class SimpleVertxDatastore extends AbstractVerticle {

    Map datastore = new HashMap()   //doesnt need to concurrent as only 1 thread will update

    SimpleVertxDatastore () {
        println ("verticle constructor:  using logback - started datastore")
    }

    @Override
    void start(Promise<Void> promise) {

        println "verticle SimpleVertxDatastore starting "


        EventBus bus = vertx.eventBus()
        bus.<JsonObject>consumer ("simple.datastore",
                {msg ->
                    JsonObject body = msg.body()
                    String id = body.getString("id")
                    datastore[(id)] = body
                    println ("stored record with key $id")

                    log.debug ("stored record with key $id")
                })
        promise.complete()
        println ("-->Starting Simple datastore, registered listener on 'simple.datastore'")

        log.debug ("-->Starting Simple datastore, registered listener on 'simple.datastore'")
    }

    @Override
    void stop (Promise<Void> future) {
        log.debug ("-->Stopping Simple datastore")
        future.complete()
    }

    private void getRecord (Message<JsonObject> message ) {
        JsonObject body = message.body()
        String id = body.getString("id")


        JsonObject record = datastore.id
        message.reply(record)

    }
}