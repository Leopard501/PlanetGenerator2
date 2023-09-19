package main.kotlin

import processing.core.PConstants
import processing.core.PImage
import processing.core.PVector
import java.awt.Color
import kotlin.math.sin

class Planet(private val size: Int) {

    private val cube = Cube(size)
    private val scale = 10f
    private var center = size + size / 2

    fun update() {
        if (app.frameCount % 10 != 0) return
        center++
        if (center >= size * 4) center = 0
    }

    fun display(position: PVector) {
        app.image(createImage(), position.x, position.y, scale * size, scale * size)
    }

    private fun createImage(): PImage {
        val img = app.createImage(size, size, PConstants.ARGB)
        img.loadPixels()

        var lastHX = -size / 2 + 1
        var outX = 0
        for (x in center until  center + size * 2) {
            val tx = x % (size * 4)
            val hx = ((size / 2f) * sin(((PConstants.PI / (2 * size)) * (x - center - size)).toDouble())).toInt()
            if (hx == lastHX && hx != 0) {
                lastHX = hx
                continue
            }

            var outY = 0
            var lastHY = -size / 2 + 1
            for (y in size / 2 + 1 until (size * 2.5f).toInt()){
                val hy = ((size / 2f) * sin(((PConstants.PI / (2 * size)) * ((y - 1 - size / 2) - (size))).toDouble())).toInt()

                if (hy == lastHY && hy != 0) {
                    lastHY = hy
                    continue
                }

                val i = outX + outY * size
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