package vertxfactory

import io.vertx.core.Future
import io.vertx.core.Vertx
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions
import spock.util.concurrent.BlockingVariables
import spock.util.concurrent.PollingConditions

class VertxFactoryTest extends Specification {

    def "empty"  () {
        expect:
        true
    }

    def "standalone vertx creation " () {
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

    def "standalone vertx creation version2 " () {

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
        then : "expect the result to be completed in specified time "
        conditions.within(2) {
            assert var == "set"
        }

        conditions.eventually {
            assert isClosed.isComplete()
        }
    }

    def "standalone vertx creation version 3 " () {

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
}
