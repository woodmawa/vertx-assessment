package scripts
import actor.*
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

StandardActor  a = Actors.actor {println "my actor was called with $it "; "actorResult:$it"}

Promise p  = a.sendAndReply("will ")

p.future().onComplete(ar -> {
    println "sendAndReply future result is ${ar.result()} "
})



def vertx = Actors.vertx

Set<String> dids = Actors.vertx.deploymentIDs()
List<String> ldids = dids.collect()
def did = ldids[0]
println "script: list of deployment ids is $ldids"

println "script: actor a got deploymentId : ${a.deploymentId}"

vertx.undeploy(a.deploymentId, ar -> {
    if (ar.succeeded()) {
        println "script, undeploy handler : undeployed actor $a.name"

    } else{
        println "script, undeploy handler: undeployError: couldnt undeploy actor $a.name, because : [${ar.cause().message}]"
    }
})

vertx.close(ar -> {
    if (ar.succeeded()) {
        println "script, close handler: vertx closed ok"
    } else  {
        println "script, close handler:couldnt close vertx reason ${ar.cause().message}"
    }
})

println "script: closed vertx and exit script"