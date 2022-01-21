package scripts

def configText = """

framework {
    environments {
        development {
            server = "local" // choice of {local|clustered}
            vertxOptions {

            }

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
}
"""

ConfigObject conf = new ConfigSlurper().parse(configText)
assert conf.framework?.environments?.size() == 3