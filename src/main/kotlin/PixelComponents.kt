package main.kotlin

import java.awt.Color
import java.util.function.Consumer

/**
 * Class that contains a function describing how to color a liquid based on depth and temperature
 */
abstract class LiquidColor() {
    abstract fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int
}

class LiquidColorByTemperature(private val coldColor: Color, private val hotColor: Color) : LiquidColor() {
    override fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int {
        return PlanetSurface.mapColor(
            coldColor,
            hotColor,
            liquid.minTemp.toFloat(),
            liquid.maxTemp.toFloat(),
            temperature
        ).rgb
    }
}

class LiquidColorByDepth(private val deepColor: Color, private val shallowColor: Color) : LiquidColor() {
    override fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int {
        return PlanetSurface.mapColor(shallowColor, deepColor, 0f, Pixel.MAX_ELEVATION * 2, depth).rgb
    }
}

class LiquidColorByBoth(
    private val hotDeepColor: Color, private val hotShallowColor: Color,
    private val coldDeepColor: Color, private val coldShallowColor: Color
): LiquidColor() {
    override fun getColor(liquid: Liquid, temperature: Float, depth: Float): Int {
        val hotColor = PlanetSurface.mapColor(hotShallowColor, hotDeepColor, 0f, Pixel.MAX_ELEVATION * 2, depth)
        val coldColor = PlanetSurface.mapColor(coldShallowColor, coldDeepColor, 0f, Pixel.MAX_ELEVATION * 2, depth)
        return PlanetSurface.mapColor(
            coldColor,
            hotColor,
            liquid.minTemp.toFloat(),
            liquid.maxTemp.toFloat(),
            temperature
        ).rgb
    }
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
    Igneous(
        Color(0x1b1b1b), Color(0x3c3c50), 2000,
        { it.meltRock() }),
    Sedimentary(
        Color(0x96331b), Color(0xc45212), 3000,
        { it.meltRock() }),
    Metamorphic(
        Color(0x434357), Color(0x817B73), 5000,
        { it.meltRock() }),
    Ice(
        Color(0xc8f2ff), Color(0xFFFFFF), 0,
        { it.changeElevation(-1f); it.addLiquid(Liquid.FreshWater, 1f) }),
    Metal(
        Color(0x948A8A), Color(0xFFFFFF), 7000,
        { it.changeElevation(-1f); it.addLiquid(Liquid.MoltenMetal, 1f) }),
    Mud(
        Color(0x392B4B), Color(0x833607), 100,
        { it.material = Sedimentary }),
    Rust(
        Color(0x3D3D41), Color(0xCB461E), 1000,
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
    val onCool: Consumer<Pixel>, val onHeat: Consumer<Pixel>
) {

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
    Ice(
        Color(0xD6EFFF), Color(0xe5e3df), 300,
        { it.replaceCoatingWithLiquid(Liquid.SaltWater) }),
    Obsidian(
        Color(0x160A23), Color(0x351F4F), 1000,
        { it.coating = None; it.addLiquid(Liquid.MoltenRock, 1f) }),
    Waste(
        Color(0x4B4111), Color(0x6B5D1C), 200,
        { it.coating = None; })
}

enum class Gas(val color: Color, val minTemp: Int, val onCool: Consumer<Pixel>) {
    None(Color.BLACK, 0, {}),
    Water(
        Color.WHITE, 400,
        { it.rain() })
}