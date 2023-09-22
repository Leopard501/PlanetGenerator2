package main.kotlin

import processing.core.PApplet
import java.awt.Color
import java.util.function.Consumer
import kotlin.math.*

@Suppress("NAME_SHADOWING")
class PlanetSurface(private val size: Int, private val cube: Cube) {

    class Pixel(private val position: Position, private val surface: PlanetSurface) {

        companion object {
            const val MIN_ELEVATION = -5f
            const val MAX_ELEVATION = 5f
        }

        enum class Material(val lowColor: Color, val highColor: Color, val maxTemp: Int, val onHeat: Consumer<Pixel>) {
            Unset(Color.BLACK, Color.WHITE, 0, {}),
            Igneous(Color(0x1b1b1b), Color(0x3c3c50), 1000,
                { it.meltRock() }),
            Sedimentary(Color(0x96331b), Color(0xc45212), 1000,
                { it.meltRock() }),
            Metamorphic(Color(0x434357), Color(0x817B73), 1000,
                { it.meltRock() }),
            Ice(Color(0xc8f2ff), Color(0xFFFFFF), 0,
                { it.changeElevation(-1f); it.addLiquid(Liquid.FreshWater) }),
            Metal(Color(0x948A8A), Color(0xFFFFFF), 2000,
                { it.changeElevation(-1f); it.addLiquid(Liquid.MoltenMetal) }),
            Mud(Color(0x392B4B), Color(0x833607), 100,
                { it.material = Sedimentary }),
            Rust(Color(0x3D3D41), Color(0xCB461E), 1000,
                { it.meltRock() })
        }

        enum class Liquid(
            val lowColor: Color, val highColor: Color,
            val minTemp: Int, val maxTemp: Int,
            val onCool: Consumer<Pixel>, val onHeat: Consumer<Pixel>) {

            None(Color.BLACK, Color.WHITE, 0, 0, {}, {}),
            MoltenRock(Color(0xFF2F00), Color(0xFF8000), 1000, 10000,
                { it.liquid = None; it.material = Material.Igneous; it.changeElevation(1f) }, { it.liquid = None; it.addGas("rock") }),
            SaltWater(Color(0x4242CC), Color(0x266fff), 0, 100,
                { it.coating = Coating.Ice }, { it.liquid = None; it.addGas("water") }),
            FreshWater(Color(0x38657C), Color(0x3E8686), 0, 100,
                { it.coating = Coating.Ice }, { it.liquid = None; it.addGas("water") }),
            MoltenMetal(Color(0xFF8000), Color(0xFFE285), 2000, 20000,
                { it.liquid = None; it.material = Material.Metal; it.changeElevation(1f) }, { it.liquid = None; it.addGas("metal") })
        }

        enum class Coating(val lowColor: Color, val highColor: Color, val maxTemp: Int, val onHeat: Consumer<Pixel>) {
            None(Color.BLACK, Color.WHITE, 0, {}),
            Ice(Color(0xc1d5d6), Color(0xe5e3df), 0,
                { it.coating = None; it.addLiquid(Liquid.FreshWater) }),
            Obsidian(Color(0x160A23), Color(0x351F4F), 1000,
                { it.coating = None; it.addLiquid(Liquid.MoltenRock) }),
            Waste(Color(0x4B4111), Color(0x6B5D1C), 200,
                { it.coating = None; })
        }

        var temperature = 0
        var elevation = 0f
        var liquidDepth = 0f
        var material = Material.Unset
        var liquid = Liquid.None
        var coating = Coating.None

        fun meltRock() {
            changeElevation(-1f)
            addLiquid(Liquid.MoltenRock)
        }

        // todo: interact with surrounding pixels
        fun changeElevation(amount: Float) {
            elevation = (elevation + amount).coerceIn(MIN_ELEVATION, MAX_ELEVATION)
        }

        // todo: interact with fluids already there
        fun addLiquid(liquid: Liquid) {
            this.liquid = liquid
            liquidDepth = 1f
        }

        // todo: gas
        fun addGas(gasTodo: String) {

        }

        fun increaseTemp() {
            temperature++
        }

        // todo: more complicated
        fun getColor(): Int {
            return if (coating != Coating.None) coating.highColor.rgb
            else if (liquid != Liquid.None && liquidDepth > 0) mapColor(
                liquid.lowColor, liquid.highColor,
                liquidDepth * 25.5f, 255f).rgb
            else mapColor(
                material.lowColor, material.highColor,
                (elevation + 5) * 25.5f, 255f).rgb
        }

        private fun getNeighbors(): Array<Pixel> {
            return arrayOf(
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Up)),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Right)),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Down)),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Left))
            )
        }

        private fun getLiquidHeight(other: Pixel): Float {
            return if (other != this) elevation + liquidDepth else elevation
        }

        fun update() {
            updateTemperature()
            updateLiquidFlow()
        }

        private fun updateTemperature() {
            if (temperature > coating.maxTemp) coating.onHeat.accept(this)
            if (temperature > liquid.maxTemp) liquid.onHeat.accept(this)
            if (temperature < liquid.minTemp) liquid.onCool.accept(this)
            if (temperature > material.maxTemp) material.onHeat.accept(this)
        }

        private fun updateLiquidFlow() {
            if (liquidDepth <= 0) return
            val neighbors = getNeighbors() + this

            neighbors.sortWith {
                    p1: Pixel, p2: Pixel -> (p1.getLiquidHeight(this) - p2.getLiquidHeight(this)).sign.toInt()
            }
            val diffs = Array(4) {
                    i -> neighbors[i+1].getLiquidHeight(this) - neighbors[i].getLiquidHeight(this)
            } + Float.MAX_VALUE
            var thisDepth = liquidDepth
            for (i in 0 until 5) {
                neighbors[i].liquidDepth += min(thisDepth, diffs[i]) / (i + 1)
                thisDepth -= diffs[i]
                if (thisDepth <= 0) break
            }
        }
    }

    private val north = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.North), this) }
    private val south = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.South), this) }
    private val east = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.East), this) }
    private val west = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.West), this) }
    private val obverse = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.Obverse), this) }
    private val reverse = Array(size * size) { i -> Pixel(Pair(positionFromIndex(i), Cube.FaceType.Reverse), this) }

    private val pixels = arrayOf(*north, *south, *east, *west, *obverse, *reverse)

    init {
        for (p in pixels) {
            p.material = Pixel.Material.Metamorphic
            p.elevation = app.random(Pixel.MIN_ELEVATION, Pixel.MAX_ELEVATION)
            p.liquid = Pixel.Liquid.FreshWater
            p.liquidDepth = if (app.random(10f) < 1f) app.random(10f) else 0f
        }

//        for (i in 0 until 20) {
//            placeRoundFeature(cube.randomPosition(), app.random(-5f, 6f), app.random(0.5f, 2f))
//        }
    }

    companion object {
        fun mapColor(a: Color, b: Color, map: Float, alpha: Float): Color {
            var alpha = alpha
            var r = PApplet.map(map.coerceAtMost(255f), 0f, 255f, a.red.toFloat(), b.red.toFloat())
            var g = PApplet.map(map.coerceAtMost(255f), 0f, 255f, a.green.toFloat(), b.green.toFloat())
            var bl = PApplet.map(map.coerceAtMost(255f), 0f, 255f, a.blue.toFloat(), b.blue.toFloat())

            r = PApplet.map(r, 0f, 255f, 0f, 1f)
            g = PApplet.map(g, 0f, 255f, 0f, 1f)
            bl = PApplet.map(bl, 0f, 255f, 0f, 1f)
            alpha = PApplet.map(alpha, 0f, 255f, 0f, 1f)

            return Color(r, g, bl, alpha)
        }
    }

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

    fun update() {
        pixels.forEach { p -> p.update() }
    }

    private fun positionFromIndex(i: Int): IntVector {
        val x = i % size
        val y = (i - x) / size

        return IntVector(x, y)
    }

    private fun pixelAtPosition(position: Position): Pixel {
        return when (position.second) {
            Cube.FaceType.North -> north[position.first.x + position.first.y * size]
            Cube.FaceType.South -> south[position.first.x + position.first.y * size]
            Cube.FaceType.East -> east[position.first.x + position.first.y * size]
            Cube.FaceType.West -> west[position.first.x + position.first.y * size]
            Cube.FaceType.Obverse -> obverse[position.first.x + position.first.y * size]
            Cube.FaceType.Reverse -> reverse[position.first.x + position.first.y * size]
        }
    }

    private fun placeRoundFeature(position: Position, height: Float, falloff: Float) {
        val radius: Int = ceil((height.absoluteValue / falloff)).toInt()
        for (x in -radius .. radius) {
            for (y in -radius .. radius) {
                val distance = sqrt(x.toFloat().pow(2) + y.toFloat().pow(2))
                val change = (1 - distance.coerceAtMost(height.absoluteValue) / height.absoluteValue) * height
                pixelAtPosition(cube.changePositionCubical(position, IntVector(x, y))).changeElevation(change)
            }
        }
    }
}