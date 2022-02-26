package vertxfactory

import spock.lang.Specification
import org.awaitility.Awaitility

class AwaitilityAndSpockTest extends Specification{

    def "awaitility test " () {

        expect:
        Awaitility.given().failFast {false }
    }
}
