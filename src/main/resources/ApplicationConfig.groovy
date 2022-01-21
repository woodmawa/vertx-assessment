

//serverMode = framework.environments."$env".server
//serverMode = framework.environments."$env".server



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