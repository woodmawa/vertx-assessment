package actor

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.AnnotationMetadataProvider
import io.vertx.core.Future

import javax.inject.Inject


class StartupCondition implements Condition{

    private ConfigObject appConfig

    private AnnotationMetadata annotationMetadata

    public StartupCondition(AnnotationMetadata annotationMetadata) {
        this.annotationMetadata = annotationMetadata
    }

    public StartupCondition () {

    }

    @Override
    public boolean matches(ConditionContext context) {
        AnnotationMetadataProvider component = context.getComponent()
        BeanContext beanContext = context.getBeanContext()
        String env

        appConfig = ApplicationContext.run().getBean (ConfigObject)  //should be inject
        if (beanContext instanceof ApplicationContext) {

            env = appConfig.env
            Map m = appConfig.flatten()
            Set s = appConfig.entrySet()
            def framework = appConfig.get ("framework")
            def environments = appConfig.get ("framework.environments")
            def devEnv = appConfig.get ("framework.environments.development")
            def server = appConfig.get ("framework.environments.development.server")

            String startupMode = appConfig.get(env)
            //hack
            startupMode ='clustered'
            if (startupMode) {
                if (startupMode == 'clustered')
                    return true
                else
                    return false
            }
        }
        false
    }

}
