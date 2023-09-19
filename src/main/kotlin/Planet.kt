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

        var lastHX = -Cube.SIZE / 2 + 1
        var outX = 0
        for (x in center until  center + Cube.SIZE * 2) {
            val tx = x % (Cube.SIZE * 4)
            val hx = ((Cube.SIZE / 2f) * sin(((PConstants.PI / (2 * Cube.SIZE)) * (x - center - Cube.SIZE)).toDouble())).toInt()
            if (hx == lastHX && hx != 0) {
                lastHX = hx
                continue
            }

            var outY = 0
            var lastHY = -Cube.SIZE / 2 + 1
            for (y in Cube.SIZE / 2 + 1 until (Cube.SIZE * 2.5f).toInt()){
                val hy = ((Cube.SIZE / 2f) * sin(((PConstants.PI / (2 * Cube.SIZE)) * ((y - 1 - Cube.SIZE / 2) - (Cube.SIZE))).toDouble())).toInt()

                if (hy == lastHY && hy != 0) {
                    lastHY = hy
                    continue
                }

                val i = outX + outY * Cube.SIZE
                img.pixels[i] = cube.netPixel(IntVector(tx, y))
                outY++
                lastHY = hy
            }
            outX++
            lastHX = hx
        }

        img.updatePixels()
        return img
    }
}