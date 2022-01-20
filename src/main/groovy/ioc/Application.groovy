package ioc

import io.micronaut.context.ApplicationContext
import jdk.internal.jimage.ImageReader
import org.apache.tools.ant.types.selectors.TypeSelector
import groovy.yaml.YamlSlurper

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

        conf.put('projectPath', System.getProperty("user.dir"))
        conf.put('resourcesPath', resourcePath.toString())

        File resourceDirectory = new File ("${System.getProperty("user.dir")}$File.separatorChar$resourcePath")
        FilenameFilter filter = {file, name -> name.matches(~/^.*properties$/) }
        List propsFiles = resourceDirectory.listFiles (filter)

        filter = {file, name -> name.matches(~/^.*yaml$/) }
        List yamlFiles = resourceDirectory.listFiles (filter)

        propsFiles.each {file ->
            Properties prop = new Properties()
            prop.load(new FileInputStream (file) )
            Map propsMap = prop.inject([:]) {result, entry -> [entry.key:entry.value]}//prop.getProperties()
            propsMap.remove('class')
            propsMap.remove('empty')
            conf.putAll(propsMap)
        }

        yamlFiles.each {file ->
            def yamlConfig = new YamlSlurper().parseText(file.text)
            conf.putAll(yamlConfig)
        }

        ApplicationContext context = ApplicationContext.run()


        Vehicle v = context.run().getBean(Vehicle)

        println v.start()
        //println v.v4Engine.start()


        println "app stopping"

    }
}
