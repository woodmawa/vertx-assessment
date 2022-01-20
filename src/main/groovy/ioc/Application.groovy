package ioc

import io.micronaut.context.ApplicationContext

class Application {

    static main (args) {

        Map envMap = [:]
        def sysProps = System.getenv()
        if (System.getProperty("env") ) {
            envMap = System.getenv().findResult { it.key?.toLowerCase().contains "env" }.collect { [(it.key?.toLowerCase().substring(0, 2)): it.value.toLowerCase()] }
        }
        def env = envMap.get ('env', "development")
        def resourcePath = "src${File.separatorChar}${env =="test" ?: "main"}${File.separatorChar}resources${File.separatorChar}"
        ConfigObject conf = new ConfigSlurper("development").parse(new File("${resourcePath}ApplicationConfig.groovy").toURI().toURL())
        conf.put('system', sysProps)
        conf.putAll(envMap)

        println "app starting"

        ApplicationContext context = ApplicationContext.run()


        Vehicle v = context.run().getBean(Vehicle)

        println v.start()
        //println v.v4Engine.start()


        println "app stopping"

    }
}
