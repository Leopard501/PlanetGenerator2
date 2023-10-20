package main.kotlin

import processing.core.PApplet
import processing.core.PVector
import java.awt.Color
import kotlin.math.*

/**
 * A simulated pixel on the surface of a planet
 *
 * @param position what face it is located on and where
 * @param surface the surface this pixel belongs to
 */
class Pixel(private val position: Position, private val surface: PlanetSurface) {

    companion object {
        const val MIN_ELEVATION = -5f
        const val MAX_ELEVATION = 5f
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

    private fun liquidIsSettled(): Boolean {
        var isSettled = true
        get4Neighbors().forEach {
            isSettled = isSettled && (getHeight() - it.getHeight()).absoluteValue < 0.1f
        }
        return isSettled
    }

    fun evaporate() {
        val change = min(liquidDepth, 0.02f)
        addGas(Gas.Water, change)
        liquidDepth -= change
        liquid = Liquid.SaltWater
    }

    fun rain() {
        val change = min(gasDensity, 0.1f)

        gasDensity -= change
        addLiquid(Liquid.FreshWater, change)
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

    // Issues with multiple gas types
    fun addGas(type: Gas, amount: Float) {
        gasDensity += amount
        gas = type
        return
    }

    /**
     * Color is prioritized: coating > liquid > surface.
     *
     * @return the color of this pixel in rgb
     */
    fun getColor(): Int {
        var r = if (coating != Coating.None) PlanetSurface.mapColor(
            coating.highColor, coating.lowColor,
            0f, coating.maxTemp.toFloat(), temperature
        ).rgb
        else if (liquid != Liquid.None && liquidDepth > 0) liquid.color.getColor(liquid, temperature, liquidDepth)
        else PlanetSurface.mapColor(
            material.lowColor, material.highColor,
            MIN_ELEVATION, MAX_ELEVATION, elevation
        ).rgb

        if (gas != Gas.None) {
            r = PlanetSurface.mapColor(Color(r), gas.color, 0f, 5f, gasDensity).rgb
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
        updateLiquid()
        updateGas()
    }

    /**
     * Calculates energy received by latitude
     * Two different systems for polar faces and equatorial faces,
     * works for now; might change based on how the cube is translated to a sphere.
     *
     * @return solar energy multiplier: 1 at equator, 0 at poles
     */
    private fun solarStrength(): Float {
        val angle = PApplet.map(surface.cube.getLatitude(position).absoluteValue, 0f, 1f, PApplet.HALF_PI, PApplet.PI)
        return PApplet.sin(angle)
    }

    /**
     * Multiplier for radiation amount based on elevation.
     */
    private fun elevationRadiationMultiplier(): Float {
        val h = elevation + liquidDepth + coatingThickness
//        return 1f + (h.pow(2) / 50f)
        return 1f
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
        if (temperature > material.maxTemp) material.onHeat.accept(this)
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

    private fun updateLiquid() {
        if (liquidDepth <= 0 || liquid == Liquid.None) {
            liquid = Liquid.None
            liquidDepth = 0f
            return
        }

        if (temperature > liquid.maxTemp && liquidDepth > 0) liquid.onHeat.accept(this)
        if (temperature < liquid.minTemp && liquidDepth > 0) liquid.onCool.accept(this)

        if (liquidIsSettled() &&
            temperature > liquid.minTemp + (liquid.maxTemp - liquid.minTemp) * 0.3f &&
            app.random(120f) < 1f)
            liquid.onEvaporate.accept(this)

        liquidFlow()
    }

    /**
     * Simulates movement of liquids.
     */
    private fun liquidFlow() {
        val neighbors = get4Neighbors() + this

        neighbors.sortWith {
                p1: Pixel, p2: Pixel -> (p1.getHeight(this) - p2.getHeight(this)).sign.toInt()
        }
        val diffs = Array(4) {
                i -> neighbors[i+1].getHeight(this) - neighbors[i].getHeight(this)
        } + Float.MAX_VALUE
        var thisDepth = liquidDepth
        for (i in 0 until 5) {
            val change = PApplet.min(thisDepth, diffs[i]) / (i + 1)
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
    fun addLiquid(type: Liquid, amount: Float) {
        if (type.ordinal == liquid.ordinal || liquid == Liquid.None) {
            liquidDepth += amount
            liquid = type
            return
        }

        liquidInteraction(liquid, liquidDepth, type, amount).accept(this)
    }

    private fun getHeight(): Float {
        return elevation + liquidDepth + coatingThickness
    }

    /**
     * If this is the checking pixel, only use elevation for calculation,
     * I'm not sure if that's better than just not doing that, but it works for now
     *
     * @param checkingPixel the pixel that this pixel is the neighbor of
     * @return the liquid height for purposes of water sharing
     */
    private fun getHeight(checkingPixel: Pixel): Float {
        return if (checkingPixel != this && liquid.ordinal == checkingPixel.liquid.ordinal) {
            elevation + liquidDepth + coatingThickness
        } else elevation + coatingThickness
    }

    private fun updateGas() {
        if (gasDensity <= 0 || gas == Gas.None) {
            gas = Gas.None
            gasDensity = 0f
            return
        }

        if (temperature < gas.minTemp) gas.onCool.accept(this)

        gasFlow()
    }

    /**
     * Simulates movement of gasses from wind.
     */
    private fun gasFlow() {
        val wind = getWind(surface.size / 16)
        if (wind.first.x == 0f && wind.first.y == 0f) return
        val strength = (wind.second / 10f).coerceAtMost(1f)
        val affectedNeighbors = arrayListOf(
            IntVector(PApplet.floor(wind.first.x), PApplet.floor(wind.first.y)),
            IntVector(PApplet.ceil(wind.first.x), PApplet.ceil(wind.first.y)),
            IntVector(PApplet.floor(wind.first.x), PApplet.ceil(wind.first.y)),
            IntVector(PApplet.ceil(wind.first.x), PApplet.floor(wind.first.y)),
        )
        // Remove this pixel
        for (i in 0 until 4) {
            if (affectedNeighbors[i].x == 0 && affectedNeighbors[i].y == 0) {
                affectedNeighbors.removeAt(i)
                break
            }
        }

        // Move gas
        var newDensity = gasDensity
        for (neighbor in affectedNeighbors) {
            val diff = (1 - neighbor.diff(wind.first).coerceIn(0f, 1f))
            val change = newDensity * strength * diff
            surface.pixelAtPosition(surface.cube.changePositionSpherical(position, neighbor))
                .addGas(gas, change)
            newDensity -= change
        }

        // Should not be necessary, precaution only
        gasDensity = newDensity.coerceAtLeast(0f)
    }

    /**
     * Gets the wind direction and strength by checking temperature and density in a square range,
     * deflected by the coriolis effect
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
        checked.shuffle()

        var dist: PVector

        // Temperature deflection
        var highTemp = temperature
        var temp = PVector(0f, 0f)
        checked.forEach {
            if (it.first.temperature > highTemp) {
                highTemp = it.first.temperature
                temp = it.second.toPVector().normalize()
            }
        }
        dist = temp.copy()
        var speed = (temperature - highTemp).absoluteValue

        // Density deflection
        var dens = PVector(0f, 0f)
        var lowDensity = gasDensity
        checked.forEach {
            lowDensity = min(lowDensity, it.first.gasDensity)
            dens += it.second.toPVector().setMag((gasDensity - it.first.gasDensity).absoluteValue)
        }
        dens.setMag((gasDensity - lowDensity).absoluteValue * 0.2f)
        dist.setMag(speed)
        dist += dens
        speed = dist.mag()

        // Coriolis Effect
        val cor = surface.cube.changePositionSpherical(position,
            if (surface.cube.getHemisphere(position) == Cube.FaceType.North && surface.angularVelocity > 0)
                Cube.Directions.Right
            else Cube.Directions.Left
        ).first.toPVector().setMag(
            surface.angularVelocity.absoluteValue * PApplet.sin(surface.cube.getLatitude(position))
        )
        dist.setMag(speed)
        dist += cor
        speed = dist.mag()
        dist.normalize()

        return Pair(dist, speed)
    }

    fun erupt() {
        temperature = 2000f
        addLiquid(Liquid.MoltenRock, 1f)
    }
}