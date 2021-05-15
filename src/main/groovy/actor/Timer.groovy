package actor

import groovy.transform.MapConstructor

import java.time.Duration

@MapConstructor
class Timer {
    private final long tid

    long getTimerId () {
        tid
    }

    Timer (long timer) {
        tid = timer
    }
}
