package main.kotlin

import processing.core.PApplet.*
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * The simulated surface of a planet
 *
 * @param size size of each face in pixels
 * @param cube cube this surface will be stretched over
 */
class PlanetSurface(val size: Int, val cube: Cube) {

    private val north = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.North), this) }
    private val south = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.South), this) }
    private val east = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.East), this) }
    private val west = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.West), this) }
    private val obverse = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.Obverse), this) }
    private val reverse = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.Reverse), this) }

    private val pixels = arrayOf(*north, *south, *east, *west, *obverse, *reverse)
    private val equatorialPixels = arrayOf(*east, *west, *obverse, *reverse)
    private val polarPixels = arrayOf(*north, *south)
    private var volcanoes = ArrayList<Pixel>()

    val solarEnergy = 5f
    val heatRadiation = 0.0115f
    val heatConductivity = 1

    init {
        for (p in pixels) {
            p.temperature = 350f
            p.material = Material.Metamorphic
            p.elevation = app.random(-0.2f, 0.2f)
            p.liquid = Liquid.SaltWater
            p.liquidDepth = app.random(2f)
        }

        for (i in 0 until 40) {
            placeRoundFeature(cube.randomPosition(), app.random(-5f, 6f), app.random(0.5f, 2f))
        }
    }

    companion object {
        /**
         * Maps a value between a range to a color between two colors
         */
        fun mapColor(a: Color, b: Color, low: Float, high: Float, value: Float): Color {
            var r = map(value, low, high, a.red.toFloat(), b.red.toFloat())
            var g = map(value, low, high, a.green.toFloat(), b.green.toFloat())
            var bl = map(value, low, high, a.blue.toFloat(), b.blue.toFloat())

            // Color expects a range of 0 to 1
            r = map(r, 0f, 255f, 0f, 1f).coerceIn(0f, 1f)
            g = map(g, 0f, 255f, 0f, 1f).coerceIn(0f, 1f)
            bl = map(bl, 0f, 255f, 0f, 1f).coerceIn(0f, 1f)

            return Color(r, g, bl)
        }
    }

    fun update() {
        pixels.forEach { it.update() }
        volcanoes.forEach { it.erupt() }

        if (app.random(120f) < 1) volcanoes.add(pixels[app.random((pixels.size - 1).toFloat()).toInt()])
        if (app.random(40f) < 1 && volcanoes.isNotEmpty()) volcanoes.removeFirst()
    }

    /**
     * Draws pixels onto the faces of the cube
     */
    fun updateCube() {
        for (f in Cube.FaceType.values()) {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val face = when (f) {
                        Cube.FaceType.North -> north
                        Cube.FaceType.South -> south
                        Cube.FaceType.East -> east
                        Cube.FaceType.West -> west
                        Cube.FaceType.Obverse -> obverse
                        Cube.FaceType.Reverse -> reverse
                    }

                    cube.setPixel(IntVector(x, y), f, face[x + y * size].getColor())
                }
            }
        }
    }

    /**
     * @param i index of pixel in face array
     * @return position of pixel on face
     */
    private fun positionFromIndex(i: Int): IntVector {
        val x = i % size
        val y = (i - x) / size

        return IntVector(x, y)
    }

    /**
     * @param position x, y and face of pixel
     * @return the pixel that exists at that location
     */
    fun pixelAtPosition(position: Position): Pixel {
        return when (position.second) {
            Cube.FaceType.North -> north[position.first.x + position.first.y * size]
            Cube.FaceType.South -> south[position.first.x + position.first.y * size]
            Cube.FaceType.East -> east[position.first.x + position.first.y * size]
            Cube.FaceType.West -> west[position.first.x + position.first.y * size]
            Cube.FaceType.Obverse -> obverse[position.first.x + position.first.y * size]
            Cube.FaceType.Reverse -> reverse[position.first.x + position.first.y * size]
        }
    }

    /**
     * Places a circular feature.
     * NOTE: will get cut off if height goes beyond the acceptable elevation range
     *
     * @param position where should the center of the feature be
     * @param height how to change the elevation of the center pixel.
     * @param falloff how much should the change decrease per pixel away from the center
     */
    private fun placeRoundFeature(position: Position, height: Float, falloff: Float) {
        val radius: Int = ceil((height.absoluteValue / falloff)).toInt()
        for (x in -radius .. radius) {
            for (y in -radius .. radius) {
                val distance = sqrt(x.toFloat().pow(2) + y.toFloat().pow(2))
                val change = (1 - distance.coerceAtMost(height.absoluteValue) / height.absoluteValue) * height
                pixelAtPosition(cube.changePositionSpherical(position, IntVector(x, y))).changeElevation(change)
            }
        }
    }
}