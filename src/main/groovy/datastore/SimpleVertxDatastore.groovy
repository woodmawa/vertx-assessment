package datastore

import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

//Simple service to receive a message on thread and store the result with index key
@Slf4j
class SimpleVertxDatastore extends AbstractVerticle {

    Map datastore = new HashMap()   //doesnt need to concurrent as only 1 thread will update

    SimpleVertxDatastore () {
        log.info ("using logback - started datastore")
    }

    void start(Future<Void> future) {
        EventBus bus = vertx.eventBus()
        bus.<JsonObject>consumer ("simple.datastore",
                {msg ->
                    JsonObject body = msg.body()
                    String id = body.getString("id")
                    datastore[(id)] = body
                    log.info ("stored record with key $id")
                })
        log.info("-->Starting Simple datastore, registered listener on 'simple.datastore'")
    }

    void stop(Future<Void> future) {
        log.info("-->Stopping Simple datastore")
    }

    private void getRecord (Message<JsonObject> message ) {
        JsonObject body = message.body()
        String id = body.getString("id")


        JsonObject record = datastore.id
        message.reply(record)

    }
}