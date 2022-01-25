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
        //see if env is defined in system properties first
        if (sysProps["env"] ) {
            envMap.put ("env", sysProps["env"])
        }

        //if not defined set default in envMap as development
        def env = envMap.get ('env', "development")

        //based on this value figure out what the project resources path should be (src/main or src/test)
        def resourcePath = "src${File.separatorChar}${env.contains ("test") ? "test" : "main"}${File.separatorChar}resources${File.separatorChar}"

        //get lst of *.properties and any yaml files and process those
        File resourceDirectory = new File ("${System.getProperty("user.dir")}$File.separatorChar$resourcePath")
        FilenameFilter filter = {file, name -> name.matches(~/^.*properties$/) }
        List propsFiles = resourceDirectory.listFiles (filter)

        filter = {file, name -> name.matches(~/^.*yaml$/) }
        List yamlFiles = resourceDirectory.listFiles (filter)

        Map propsMap = [:]
        propsFiles.each {file ->
            Properties prop = new Properties()
            prop.load(new FileInputStream (file) )
            for (key in prop.stringPropertyNames()) {
                propsMap.put(key, prop.getProperty(key))
            }
        }

        def yamlConfig
        yamlFiles.each {file ->
            yamlConfig = new YamlSlurper().parseText(file.text)
        }

        //use config slurper with default env setting - updates any defaults from matched environment
        ConfigObject appConfig = new ConfigSlurper("$env").parse(new File("${resourcePath}ApplicationConfig.groovy").text )

        ConfigObject finalConfig = new ConfigObject()

        //build final config starting with lowest hierarchy sources
        // 1. properties file first
        // 2. overlayed by yaml files
        // 3. overlayed by applicationConfig.groovy entries

        finalConfig.putAll (propsMap)
        finalConfig.putAll (yamlConfig)
        finalConfig.merge(appConfig)
        if (!finalConfig.get("env"))
            finalConfig.putAll(envMap)  //if env not defined yet, add initial env from envMap

        finalConfig.put('systemProperties', sysProps)
        finalConfig.put('projectPath', System.getProperty("user.dir"))
        finalConfig.put('resourcesPath', resourcePath.toString())

        finalConfig
    }
}
