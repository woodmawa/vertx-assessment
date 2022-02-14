package com.softwood.actor

import groovy.util.logging.Slf4j
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.codehaus.groovy.runtime.MethodClosure

@Slf4j
class DynamicDispatchActor {


    /*
     * dynamic dispatch methods
     */
    def onMessage (def args) {
        log.info "default untyped args Actor.action invoked with $args"

        action (args)
    }

    def onMessage (Integer arg) {
        log.info "default Integer Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (BigInteger arg) {
        log.info "default BigInteger Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (Float arg) {
        log.info "default Float Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (Double arg) {
        log.info "default Double Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (BigDecimal arg) {
        log.info "default BigDecimal Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (String arg) {
        log.info "default String Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (JsonObject arg) {
        log.info "default JsonObject Actor.action invoked with $arg"

        action (arg)
    }

    def onMessage (JsonArray arg) {
        log.info "default JsonArray Actor.action invoked with $arg"

        action (arg)
    }

    //dynamic dispatch logic handled by groovy
    MethodClosure dispatchMessage = this::onMessage
    MethodClosure action = { it }  //identity closure

}
