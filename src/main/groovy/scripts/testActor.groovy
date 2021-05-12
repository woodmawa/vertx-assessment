package scripts
import actor.*

StandardActor a = Actors.actor ("fred") {println "\t--my actor [called: ${getName()}] action(): action was called with [$it] "; "actorResult:$it"}

//blocking send&reply - no result will come back
def result  = a.requestAndReply("will")

println ">>script: sendAndReply blocking  result is ${result} "

//needs a small amount of time <50ms before just calling send async will work
//sleep(50)

//asynchronous send - no result will come back
a << ["woodman", "tribe"].stream()
println ">>script: send, async and no return  "


//pub sub - model all consumers on the address will get the request
a.publish(" & Marian ")
println ">>script: publish, async and no return  "

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