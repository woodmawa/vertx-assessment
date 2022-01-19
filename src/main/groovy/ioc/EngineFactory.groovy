package ioc

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Prototype

@Factory
class EngineFactory {

    @Bean
    Engine v2Engine (CrankShaft crankShaft) {
        new V2Engine (crankShaft)
    }
}

