package com.softwood.actor

import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import jakarta.inject.Inject
import org.codehaus.groovy.runtime.MethodClosure

/**
 * base class for actors, defines standard fields
 *
 * ActorTrait  provides default method implementations for implementing actors
 *
 * @Author will woodman
 * @Date 16-02-2022
 */
abstract class AbstractActor extends AbstractVerticle implements Verticle, ActorTrait {


    @Inject ConfigObject appConfig

    //AbstractVerticle provides getVertx() ref to vertx that deployed this verticle

    protected Vertx _vertx
    protected ActorState _status = ActorState.Created
    protected Optional<String> _name = Optional.of ("${getClass().simpleName}@${Integer.toHexString(System.identityHashCode(this)) }")
    protected String _deploymentId = ""
    protected List<MessageConsumer> _consumers = []
    protected MessageConsumer _selfConsumer
    protected Closure _action = { it }  //identity closure


}
