package scripts

import com.softwood.actor.DefaultActor

trait Ability {
    String getName() {nameString}  //refers to field in class
    void setName (String someName) {
        nameString = someName
    }
}

class SomeClass implements Ability {
    String nameString
}

SomeClass sc = new SomeClass(name:"will")
println sc.name

DefaultActor ma = new DefaultActor (deploymentId: "dep id will")

println ma.deploymentId