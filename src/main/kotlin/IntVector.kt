package main.kotlin

import processing.core.PVector
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.math.pow

class IntVector {
    var x: Int
    var y: Int

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    constructor(arr: IntArray) : this(arr[0], arr[1]) {
        if (arr.size > 2) {
            throw RuntimeException("IntVector input array must have a length of two")
        }
    }

    constructor(p: PVector) {
        x = p.x.toInt()
        y = p.y.toInt()
    }

    override fun toString(): String {
        return "{$x, $y}"
    }

    fun toPVector(): PVector {
        return PVector(x.toFloat(), y.toFloat())
    }

    fun sub(amount: Int): IntVector {
        x -= amount
        y -= amount
        return this
    }

    companion object {
        fun sub(iv: IntVector, amount: Int): IntVector {
            return IntVector(iv.x - amount, iv.y - amount)
        }
    }

    fun copy(): IntVector {
        return IntVector(x, y)
    }

    fun diff(other: PVector): Float {
        return (x - other.x).absoluteValue + (y - other.y).absoluteValue
    }
}