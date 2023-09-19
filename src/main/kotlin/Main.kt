package main.kotlin

import processing.core.PApplet
import processing.core.PConstants
import processing.core.PVector

typealias Position = Pair<IntVector, Cube.Face>

var app = Main()

fun main() {
    PApplet.main("main.kotlin.Main")
}

class Main: PApplet() {

    private lateinit var cube: Cube
    private lateinit var planet: Planet

    override fun settings() {
        size(900, 600)
        noSmooth()

        app = this
    }

    override fun setup() {
        imageMode(PConstants.CENTER)
        surface.setTitle("Experiment")

        cube = Cube()
        planet = Planet()
    }

    override fun draw() {
        background(0)

        planet.display(PVector(width / 2f, height / 2f))
        planet.update()
//        cube.display(PVector(width / 2f, height /2f))
    }
}

// https://github.com/nickoala/kproc/blob/master/balls/src/procexxing.kt
operator fun PVector.plus(v: PVector): PVector {
    return PVector.add(this, v)
}

operator fun PVector.minus(v: PVector): PVector {
    return PVector.sub(this, v)
}

operator fun PVector.times(n: Float): PVector {
    return PVector.mult(this, n)
}

operator fun PVector.div(n: Float): PVector {
    return PVector.div(this, n)
}