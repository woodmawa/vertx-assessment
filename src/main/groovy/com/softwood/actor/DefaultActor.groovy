package com.softwood.actor

import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j
import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.eventbus.DeliveryOptions

@InheritConstructors
@Slf4j
class DefaultActor extends AbstractActor implements Verticle, Actor {


}
