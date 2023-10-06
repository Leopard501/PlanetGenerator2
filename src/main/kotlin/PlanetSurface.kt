package main.kotlin

import processing.core.PApplet.*
import processing.core.PVector
import java.awt.Color
import java.util.function.Consumer
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

/**
 * The simulated surface of a planet
 *
 * @param size size of each face in pixels
 * @param cube cube this surface will be stretched over
 */
class PlanetSurface(private val size: Int, private val cube: Cube) {

    /**
     * A simulated pixel on the surface of a planet
     *
     * @param position what face it is located on and where
     * @param surface the surface this pixel belongs to
     */
    class Pixel(private val position: Position, private val surface: PlanetSurface) {

        /**
         * Class that contains a function describing how to color a liquid based on depth and temperature
         */
        abstract class LiquidColor() {
            abstract fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int
        }

        class LiquidColorByTemperature(private val coldColor: Color, private val hotColor: Color) : LiquidColor() {
            override fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int {
                return mapColor(coldColor, hotColor, liquid.minTemp.toFloat(), liquid.maxTemp.toFloat(), temperature.toFloat()).rgb
            }
        }

        class LiquidColorByDepth(private val deepColor: Color, private val shallowColor: Color) : LiquidColor() {
            override fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int {
                return mapColor(shallowColor, deepColor, 0f, MAX_ELEVATION * 2, depth).rgb
            }
        }

        class LiquidColorByBoth(
            private val hotDeepColor: Color, private val hotShallowColor: Color,
            private val coldDeepColor: Color, private val coldShallowColor: Color): LiquidColor() {
            override fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int {
                val hotColor = mapColor(hotShallowColor, hotDeepColor, 0f, MAX_ELEVATION * 2, depth)
                val coldColor = mapColor(coldShallowColor, coldDeepColor, 0f, MAX_ELEVATION * 2, depth)
                return mapColor(coldColor, hotColor, liquid.minTemp.toFloat(), liquid.maxTemp.toFloat(), temperature.toFloat()).rgb
            }
        }

        companion object {
            const val MIN_ELEVATION = -5f
            const val MAX_ELEVATION = 5f
        }

        /**
         * The liquid layer of the pixel
         *
         * @param lowColor color at low elevation
         * @param highColor color at high elevation
         * @param maxTemp maximum stable temperature
         * @param onHeat function describing what happens above max temperature, UNFINISHED
         */
        enum class Material(val lowColor: Color, val highColor: Color, val maxTemp: Int, val onHeat: Consumer<Pixel>) {
            Unset(Color.BLACK, Color.WHITE, 0, {}),
            Igneous(Color(0x1b1b1b), Color(0x3c3c50), 2000,
                { it.meltRock() }),
            Sedimentary(Color(0x96331b), Color(0xc45212), 3000,
                { it.meltRock() }),
            Metamorphic(Color(0x434357), Color(0x817B73), 5000,
                { it.meltRock() }),
            Ice(Color(0xc8f2ff), Color(0xFFFFFF), 0,
                { it.changeElevation(-1f); it.addLiquid(Liquid.FreshWater, 1f) }),
            Metal(Color(0x948A8A), Color(0xFFFFFF), 7000,
                { it.changeElevation(-1f); it.addLiquid(Liquid.MoltenMetal, 1f) }),
            Mud(Color(0x392B4B), Color(0x833607), 100,
                { it.material = Sedimentary }),
            Rust(Color(0x3D3D41), Color(0xCB461E), 1000,
                { it.meltRock() })
        }

        /**
         * The liquid layer of the pixel
         *
         * @param color class to get the color based on temperature and depth
         * @param minTemp minimum stable temperature
         * @param maxTemp maximum stable temperature
         * @param onCool function describing what happens below min temperature, UNFINISHED
         * @param onHeat function describing what happens above max temperature, UNFINISHED
         */
        enum class Liquid(
            val color: LiquidColor,
            val minTemp: Int, val maxTemp: Int,
            val onCool: Consumer<Pixel>, val onHeat: Consumer<Pixel>) {

            None(LiquidColorByDepth(Color.WHITE, Color.BLACK), 0, 1, {}, {}),
            MoltenRock(LiquidColorByTemperature(Color(0xFF2F00), Color(0xFF8000)), 1000, 10000,
                { it.liquid = None; it.material = Material.Igneous; it.changeElevation(1f) }, { it.liquid = None;  }),
            SaltWater(LiquidColorByBoth(
                Color(20, 45, 105), Color(60, 140, 180), Color(5, 20, 40), Color(40, 60, 90)
            ), 300, 400,
                { it.replaceLiquidWithCoating(Coating.Ice) }, { it.liquidDepth -= 0.1f; it.addGas(Gas.Water, 0.1f) }),
            FreshWater(LiquidColorByBoth(
                Color(0x274E62), Color(0x3E8686), Color(0x1F335E), Color(0x477288)
            ), 300, 400,
                { it.replaceLiquidWithCoating(Coating.Ice) }, { it.liquidDepth -= 0.1f; it.addGas(Gas.Water, 0.1f) }),
            MoltenMetal(LiquidColorByTemperature(Color(0xFF8000), Color(0xFFE285)), 2000, 20000,
                { it.liquid = None; it.material = Material.Metal; it.changeElevation(1f) }, { it.liquid = None; })
        }

        enum class Coating(val lowColor: Color, val highColor: Color, val maxTemp: Int, val onHeat: Consumer<Pixel>) {
            None(Color.BLACK, Color.WHITE, 0, {}),
            Ice(Color(0xD6EFFF), Color(0xe5e3df), 300,
                { it.replaceCoatingWithLiquid(Liquid.SaltWater) }),
            Obsidian(Color(0x160A23), Color(0x351F4F), 1000,
                { it.coating = None; it.addLiquid(Liquid.MoltenRock, 1f) }),
            Waste(Color(0x4B4111), Color(0x6B5D1C), 200,
                { it.coating = None; })
        }

        enum class Gas(val color: Color, val minTemp: Int, val onCool: Consumer<Pixel>) {
            None(Color.BLACK, 0, {}),
            Water(Color.WHITE, 400,
                { it.rain() })
        }

        var temperature = 0f
        var elevation = 0f
        var liquidDepth = 0f
        var coatingThickness = 0f
        var gasDensity = 0f
        var material = Material.Unset
        var liquid = Liquid.None
        var coating = Coating.None
        var gas = Gas.None

        private var angularVelocity = 20f

        fun rain() {
            gasDensity -= 0.1f
            addLiquid(Liquid.FreshWater, 0.1f)
        }

        fun replaceLiquidWithCoating(coating: Coating) {
            this.coating = coating
            coatingThickness += liquidDepth
            liquidDepth = 0f
            liquid = Liquid.None
        }

        fun replaceCoatingWithLiquid(liquid: Liquid) {
            this.liquid = liquid
            liquidDepth = coatingThickness
            coatingThickness = 0f
            coating = Coating.None
        }

        fun meltRock() {
            changeElevation(-1f)
            addLiquid(Liquid.MoltenRock, 1f)
        }

        fun changeElevation(amount: Float) {
            elevation = (elevation + amount).coerceIn(MIN_ELEVATION, MAX_ELEVATION)
        }

        fun addGas(type: Gas, amount: Float) {
            if (type.ordinal == gas.ordinal || gas == Gas.None) {
                gasDensity += amount
                gas = type
                return
            }

            TODO("gas interactions")
//            liquidInteraction(liquid, liquidDepth, type, amount).accept(this)
        }

        /**
         * Color is prioritized: coating > liquid > surface.
         *
         * @return the color of this pixel in rgb
         */
        fun getColor(): Int {
            var r = if (coating != Coating.None) mapColor(
                coating.highColor, coating.lowColor,
                0f, coating.maxTemp.toFloat(), temperature
            ).rgb
            else if (liquid != Liquid.None && liquidDepth > 0) liquid.color.getColor(liquid, temperature, liquidDepth)
            else mapColor(
                material.lowColor, material.highColor,
                MIN_ELEVATION, MAX_ELEVATION, elevation).rgb

            if (gas != Gas.None) {
                r = mapColor(Color(r), gas.color, 0f, 10f, gasDensity).rgb
            }

//            var r = mapColor(Color.BLUE, Color.RED, 300f, 400f, temperature).rgb

            return r
        }

        /**
         * @return an array of 4 touching pixels, ordered clockwise from up
         */
        private fun get4Neighbors(): Array<Pixel> {
            return arrayOf(
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Up)),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Right)),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Down)),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, Cube.Directions.Left))
            )
        }

        /**
         * @return an array of 8 touching pixels and corners, ordered clockwise from up
         */
        private fun get8Neighbors(): Array<Pixel> {
            return arrayOf(
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(0, -1))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(1, -1))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(1, 0))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(1, 1))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(0, 1))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(-1, 1))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(-1, 0))),
                surface.pixelAtPosition(surface.cube.changePositionCubical(position, IntVector(-1, -1))),
            )
        }

        fun update() {
            updateTemperature()
            liquidFlow()
            gasFlow()
        }

        /**
         * Calculates energy received by latitude
         * Two different systems for polar faces and equatorial faces,
         * works for now; might change based on how the cube is translated to a sphere.
         *
         * @return solar energy multiplier: 1 at equator, 0 at poles
         */
        private fun solarStrength(): Float {
            val angle = map(surface.cube.getLatitude(position).absoluteValue, 0f, 1f, HALF_PI, PI)
            return sin(angle)
        }

        private fun elevationRadiationMultiplier(): Float {
            val h = elevation + liquidDepth + coatingThickness
            return 1f + (h.pow(2) / 50f)
        }

        /**
         * Simulates temperature and changes state.
         */
        private fun updateTemperature() {
            val tempScale = 16f / surface.size
            val solar = (solarStrength() * surface.solarEnergy) * tempScale
            val landRadiationMultiplier = if (liquidDepth > 0) 0.90f else 1.1f
            val radiation = (temperature * surface.heatRadiation * elevationRadiationMultiplier() * landRadiationMultiplier) * tempScale
            temperature = temperature + solar - radiation
            heatFlow()

            if (temperature > coating.maxTemp) coating.onHeat.accept(this)
            if (temperature > liquid.maxTemp && liquidDepth > 0) liquid.onHeat.accept(this)
            if (temperature < liquid.minTemp && liquidDepth > 0) liquid.onCool.accept(this)
            if (temperature > material.maxTemp) material.onHeat.accept(this)
            if (temperature < gas.minTemp) gas.onCool.accept(this)
        }

        /**
         * Simulates movement of heat
         */
        private fun heatFlow() {
            @Suppress("KotlinConstantConditions")
            if (surface.heatConductivity <= 0f) return
            val oldTemp = temperature
            for (n in get4Neighbors()) {
                temperature += n.temperature / (4 + 1 / surface.heatConductivity)
            }
            temperature -= (oldTemp / (4 + 1 / surface.heatConductivity)) * 4
        }

        /**
         * Simulates movement of liquids.
         */
        private fun liquidFlow() {
            if (liquidDepth <= 0 || liquid == Liquid.None) {
                liquid = Liquid.None
                liquidDepth = 0f
                return
            }
            val neighbors = get4Neighbors() + this

            neighbors.sortWith {
                    p1: Pixel, p2: Pixel -> (p1.getLiquidHeight(this) - p2.getLiquidHeight(this)).sign.toInt()
            }
            val diffs = Array(4) {
                    i -> neighbors[i+1].getLiquidHeight(this) - neighbors[i].getLiquidHeight(this)
            } + Float.MAX_VALUE
            var thisDepth = liquidDepth
            for (i in 0 until 5) {
                val change = min(thisDepth, diffs[i]) / (i + 1)
                neighbors[i].addLiquid(liquid, change)
                liquidDepth -= change
                // diffs[i] is smoother than change
                thisDepth -= diffs[i]
                if (thisDepth <= 0) break
            }
        }

        /**
         * Adds some liquid to the surface, with interaction
         *
         * @param type type of liquid to add
         * @param amount depth of liquid added
         */
        private fun addLiquid(type: Liquid, amount: Float) {
            if (type.ordinal == liquid.ordinal || liquid == Liquid.None) {
                liquidDepth += amount
                liquid = type
                return
            }

            liquidInteraction(liquid, liquidDepth, type, amount).accept(this)
        }

        /**
         * If this is the checking pixel, only use elevation for calculation,
         * I'm not sure if that's better than just not doing that, but it works for now
         *
         * @param checkingPixel the pixel that this pixel is the neighbor of
         * @return the liquid height for purposes of water sharing
         */
        private fun getLiquidHeight(checkingPixel: Pixel): Float {
            return if (checkingPixel != this && liquid.ordinal == checkingPixel.liquid.ordinal) {
                elevation + liquidDepth + coatingThickness
            } else elevation + coatingThickness
        }

        /**
         * Simulates movement of gasses from wind.
         *
         * todo: density
         */
        private fun gasFlow() {
            if (gasDensity <= 0 || gas == Gas.None) {
                gas = Gas.None
                gasDensity = 0f
                return
            }

            val wind = getWind(surface.size / 16)
            if (wind.first.x == 0f && wind.first.y == 0f) return
            val strength = (wind.second / 10f).coerceAtMost(1f)

            val affectedNeighbors = arrayListOf(
                IntVector(floor(wind.first.x), floor(wind.first.y)),
                IntVector(ceil(wind.first.x), ceil(wind.first.y)),
                IntVector(floor(wind.first.x), ceil(wind.first.y)),
                IntVector(ceil(wind.first.x), floor(wind.first.y)),
            )
            for (i in 0 until 4) {
                if (affectedNeighbors[i].x == 0 && affectedNeighbors[i].y == 0) {
                    affectedNeighbors.removeAt(i)
                    break
                }
            }

            var newDensity = gasDensity
            for (neighbor in affectedNeighbors) {
                val diff = (1 - neighbor.diff(wind.first).coerceIn(0f, 1f))
                val change = newDensity * strength * diff
                surface.pixelAtPosition(surface.cube.changePositionSpherical(position, neighbor))
                    .addGas(gas, change)
                newDensity -= change
            }

            gasDensity = newDensity.coerceAtLeast(0f)
        }

        /**
         * Gets the wind direction and strength by checking temperature in a square range
         *
         * @param range radius of a square of pixels to check temperature
         * @return a PVector pointing toward the hottest pixel in range, and the difference in temperature
         */
        private fun getWind(range: Int): Pair<PVector, Float> {
            val checked = ArrayList<Pair<Pixel, IntVector>>()
            for (x in -range .. range) {
                for (y in -range .. range) {
                    val p = IntVector(x, y)
                    checked.add(Pair(surface.pixelAtPosition(surface.cube.changePositionSpherical(position, p)), p))
                }
            }

            // Temperature deflection
            var highTemp = temperature
            var dist = PVector(0f, 0f)
            checked.forEach {
                if (it.first.temperature > highTemp) {
                    highTemp = it.first.temperature
                    dist = it.second.toPVector().normalize()
                }
            }
            var speed = (temperature - highTemp).absoluteValue

            // Coriolis Effect
            val cor = surface.cube.changePositionSpherical(position,
                if (surface.cube.getHemisphere(position) == Cube.FaceType.North && angularVelocity > 0)
                    Cube.Directions.Right
                else Cube.Directions.Left
            ).first.toPVector().setMag(
                angularVelocity.absoluteValue * sin(surface.cube.getLatitude(position))
            )
            dist.setMag(speed)
            dist += cor
            speed += dist.mag()
            dist.normalize()

            return Pair(dist, speed)
        }

        fun erupt() {
            temperature = 2000f
            addLiquid(Liquid.MoltenRock, 1f)
        }
    }

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

    private val solarEnergy = 5f
    private val heatRadiation = 0.0115f
    private val heatConductivity = 1

    init {
        for (p in pixels) {
            p.temperature = 350f
            p.material = Pixel.Material.Metamorphic
            p.elevation = app.random(-0.2f, 0.2f)
            p.liquid = Pixel.Liquid.SaltWater
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
//        if (app.random(40f) < 1 && volcanoes.isNotEmpty()) volcanoes.removeFirst()
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