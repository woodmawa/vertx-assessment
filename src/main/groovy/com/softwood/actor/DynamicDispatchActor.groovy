package com.softwood.actor

import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.codehaus.groovy.runtime.MethodClosure

@Slf4j
@MapConstructor
class DynamicDispatchActor extends DefaultActor {

    //dynamic dispatch logic handled by groovy
    MethodClosure dynamicDispatchMessage = this::onMessage

    Closure dynamicActionClosure

    DynamicDispatchActor () {
        //change the inherited action closure to dynamic dispatch
        setAction (dynamicDispatchMessage)
    }

    DynamicDispatchActor (String name, Closure action = null ) {
        //change the inherited action closure to dynamic dispatch
        super()
        setName (name)
        if (action != null) {

            dynamicActionClosure = action.clone()
            dynamicActionClosure.delegate = this
        }

        assert _action == dynamicDispatchMessage
        assert getAction() == dynamicDispatchMessage
    }

    @Override
    Closure getAction () {
        dynamicDispatchMessage
    }

    @Override
    void setAction (Closure action ) {
        dynamicDispatchMessage = action.clone()
        dynamicDispatchMessage.delegate = this
        super.setAction(dynamicDispatchMessage)
    }


    /*
     * dynamic dispatch methods
     */
    def onMessage (def args) {
        log.info "default untyped args Actor.onMessage invoked with $args"

        dynamicActionClosure (args)
    }

    def onMessage (Integer arg) {
        log.info "default Integer Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (BigInteger arg) {
        log.info "default BigInteger Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (Float arg) {
        log.info "default Float Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (Double arg) {
        log.info "default Double Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (BigDecimal arg) {
        log.info "default BigDecimal Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (String arg) {
        log.info "default String Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (JsonObject arg) {
        log.info "default JsonObject Actor.onMessage invoked with $arg"

        dynamicActionClosure (arg)
    }

    def onMessage (JsonArray arg) {
        log.info "default JsonArray Actor.action invoked with $arg"

        dynamicActionClosure (arg)
    }


}
