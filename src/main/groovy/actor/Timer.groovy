package actor

import groovy.transform.MapConstructor
import io.vertx.core.Future
import io.vertx.core.Vertx
import jakarta.inject.Inject
import jakarta.inject.Named

import java.time.Duration
import java.util.concurrent.CompletionStage

@MapConstructor
class Timer {
    private final long tid

    Future future

    //return scheduled timer scheduledWork Future
    Future future() {
        future
    }

    //return standard java concurrent type
    CompletionStage completionStage() {
        future?.toCompletionStage()
    }

    long getTimerId () {
        tid
    }

    Timer (long timer) {
        tid = timer
    }

    boolean cancel() {
        boolean result = Actors.getVertx().cancelTimer(tid)
        future = null
        result
    }
}
