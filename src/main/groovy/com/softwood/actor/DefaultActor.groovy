package com.softwood.actor

import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.eventbus.DeliveryOptions

import java.util.concurrent.TimeUnit

@InheritConstructors
@Slf4j
class DefaultActor extends AbstractActor implements Verticle, Actor {


    @Override
    def requestAndReply(Actor actor, Object args, long waitFor, TimeUnit tu) {
        return null
    }

    @Override
    def requestAndReply(Actor actor, Object args, long waitFor, TimeUnit tu, DeliveryOptions options) {
        return null
    }
}
