

framework {
    serverMode = "local"
    vertxOptions {

    }
}



//config slurper will update framework from any matched environment in constructor call
environments {
    development {
        framework.serverMode = "local" // choice of {local|clustered}
        framework.vertxOptions = [:]

    }
    test {
        framework.serverMode = "local" // choice of {local|clustered}
        framework.vertxOptions = [:]

    }
    production {
        framework.serverMode = "clustered" // choice of {local|clustered}
        framework.vertxOptions = [:]

    }
}
