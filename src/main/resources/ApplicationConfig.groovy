

actor{
    framework {
        serverMode = "local"
        vertxOptions {

        }

        circuitBreaker{
            retries = 2
            timeout = 3_000         // consider if failure if circuit breaker execute() doesnt succeed
            resetTimeout = 10_000   //time spent in open state before attempting to reset
        }
    }
}



//config slurper will update framework from any matched environment in constructor call
environments {
    development {
        actor.framework.serverMode = "local" // choice of {local|clustered}
        actor.framework.vertxOptions = [:]

    }
    test {
        actor.framework.serverMode = "local" // choice of {local|clustered}
        actor.framework.vertxOptions = [:]

    }
    production {
        actor.framework.serverMode = "clustered" // choice of {local|clustered}
        actor.framework.vertxOptions = [:]

    }
}
