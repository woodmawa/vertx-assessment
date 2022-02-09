package vertxfactory

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.AnnotationMetadataProvider
import io.vertx.core.Future

import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

//@Requires (beans = [ConfigObject])
//@Prototype
//@Singleton
@Slf4j
class ClusteredStartupCondition implements Condition{

    private ConfigObject appConfig

    @Inject  void setAppConfig (@Named ('appConfig') ConfigObject config) {
        log.debug "clusteredStartup : condition injected with appConfig  $config"
        appConfig = config
    }

    private AnnotationMetadata annotationMetadata


    ClusteredStartupCondition(AnnotationMetadata annotationMetadata) {
        this.annotationMetadata = annotationMetadata
    }

    ClusteredStartupCondition() {
        log.debug "clusteredStartupCondition - default constructor() called "
    }

    @Override
    public boolean matches(ConditionContext context) {
        AnnotationMetadataProvider component = context.getComponent()
        BeanContext beanContext = context.getBeanContext()
        String env

        appConfig = ApplicationContext.run().getBean (ConfigObject)  //should be inject
        if (beanContext instanceof ApplicationContext) {


            String startupMode = appConfig.framework.serverMode
            if (startupMode) {
                if (startupMode == 'clustered') {
                    log.debug "clusteredStartup matches with startup mode 'clustered', return true  "
                    return true
                }
                else
                    return false
            }
        }
        false
    }

}
