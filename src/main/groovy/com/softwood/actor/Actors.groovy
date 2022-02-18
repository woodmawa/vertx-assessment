package com.softwood.actor

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Prototype
import io.micronaut.inject.qualifiers.Qualifiers
import io.vertx.core.Vertx
import io.vertx.core.Verticle
import io.vertx.core.Future

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Inject
import jakarta.inject.Named

import java.util.concurrent.ConcurrentHashMap

/**
 * standard interface spec for base actors capabilities
 *
 *
 * @author Will Woodman
 * @date 16-02-2022
 */

@Factory
@Slf4j
class Actors<T> {

    static Vertx vertx

    static Future futureServer

    /*
     * if you want to use the static class methods we need means to
     * initialise the vertx.  The vertx uses conditional bean management based on the
     * env variable to decide wither to build a local or clustered vertx
     */
    static {
        ApplicationContext context = ApplicationContext.run()
        if (!futureServer ) {
            futureServer = context.getBean(Future<Vertx>, Qualifiers.byName("Vertx"))
            futureServer.onComplete { ar ->
                if (ar.succeeded()) {
                    log.debug "running static initialisation for Actors vertx "
                    vertx = ar.result()
                } else {
                    log.error("static initialiser failed to initialise Actor vertx : ") + ar.cause().message
                }
            }
        }
    }

    /*
     * if static methods are used (rather than direct new action) the actor
     * returned will have been started automatically and ready for use
     */
    //constructor injection here
    @Inject Actors (@Named("Vertx") Future<Vertx> future) {
        log.debug "Actors constructor: injected future $future"

        if (!futureServer) {
            futureServer = future
            assert futureServer.isComplete()
            futureServer.onComplete { ar ->
                if (ar.succeeded()) {
                    vertx = ar.result()
                } else {
                    ar.cause().printStackTrace()
                }
            }
            log.debug "Actors constructor: first time initialisation, injected future $future and set vertx as $vertx"
        }
    }

    static HashMap<String, Actor> deployedActors = new ConcurrentHashMap<>()

    static Map getDeployedActors () {
        deployedActors.asImmutable()
    }

    static List<String> activeActors () {
        vertx.deploymentIDs().collect().asImmutable()
    }

    //todo - need to get types sorted to make this easier
    static List findDeployedActorsByName (String name) {
        List actors = deployedActors.values().toList()

        def actor = actors.findAll {it.getName() == name}.asImmutable()
    }

    private static void addDeployedActor (Actor actor) {
        deployedActors.putIfAbsent (actor.deploymentId, actor)
    }

    //private used internally when undeploying an actor
    private static void removeDeployedActor (Actor actor) {
        deployedActors.remove (actor.deploymentId)
    }

    static Actor defaultActor(String name, Closure action=null) {

        DefaultActor actor
        actor = new DefaultActor (name:name)

        //deploy this specific verticle instance
        vertx.deployVerticle(actor, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()

                addDeployedActor(actor)
                actor.status = ActorState.Running

                log.debug ("Actors.defaultActor(): started verticle $this successfully and got deploymentId ${ar.result()}")

            } else {
                log.debug ("Actors.defaultActor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor

    }

    static Actor dynamicDispatchActor(String name, Closure action=null) {

        DynamicDispatchActor actor
        actor = new DynamicDispatchActor (name:name, dynamicActionClosure:action ?: {it})

        //deploy this specific verticle instance
        vertx.deployVerticle(actor, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()

                addDeployedActor(actor)
                actor.status = ActorState.Running

                log.debug ("Actors.dynamicDispatchActor(): started verticle $this successfully and got deploymentId ${ar.result()}")

            } else {
                log.debug ("Actors.dynamicDispatchActor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor

    }

    /**
     * actor with name and optional action closure , automatically returns a started actor
     * @param String name, can be null
     * @param Closure action,  for action - default provided which just returns it
     * @return returns a started actor
     */
    static Actor fStandardActor (String name, Closure action=null) {
        FirstStandardActor actor
        if (action)
            actor = new FirstStandardActor (name, action)
        else
            actor = new FirstStandardActor (name)

        //deploy this specific verticle instance
        vertx.deployVerticle(actor, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                addDeployedActor(actor)
                actor.status = ActorState.Running


                log.debug ("Actors.actor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor

    }


    /**
     * actor with optional action closure , automatically returns a started actor
     * @param Closure action,  for action - default provided which just returns it
     * @return returns a started actor
     */
    static Actor fStandardActor (Closure action=null) {
        FirstStandardActor actor = new FirstStandardActor (action)

        //deploy this specific verticle instance
        vertx.deployVerticle(actor, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                addDeployedActor(actor)
                actor.status = ActorState.Running


                log.debug ("Actors.actor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.actor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    //todo : trying to inject standard actor bean fails - not sure why
    @Bean
    @Named ("StandardActor")
    @Prototype
    Actor standardActorGenerator (@Named("Vertx") Future<Vertx> future) {

        if (!futureServer) {
            futureServer = future
            assert futureServer.isComplete()
            log.warn "standardActorGenerator found uninitialsed vertx state, attempted fix : injected future $future and set vertx as $vertx"

            futureServer.onComplete { ar ->
                if (ar.succeeded()) {
                    vertx = ar.result()

                } else {
                    ar.cause().printStackTrace()
                }
            }
        }

        FirstStandardActor actor = new FirstStandardActor()
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx.deployVerticle(v, { ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                addDeployedActor(actor)
                actor.status = ActorState.Running

                log.debug("Actors.standardActor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug("Actors.standardActor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    @Bean
    @Named ("DefaultActor")
    @Prototype
    Actor defaultActorGenerator (@Named("Vertx") Future<Vertx> future) {

        if (!futureServer) {
            futureServer = future
            assert futureServer.isComplete()
            futureServer.onComplete { ar ->
                if (ar.succeeded()) {
                    vertx = ar.result()

                } else {
                    ar.cause().printStackTrace()
                }
            }
            println "actorGenerator found uninitialsed vertx state, attempted fix : injected future $future and set vertx as $vertx"
        }

        DefaultActor actor = new DefaultActor ()

        //deploy this specific verticle instance
        vertx.deployVerticle(actor, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                addDeployedActor(actor)
                actor.status = ActorState.Running

                log.debug ("Actors.standardActor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.standardActor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    @Bean
    @Named ("DynamicDispatchActor")
    @Prototype
    Actor dynamicDispatchActorGenerator (@Named("Vertx") Future<Vertx> future) {

        if (!futureServer) {
            futureServer = future
            assert futureServer.isComplete()
            futureServer.onComplete { ar ->
                if (ar.succeeded()) {
                    vertx = ar.result()

                } else {
                    ar.cause().printStackTrace()
                }
            }
            log.info  "dynamicDisptachActorGenerator found uninitialsed vertx state, attempted fix : injected future $future and set vertx as $vertx"
        }

        DynamicDispatchActor actor = new DynamicDispatchActor ()

        //deploy this specific verticle instance
        vertx.deployVerticle(actor, {ar ->
            if (ar.succeeded()) {
                actor.deploymentId = ar.result()
                addDeployedActor(actor)
                actor.status = ActorState.Running

                log.debug ("Actors.dynamicDispatchActor(): started verticle $this successfully and got deploymentId ${ar.result()}")

                //whoopee
            } else {
                log.debug ("Actors.dynamicDispatchActor(): deployVerticle $this encountered a problem ${ar.cause().message}")
            }
        })

        actor
    }

    static shutdown () {
        log.debug ">>Actors.shutdown(): ${deployedActors.size()} to be shutdown "
        vertx.close(ar -> {
            if (ar.succeeded()) {
                log.debug ">> Actors.shutdown(): close handler: actor network closed successfully "
                deployedActors.clear()
            } else  {
                log.debug ">> Actors.shutdown(): close handler:couldnt close actor network,  reason ${ar.cause().message}"
            }
        })

        futureServer = null
        vertx = null
    }

    static undeploy (Actor actor) {
        assert actor.deploymentId

        vertx.undeploy(actor.deploymentId) {ar ->
            if (ar.succeeded()) {
                log.debug "Actors.undeploy: undeployed actor $actor.name with deploymentId $actor.deploymentId"
                Actors.removeDeployedActor (actor)
                ActorTrait asActor = actor as ActorTrait
                asActor.with {
                    setStatus ( ActorState.Stopped )
                    setDeploymentId("")
                    removeConsumer (getSelfConsumer())
                    setSelfConsumer(null)
                }
            } else {
                log.error ("Actors.undeploy: couldn't undeploy $actor.name with deploymentId [$actor.deploymentId]")
            }
        }
    }
}
