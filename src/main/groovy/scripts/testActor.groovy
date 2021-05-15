package scripts
import actor.*
import io.vertx.core.Promise
import io.vertx.core.eventbus.MessageConsumer
import org.codehaus.groovy.runtime.MethodClosure


MyActor myAct = new MyActor ("specific actor")
myAct.start()
myAct.run { println "myAct [${myAct.getName()}(dep:${myAct.deploymentId})] was run using dynamic dispatch, by overriding onmessage(String)  "}
myAct.timer(1) {println "timer tick fired "}
myAct.stop()

println "script>> now use Actors static invocation "
StandardActor a = Actors.actor ("fred") {println "\t--my actor.action(): actor ${getName()} action was called with [$it] "; "actorResult:$it"}
StandardActor b = Actors.actor ("mavis ") {println "\t--my actor.action(): actor ${getName()} action was called with [$it] "; "actorResult:$it"}

//blocking send&reply - no result will come back
def result  = a.requestAndReply("will")

println ">>script: sendAndReply blocking  result is ${result} "

a.run { Promise promise ->
    sleep(5)
    println "\t\t++++ run passed closure on vertx context"
    promise.complete ("run passed ok")
}

println ">>script: a.run  run code block  "

a  >>  b

a.send (10)

//asynchronous send - no result will come back
a << ["woodman", "tribe"].stream()
println ">>script: send, async and no return  "

def reports = new Address ("reports.data")

MessageConsumer consumer = a.addConsumer(reports) {
    println "reports called for with arg: ${it.body()}"
}
println ">>script new consumer registered [${consumer.address()}]"

a.publish (reports, "want my report right now please ")

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