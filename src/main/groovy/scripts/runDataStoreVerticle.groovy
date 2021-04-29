package scripts

import datastore.SimpleVertxDatastore
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

//setup script logging
import org.slf4j.LoggerFactory
def log = LoggerFactory.getLogger(this.class)

Vertx vertx = Vertx.vertx()

//try and do async deploy with callback


def send = {
    JsonObject payload = new JsonObject ()
    payload.put("id", UUID.randomUUID().toString()).put "temp", "getting hot now"

    def res = vertx.eventBus().publish("simple.datastore", payload)
    log.debug "\t>> publish first payload"
}

def undeploy = {did ->
    vertx.undeploy(did, ar -> {
        if (ar.succeeded()) {
            log.debug "\t>> undeploy vertical $did"

        } else {
            log.debug "\t>> couldnt undeploy vertical $did"

        }
    })

}

String did

Future<String> state = vertx.deployVerticle("datastore.SimpleVertxDatastore", ar -> {
    if (ar.succeeded()) {
        did = ar.result()
        log.debug "\t>> successfully deployed $did"

        send ()

        //undeploy (did) //seems to erro and say verticle was undeployed !

        //needed to close vertx to exit gracefully
        //vertx.close()


    } else {
        log.debug "\tt>> failed to deploy ${ar.cause()}"
    }
})

def tid = vertx.setTimer (2000, timerIdArg-> {
    println "in timer handler, got timer id $timerIdArg, now closing vertx"
    //undeploy (did)
    vertx.close()
})
println "setTimer returned timer id $tid"



println ">>stopping the script"