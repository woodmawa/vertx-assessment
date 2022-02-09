package vertxfactory

import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.vertx.core.Future
import io.vertx.core.Vertx
import jakarta.inject.Inject
import jakarta.inject.Qualifier
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions
import spock.util.concurrent.BlockingVariables
import spock.util.concurrent.PollingConditions

@MicronautTest
class VertxFactoryTest extends Specification {

    @Inject @Shared ApplicationContext context

    @Shared
    Vertx vertxFromFactory

    def setupSpec () {

        ApplicationContext ctx = context
        assert ctx, "ApplicationContext context DI didnt work "  //check DI fired
        println "setupSpec:setup conditions for all tests"
    }

    def cleanupSpec () {
        println "cleanup after all tests "
        vertxFromFactory?.close {ar ->
            if (ar.failed()) {
                println "cleanupSpec:failed to close factory vertx " + ar.cause().message
            } else {
                println "cleanupSpec:successfully closed vertx "
            }
        }
    }

    def "standalone vertx creation, async.await version  " () {
        given: "a new vertx"
            def vertx = Vertx.vertx()
            def var
        and: "an instance of AsyncConditions"
            def async = new AsyncConditions(1)  //expected number of evaluate blocks

        when: "we do an async evaluation "
        async.evaluate {println "hello world"; var = "set"}  //void return
        async.evaluate {println "hello Will"}  //void return - thought this might fail test - it doesnt

        then : "expect the result to be completed in specified time "
        async.await(2.0)
    }

    def "standalone vertx creation using pollingConditions version2 " () {

        def conditions = new PollingConditions(timeout: 10)

        given: "a new vertx"

        def vertx = Vertx.vertx()
        def var
        and: "an instance of AsyncConditions"
        def async = new AsyncConditions(1)  //expected number of evaluate blocks

        when: "we do an async evaluation "
        async.evaluate {println "hello world"; var = "set"}  //void return
        async.evaluate {println "hello Will" }  //void return - thought this might fail test - it doesnt

        Future isClosed = vertx.close()

        //can put this in the then block - but these methods are void return so maybe best in then and wait
        conditions.within(2) {
            assert var == "set"
        }

        conditions.eventually {
            assert isClosed.isComplete()
        }

        then : "expect the result to be completed in specified time "
        noExceptionThrown()
    }

    def "standalone vertx creation, using blockingVariables  version 3 " () {

        BlockingVariables vars = new BlockingVariables(3.0)  //behaves like a blocking map, in this case with timeout


        given: "a new vertx"
        def vertx = Vertx.vertx()
        and: "an instance of AsyncConditions"
        def async = new AsyncConditions(1)  //expected number of evaluate blocks

        when: "we do an async evaluation "
        async.evaluate {println "hello world"; vars.result = "set"}  //void return
        async.evaluate {println "hello Will"}  //void return - thought this might fail test - it doesnt

        Future isClosed = vertx.close()

        then : "expect the result to be completed in specified time "
        vars.result == "set"  //will block until get completes
    }

    def "create vertx from factory "() {

        when:
        //vertxFromFactory = context.get(Future<Vertx>, Qualifiers.byName("Vertx"))
        Future<Vertx> futVertx  = context.getBean (Future<Vertx>,Qualifiers.byName("Vertx"))
        def list = context.getBeansOfType(Future<Vertx>)
        if (futVertx.succeeded())
            vertxFromFactory = futVertx.result()

        then: "should see Vertx in DI context "
        context != null
        context.containsBean(Future<Vertx>)

        futVertx.succeeded()
        vertxFromFactory.isClustered() == false
    }

    def "get vertx from factory "() {


        when:
        Optional<Vertx> opt = VertxFactory::vertx(context)

        then:
        opt.isPresent()
    }

}
