package appconfig

import groovy.yaml.YamlSlurper
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Named

@Factory
class ConfigurationFactory {

    @Bean
    @Named('config')
    ConfigObject config () {

        Map envMap = [:]
        def sysProps = System.getenv()
        if (System.getProperty("env") ) {
            envMap = System.getenv().findResult { it.key?.toLowerCase().contains "env" }.collect { [(it.key?.toLowerCase().substring(0, 2)): it.value.toLowerCase()] }
        }

        def resourcePath = "src${File.separatorChar}${env =="test" ?: "main"}${File.separatorChar}resources${File.separatorChar}"
        //use config slurper with default env setting - updates any defaults from matched environment
        ConfigObject config = new ConfigSlurper("$env").parse(new File("${resourcePath}ApplicationConfig.groovy").text )
        config.put('systemProperties', sysProps)

        config.put('projectPath', System.getProperty("user.dir"))
        config.put('resourcesPath', resourcePath.toString())

        File resourceDirectory = new File ("${System.getProperty("user.dir")}$File.separatorChar$resourcePath")
        FilenameFilter filter = {file, name -> name.matches(~/^.*properties$/) }
        List propsFiles = resourceDirectory.listFiles (filter)

        filter = {file, name -> name.matches(~/^.*yaml$/) }
        List yamlFiles = resourceDirectory.listFiles (filter)

        propsFiles.each {file ->
            Properties prop = new Properties()
            prop.load(new FileInputStream (file) )
            Map propsMap = [:]
            for (key in prop.stringPropertyNames()) {
                propsMap.put(key, prop.getProperty(key))
            }
            config.putAll(propsMap)
        }

        yamlFiles.each {file ->
            def yamlConfig = new YamlSlurper().parseText(file.text)
            config.putAll(yamlConfig)
        }

        if (envMap) //if env declared as system property update as default
            config.putAll(envMap)
        else
            envMap.get ('env', "development")  //if env not set - use development as default

        config
    }
}
