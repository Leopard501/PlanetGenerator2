package main.kotlin

import java.util.function.Consumer

fun liquidInteraction(typeA: Liquid, depthA: Float, typeB: Liquid, depthB: Float): Consumer<Pixel> {
    if (typeA == Liquid.None || typeB == Liquid.None) return Consumer {}
    return when (typeA) {
        Liquid.SaltWater ->
            when (typeB) {
                Liquid.FreshWater ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                Liquid.MoltenRock ->
                    vaporize(typeB, depthB)
                Liquid.MoltenMetal ->
                    vaporize(typeB, depthB)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        Liquid.FreshWater ->
            when (typeB) {
                Liquid.SaltWater ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                Liquid.MoltenRock ->
                    vaporize(typeB, depthB)
                Liquid.MoltenMetal ->
                    vaporize(typeB, depthB)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        Liquid.MoltenRock ->
            when (typeB) {
                Liquid.MoltenMetal ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                Liquid.SaltWater ->
                    vaporize(typeA, depthA)
                Liquid.FreshWater ->
                    vaporize(typeA, depthA)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        Liquid.MoltenMetal ->
            when (typeB) {
                Liquid.MoltenRock ->
                    simpleReplace(typeA, depthA, typeB, depthB)
                Liquid.SaltWater ->
                    vaporize(typeA, depthA)
                Liquid.FreshWater ->
                    vaporize(typeA, depthA)
                else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
            }
        else -> throw RuntimeException("The interaction between $typeA and $typeB is not defined")
    }
}

fun simpleReplace(typeA: Liquid, depthA: Float,
                  typeB: Liquid, depthB: Float): Consumer<Pixel> {
    return Consumer { p ->
        p.liquid =
            if (depthA > depthB) typeA
            else if (depthB > depthA) typeB
            else if (app.random(2f) < 1f) typeA
            else typeB
        p.liquidDepth = depthA + depthB
    }
}

fun vaporize(replacement: Liquid, replacementDepth: Float): Consumer<Pixel> {
    return Consumer { p ->
        p.addGas(Gas.Water, p.liquidDepth)
        p.liquid = replacement
        p.liquidDepth = replacementDepth
    }
}