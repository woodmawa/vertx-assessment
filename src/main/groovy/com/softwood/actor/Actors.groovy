package com.softwood.actor

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContextConfiguration
import io.micronaut.context.BeanRegistration
import io.micronaut.context.Qualifier
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Value
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.convert.value.MutableConvertibleValues
import io.micronaut.core.type.Argument
import io.micronaut.inject.BeanConfiguration
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.BeanDefinitionReference
import io.micronaut.inject.BeanIdentifier
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.inject.validation.BeanDefinitionValidator
import io.vertx.core.Vertx
import io.vertx.core.Verticle
import io.vertx.core.Future

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Inject
import jakarta.inject.Named

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

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
        println "future was " + future

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

    //todo - need to get types sorted to make this easier
    static findDeployedActorByName (String name) {
        deployedActors.elements().find {it.getName() == name}
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

    static MyActor myActor (String name, Closure action=null) {

        MyActor actor
        actor = new MyActor (name:name)

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
