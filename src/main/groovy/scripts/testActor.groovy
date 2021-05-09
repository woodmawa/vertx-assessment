package scripts
import actor.*
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

StandardActor  a = Actors.actor {println "\t--my actors action was called with $it "; "actorResult:$it"}

//blocking send&reply - no result will come back
def result  = a.sendAndReply("will ")

println ">>script: sendAndReply blocking  result is ${result} "

//asynchronous send - no result will come back
a.send("woodman ")
a.publish(" & Marian ")


def vertx = Actors.vertx

println ">>script: actor a got deploymentId : ${a.deploymentId}"

/*
vertx.undeploy(a.deploymentId, ar -> {
    if (ar.succeeded()) {
        println "script, undeploy handler : undeployed actor $a.name"

    } else {
        println "script, undeploy handler: undeployError: couldnt undeploy actor $a.name, because : [${ar.cause().message}]"
    }
})
*/


Actors.shutdown()
println ">>script: closed vertx and exit script"