package scripts
import actor.*
import com.softwood.actor.Actors
import com.softwood.actor.Address
import com.softwood.actor.DefaultActor
import com.softwood.actor.FirstStandardActor
import com.softwood.actor.Timer
import io.vertx.core.Promise
import io.vertx.core.eventbus.MessageConsumer

/*Future cstart = Actors.clusterInit()
cstart.onComplete(ar -> {
    if (ar.succeeded()) {
        println  "clustered vertx started successfully with ${ar.result()}"
    } else {
        println("couldn't start clustered vertx, reason : ${ar.cause().message}")
    }
})*/

//Actors.localInit()


println "waited 5 seconds to start cluster"
DefaultActor myAct = new DefaultActor ("specific actor")
myAct.start()
myAct.run { println "myAct [${myAct.getName()}(dep:${myAct.deploymentId})] was run using dynamic dispatch, by overriding onmessage(String)  "}
Timer myTimer = myAct.timer(1) { Promise promise -> println "timer tick fired "; promise.complete ("timer OK")}

myTimer.future().onComplete{println "\ttimer action  returned ${it.result()}" }
myAct.stop()

println "script>> now use Actors static invocation "
FirstStandardActor a = Actors.actor ("fred") {println "\t--my actor.action(): actor ${getName()} action was called with [$it] "; "actorResult:$it"}
FirstStandardActor b = Actors.actor ("mavis ") {println "\t--my actor.action(): actor ${getName()} action was called with [$it] "; "actorResult:$it"}

//blocking send&reply - no result will come back
def result  = a.requestAndReply("will")

println ">>script: sendAndReply blocking  result is ${result} "

a.run { Promise promise ->
    sleep(5)
    println "\t\t++++ run passed closure on vertx context"
    promise.complete ("run passed ok")
}

println ">>script: a.run  run code block  "

//a  >>  b - doesnt work yet

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

def vertx = Actors.vertx()

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