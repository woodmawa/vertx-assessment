package scripts

import datastore.SimpleVertxDatastore
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

Vertx vertx = Vertx.vertx()

Future<String> state = vertx.deployVerticle(new datastore.SimpleVertxDatastore())

println "state of future is  ${state.result()}"

def success = {
    println "success handler called - publish first payload"
    JsonObject payload = new JsonObject ()
    payload.put(UUID.randomUUID().toString()).put "temp", "getting hot now"

    vertx.eventBus().publish("simple.datastore", payload)
}

state.onSuccess({
    success()
})

state.eventually({
    vertx.close()
})

println "stopping the script"