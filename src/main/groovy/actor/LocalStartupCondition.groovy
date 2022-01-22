package actor

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.AnnotationMetadataProvider

class LocalStartupCondition implements Condition{

    private ConfigObject appConfig

    private AnnotationMetadata annotationMetadata

    public LocalStartupCondition(AnnotationMetadata annotationMetadata) {
        this.annotationMetadata = annotationMetadata
    }

    public LocalStartupCondition() {

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
                if (startupMode == 'local')
                    return true
                else
                    return false
            }
        }
        false
    }

}
