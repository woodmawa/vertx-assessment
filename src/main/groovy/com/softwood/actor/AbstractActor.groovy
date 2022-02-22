package com.softwood.actor

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
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
@Slf4j
abstract class AbstractActor extends AbstractVerticle implements Verticle, ActorTrait {


    //@Inject ConfigObject appConfig - seems to fail here - try direct read

    ConfigObject appConfig = ApplicationContext.run().getBean(ConfigObject)

    protected Vertx _vertx
    protected ActorState _status = ActorState.Created
    protected Optional<String> _name = Optional.of ("${getClass().simpleName}@${Integer.toHexString(System.identityHashCode(this)) }")
    protected String _deploymentId = ""
    protected List<MessageConsumer> _consumers = []
    protected MessageConsumer _selfConsumer
    protected Closure _action = { it }  //identity closure

    protected CircuitBreaker _breaker  //used for retry handling
    protected Optional<Closure> _optionalExceptionStrategy = Optional.empty()

    //default constructor
    AbstractActor () {

        def vertx = Actors.vertx

        assert appConfig.actor, "ConfigObject was not initialised correctly "  // check this has been loaded correctly
        long timeout = appConfig.actor.framework.circuitBreaker.timeout
        long resetTimeout = appConfig.actor.framework.circuitBreaker.resetTimeout
        int retries = appConfig.actor.framework.circuitBreaker.retries

        _breaker = CircuitBreaker.create("actor-circuit-breaker", vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(retries) // number of failure before opening the circuit
                        .setTimeout(timeout) // consider a failure if the operation does not succeed in time
                        .setFallbackOnFailure(true) // do we call the fallback on failure
                        .setResetTimeout(resetTimeout) // time spent in open state before attempting to re-try
        )


    }

    /*
     * used as method closure for use with vertx futures  onFailure (Handler<Throwable>)
     * can be overriden - or just set the exception strategy closure
     */
    def processException (Throwable t) {
        //if there is an exeption strategy set - call it - else just print stacktrace as default

        log.debug "actor processing failed with $t.message, call actor's error strategy where this is defined $_optionalExceptionStrategy"
        _optionalExceptionStrategy.orElse({Throwable::printStackTrace}).call()
        t
    }

}
