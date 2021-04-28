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
        log.debug ("verticle constructor:  using logback - started datastore")
    }

    @Override
    void start(Promise<Void> promise) {

        log.debug "verticle SimpleVertxDatastore starting "

        //abstractVerticle pre declares the vertx variable for you
        EventBus bus = vertx.eventBus()

        log.debug "registering listener on 'simple.datastore' with callback handler "
        bus.<JsonObject>consumer ("simple.datastore",
                {msg ->

                    log.debug ("consumer: received message from eventBus  $msg")

                    JsonObject body = msg.body()
                    String id = body.getString("id")
                    datastore[(id)] = body

                    log.debug ("consumer: stored record with key $id, #messages stored now: ${datastore.size()}")
                })

        //mark the promise as complete
        promise.complete()
        log.debug ("-->Starting Simple datastore, registered listener on 'simple.datastore'")
    }

    @Override
    void stop (Promise<Void> promise) {
        log.debug ("-->Stopping Simple datastore")
        log.debug "datastore now contains: $datastore"
        promise.complete()
    }

    private void getRecord (Message<JsonObject> message ) {
        JsonObject body = message.body()
        String id = body.getString("id")


        JsonObject record = datastore.id
        message.reply(record)

    }
}