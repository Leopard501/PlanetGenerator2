package main.kotlin

import processing.core.PVector
import java.awt.Color
import java.util.function.Consumer

class PlanetSurface(private val size: Int, private val cube: Cube) {

    class Pixel {

        companion object {
            const val MIN_ELEVATION = -5
            const val MAX_ELEVATION = 5
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
                { it.changeElevation(-1); it.addFluid(Fluid.FreshWater) }),
            Metal(Color(0x948A8A), Color(0xFFFFFF), 2000,
                { it.changeElevation(-1); it.addFluid(Fluid.MoltenMetal) }),
            Mud(Color(0x392B4B), Color(0x833607), 100,
                { it.material = Sedimentary }),
            Rust(Color(0x3D3D41), Color(0xCB461E), 1000,
                { it.meltRock() })
        }

        enum class Fluid(
            val lowColor: Color, val highColor: Color,
            val minTemp: Int, val maxTemp: Int,
            val onCool: Consumer<Pixel>, val onHeat: Consumer<Pixel>) {

            None(Color.BLACK, Color.WHITE, 0, 0, {}, {}),
            MoltenRock(Color(0xFF2F00), Color(0xFF8000), 1000, 10000,
                { it.fluid = None; it.material = Material.Igneous; it.changeElevation(1) }, { it.fluid = None; it.addGas("rock") }),
            SaltWater(Color(0x4242CC), Color(0x266fff), 0, 100,
                { it.coating = Coating.Ice }, { it.fluid = None; it.addGas("water") }),
            FreshWater(Color(0x38657C), Color(0x3E8686), 0, 100,
                { it.coating = Coating.Ice }, { it.fluid = None; it.addGas("water") }),
            MoltenMetal(Color(0xFF8000), Color(0xFFE285), 2000, 20000,
                { it.fluid = None; it.material = Material.Metal; it.changeElevation(1) }, { it.fluid = None; it.addGas("metal") })
        }

        enum class Coating(val lowColor: Color, val highColor: Color, val maxTemp: Int, val onHeat: Consumer<Pixel>) {
            None(Color.BLACK, Color.WHITE, 0, {}),
            Ice(Color(0xc1d5d6), Color(0xe5e3df), 0,
                { it.coating = None; it.addFluid(Fluid.FreshWater) }),
            Obsidian(Color(0x160A23), Color(0x351F4F), 1000,
                { it.coating = None; it.addFluid(Fluid.MoltenRock) }),
            Waste(Color(0x4B4111), Color(0x6B5D1C), 200,
                { it.coating = None; })
        }

        var temperature = 0
        var elevation = 0
        var material = Material.Unset
        var fluid = Fluid.None
        var coating = Coating.None

        fun meltRock() {
            changeElevation(-1)
            addFluid(Fluid.MoltenRock)
        }

        // todo: interact with surrounding pixels
        fun changeElevation(amount: Int) {
            elevation = (elevation + amount).coerceAtLeast(MIN_ELEVATION)
        }

        // todo: interact with fluids already there
        fun addFluid(fluid: Fluid) {
            this.fluid = fluid
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
            else if (fluid != Fluid.None) fluid.highColor.rgb
            else material.highColor.rgb
        }

        fun update() {
            if (temperature > coating.maxTemp) coating.onHeat.accept(this)
            if (temperature > fluid.maxTemp) fluid.onHeat.accept(this)
            if (temperature < fluid.minTemp) fluid.onCool.accept(this)
            if (temperature > material.maxTemp) material.onHeat.accept(this)
        }
    }

    private val north = Array(size * size) { _ -> Pixel() }
    private val south = Array(size * size) { _ -> Pixel() }
    private val east = Array(size * size) { _ -> Pixel() }
    private val west = Array(size * size) { _ -> Pixel() }
    private val obverse = Array(size * size) { _ -> Pixel() }
    private val reverse = Array(size * size) { _ -> Pixel() }

    private val pixels = arrayOf(*north, *south, *east, *west, *obverse, *reverse)

    init {
        for (p in pixels) {
            p.temperature = -100
            p.material = Pixel.Material.Metamorphic
            p.fluid = Pixel.Fluid.SaltWater
            p.coating = Pixel.Coating.Ice
        }
    }

    fun update() {
        pixels.forEach { p -> p.update(); p.increaseTemp() }
    }

    fun display(position: PVector) {
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

        cube.display(position)
    }
}