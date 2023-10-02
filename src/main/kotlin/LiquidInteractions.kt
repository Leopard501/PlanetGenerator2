package main.kotlin

import java.util.function.Consumer

fun liquidInteraction(typeA: PlanetSurface.Pixel.Liquid, depthA: Float,
                      typeB: PlanetSurface.Pixel.Liquid, depthB: Float): Consumer<PlanetSurface.Pixel> {
    return when (typeA) {
        PlanetSurface.Pixel.Liquid.SaltWater ->
            when (typeB) {
                PlanetSurface.Pixel.Liquid.FreshWater ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                PlanetSurface.Pixel.Liquid.MoltenRock ->
                    vaporize(typeB, depthB)
                PlanetSurface.Pixel.Liquid.MoltenMetal ->
                    vaporize(typeB, depthB)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        PlanetSurface.Pixel.Liquid.FreshWater ->
            when (typeB) {
                PlanetSurface.Pixel.Liquid.SaltWater ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                PlanetSurface.Pixel.Liquid.MoltenRock ->
                    vaporize(typeB, depthB)
                PlanetSurface.Pixel.Liquid.MoltenMetal ->
                    vaporize(typeB, depthB)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        PlanetSurface.Pixel.Liquid.MoltenRock ->
            when (typeB) {
                PlanetSurface.Pixel.Liquid.MoltenMetal ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                PlanetSurface.Pixel.Liquid.SaltWater ->
                    vaporize(typeA, depthA)
                PlanetSurface.Pixel.Liquid.FreshWater ->
                    vaporize(typeA, depthA)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        PlanetSurface.Pixel.Liquid.MoltenMetal ->
            when (typeB) {
                PlanetSurface.Pixel.Liquid.MoltenRock ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                PlanetSurface.Pixel.Liquid.SaltWater ->
                    vaporize(typeA, depthA)
                PlanetSurface.Pixel.Liquid.FreshWater ->
                    vaporize(typeA, depthA)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
    }
}

fun simpleReplace(typeA: PlanetSurface.Pixel.Liquid, depthA: Float,
                  typeB: PlanetSurface.Pixel.Liquid, depthB: Float): Consumer<PlanetSurface.Pixel> {
    return Consumer { p ->
        p.liquid =
            if (depthA > depthB) typeA
            else if (depthB > depthA) typeB
            else if (app.random(2f) < 1f) typeA
            else typeB
        p.liquidDepth = depthA + depthB
    }
}

fun vaporize(replacement: PlanetSurface.Pixel.Liquid, replacementDepth: Float): Consumer<PlanetSurface.Pixel> {
    return Consumer { p ->
        p.addGas(PlanetSurface.Pixel.Gas.Water, p.liquidDepth)
        p.liquid = replacement
        p.liquidDepth = replacementDepth
    }
}