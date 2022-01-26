package ioc

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Prototype

@Factory
class EngineFactory {

    //note within the factory - Crankshaft is recognised as a bean and is injected automatically
    //without an explicit @Inject
    @Bean
    Engine v2Engine (CrankShaft crankShaft) {
        new V2Engine (crankShaft)
    }
}

