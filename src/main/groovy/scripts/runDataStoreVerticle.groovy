package scripts

import datastore.SimpleVertxDatastore
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

Vertx vertx = Vertx.vertx()

//try and do async deploy with callback


def send = {
    JsonObject payload = new JsonObject ()
    payload.put("id", UUID.randomUUID().toString()).put "temp", "getting hot now"

    def res = vertx.eventBus().publish("simple.datastore", payload)
    println "\t>> publish first payload"
}

String did
Future<String> state = vertx.deployVerticle("datastore.SimpleVertxDatastore", ar -> {
    if (ar.succeeded()) {
        did = ar.result()
        println "\t>> successfully deployed $did"

        send ()

        vertx.undeploy(did, aru -> {
            if (aru.succeeded()) {
                println "\t>> undeploy vertical $did"

            } else {
                println "\t>> couldnt undeploy vertical $did"

            }
        }
    )

    } else {
        println "\tt>> failed to deploy ${ar.cause()}"
    }
})

//println "\t>> state of future is  ${state.result()}"






sleep(1)


println "stopping the script"