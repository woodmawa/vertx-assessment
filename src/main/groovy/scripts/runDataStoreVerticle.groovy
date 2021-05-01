package scripts

import datastore.SimpleVertxDatastore
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject

//setup script logging
import org.slf4j.LoggerFactory
def log = LoggerFactory.getLogger(this.class)

Vertx vertx = Vertx.vertx()

//try and do async deploy with callback

def recordId
def send = {
    JsonObject payload = new JsonObject ()
    recordId = UUID.randomUUID().toString()
    payload.put("id", recordId).put "temp", "getting hot now"

    def res = vertx.eventBus().publish("simple.datastore", payload)
    log.debug "\t>> publish first payload"
}

def query = {recId ->
    JsonObject queryPayload = new JsonObject ()
    queryPayload.put "id", recId

    DeliveryOptions sndOptions = new DeliveryOptions()
    sndOptions.addHeader("action", "query")

    //using send - only one registered consumer will get the message
    //using publish - all consumers would be triggered
    def res = vertx.eventBus().request ("simple.datastore.query", queryPayload, sndOptions, ar -> {
        if (ar.succeeded()) {
            log.info ("query: send request, received reply : " + ar.result().body() )
        } else {
            log.error "query: request failed, reason : " + ar.cause().getMessage()
        }
    })
    //log.debug "\t>> sent query payload, got $res"
}

def undeploy = {did ->
    vertx.undeploy(did, ar -> {
        if (ar.succeeded()) {
            log.debug "\t>> undeploy vertical $did"

        } else {
            log.error "\t>> couldnt undeploy vertical $did"

        }
    })

}

String did

Future<String> state = vertx.deployVerticle("datastore.SimpleVertxDatastore", ar -> {
    if (ar.succeeded()) {
        did = ar.result()
        log.debug "\t>> successfully deployed $did"

        send ()

        query (recordId)

        //undeploy (did) //seems to erro and say verticle was undeployed !

        //needed to close vertx to exit gracefully
        //vertx.close()


    } else {
        log.error "\t>> failed to deploy SimpleVertxDatastore, reason ${ar.cause()}"
    }
})

def tid = vertx.setTimer (2000, timerIdArg-> {
    log.debug "*** in timer handler: got timer id $timerIdArg, now closing vertx"
    vertx.close()
})
println "setTimer returned timer id $tid"

Context ctx = vertx.getOrCreateContext()
ctx.runOnContext(v -> log.info ("\tscript: runOnCtx: running task via context "))


println ">>stopping the script"