package scripts
import actor.*
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

StandardActor  a = Actors.actor ("fred") {println "\t--my actor [called: ${getName()}] action(): action was called with [$it] "; "actorResult:$it"}

//blocking send&reply - no result will come back
def result  = a.sendAndReply("will")

println ">>script: sendAndReply blocking  result is ${result} "

//needs a small amount of time <50ms before just calling send async will work
//sleep(50)

//asynchronous send - no result will come back
a.send("woodman ")
println ">>script: send  async  "

//sleep(5)
//a.publish(" & Marian ")
//println ">>script: sendAndReply blocking  result is ${result} "

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