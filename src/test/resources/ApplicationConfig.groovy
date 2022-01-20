environment = "unitTest"  // default from {unitTest|integrationTest}

framework {
    environments {
        unitTest {
            server = "local" // choice of {local|clustered}
            vertxOptions {

            }

        }
        integrationTest {
            server = "clustered" // choice of {local|clustered}
            vertxOptions {

            }

        }
    }
}