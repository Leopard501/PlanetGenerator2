package main.kotlin

import processing.core.PConstants
import processing.core.PImage
import processing.core.PVector
import kotlin.math.sin

class Planet {

    private val cube = Cube()
    private val scale = 10f
    private var center = Cube.SIZE + Cube.SIZE / 2

    fun update() {
        if (app.frameCount % 10 != 0) return
        center++
        if (center >= Cube.SIZE * 4) center = 0
    }

    fun display(position: PVector) {
        app.image(createImage(), position.x, position.y, scale * Cube.SIZE, scale * Cube.SIZE)
    }

    private fun createImage(): PImage {
        val img = app.createImage(Cube.SIZE, Cube.SIZE, PConstants.ARGB)
        img.loadPixels()

        var lastH = -Cube.SIZE / 2 + 1
        var outX = 0
        for (x in center until  center + Cube.SIZE * 2) {
            val tx = x % (Cube.SIZE * 4)
            val hx = x - center - Cube.SIZE
            val h = ((Cube.SIZE / 2f) * sin(((PConstants.PI / (2 * Cube.SIZE)) * hx).toDouble())).toInt()
            val notDisplayed = h == lastH && h != 0
            if (notDisplayed) {
                lastH = h
                continue
            }
            for (y in 0 until  Cube.SIZE) {
                val i = outX + y * Cube.SIZE

                img.pixels[i] = cube.netPixel(tx, y + Cube.SIZE)
            }
            outX++
            lastH = h
        }

        img.updatePixels()
        return img
    }
}