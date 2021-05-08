package scripts
import actor.*
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

StandardActor  a = Actors.actor {println "my actor was called with $it "; "actorResult:$it"}

EventBus bus = a.sendAndReply("will ")

Vertx vertx = Vertx.vertx()
//vertx.undeploy (a.deploymentId)

println "actor a got deploymentId : ${a.deploymentId}"

vertx.undeploy(a.deploymentId, ar -> {
    if (ar.succeeded()) {
        println "undeployed actor $a.name"
    } else{
        println "undeployError: couldnt undeploy actor $a.name, because : [${ar.cause().message}]"
    }
})

vertx.close(ar -> {
    if (ar.succeeded()) {
        println "vertx closed ok"
    } else  {
        println "couldnt close vertx reason ${ar.cause().message}"
    }
})

println "closed vertx and exit script"