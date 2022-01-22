package scripts

def configText = """

framework {
    server = "default"
    vertxOptions {
        map = [:]
    }
}

    environments {
        development {
            framework.server = "local" // choice of {local|clustered}
            framework.vertxOptions.map = [over:"added data"]
        }
        test {
            server = "local" // choice of {local|clustered}
            vertxOptions {

            }

        }
        production {
            server = "clustered" // choice of {local|clustered}
            vertxOptions {

            }

        }
    }

"""

ConfigObject conf = new ConfigSlurper("development").parse(configText)
println conf