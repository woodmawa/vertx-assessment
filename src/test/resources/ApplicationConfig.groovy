environment = "unitTest"  // default from {unitTest|integrationTest}


actor{
    framework {
        serverMode = "local"
        vertxOptions {

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
