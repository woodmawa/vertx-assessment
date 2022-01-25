package ioc

import actor.Actor

import javax.inject.Inject

class TestActorDI {
    @Inject Actor actor //should inject a prototype unnamed actor
}
