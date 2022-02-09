package vertxfactory

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.AnnotationMetadataProvider

import jakarta.inject.Inject
import jakarta.inject.Named

//@Requires (beans = [ConfigObject])
//@Prototype
@Slf4j
class LocalStartupCondition implements Condition{

    private ConfigObject appConfig

    @Inject  void setAppConfig (@Named ('appConfig') ConfigObject config) {
        log.debug "localStartup : condition injected with appConfig  $config"
        appConfig = config
    }

    AnnotationMetadata annotationMetadata

    LocalStartupCondition(AnnotationMetadata annotationMetadata) {
        this.annotationMetadata = annotationMetadata
    }

    LocalStartupCondition() {
        log.debug "localStartupCondition - default constructor called"
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
                if (startupMode == 'local') {
                    log.debug "localStartup matches with startup mode 'local', return true  "
                    return true
                }
                else
                    return false
            }
        }
        false
    }

}
