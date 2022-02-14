package com.softwood.actor

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Prototype
import io.vertx.core.Vertx
import io.vertx.core.Verticle
import io.vertx.core.Future

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Inject
import jakarta.inject.Named

import java.util.concurrent.ConcurrentHashMap

@Factory
@Slf4j
class Actors<T> {

    static Vertx vertx

    static Future futureServer

    /*
     * if static methods are used (rather than direct new action) the actor
     * returned will have been started automatically and ready for use
     */
    //constructor injection here
    @Inject Actors (@Named("Vertx") Future<Vertx> future) {
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

    static HashMap deployedActors = new ConcurrentHashMap<>()

    static List<String> activeActors () {
        vertx.deploymentIDs().collect()
    }

    static void removeDeployedActor (Actor actor) {
        vertx.undeploy(actor.deploymentId) {ar ->
            if (ar.succeeded()){
                log.info "successfully undeployed ${actor.name}"
            } else {
                log.error "failed to undeploy ${actor.name}, reason : " + ar.cause()
            }
        }
        deployedActors.remove(actor.deploymentId)
    }

    static addDeployedActor (Actor actor) {
        deployedActors.putIfAbsent (actor.deploymentId, actor)
    }
    /**
     * actor with name and optional action closure , automatically returns a started actor
     * @param String name, can be null
     * @param Closure action,  for action - default provided which just returns it
     * @return returns a started actor
     */
    static Actor actor (String name, Closure action=null) {
        StandardActor actor
        if (action)
            actor = new StandardActor (name, action)
        else
            actor = new StandardActor (name)

        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx.deployVerticle(v, {ar ->
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
    static Actor actor (Closure action=null) {
        StandardActor actor = new StandardActor (action)
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx.deployVerticle(v, {ar ->
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

    @Bean
    @Named ("Actor")
    @Prototype
    Actor actorGenerator (@Named("Vertx") Future<Vertx> future) {

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

        StandardActor actor = new StandardActor ()
        Verticle v = actor as Verticle

        //deploy this specific verticle instance
        vertx.deployVerticle(v, {ar ->
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
        vertx.undeploy(actor.deploymentId) {ar ->
            if (ar.succeeded()) {
                log.debug "undeployed actor $actor.name with deploymentId $actor.deploymentId"
                Actors.undeploy(actor )
            } else {
                log.error ("couldn't undeploy $actor.name with deploymentId [$actor.deploymentId]")
            }
        }
    }
}