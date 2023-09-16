package main.kotlin

import processing.core.PConstants
import processing.core.PImage
import processing.core.PVector
import java.awt.Color

class Planet {

    enum class Directions {
        Right,
        Left,
        Up,
        Down
    }

    enum class Face {
        North,
        South,
        East,
        West,
        Obverse,
        Reverse;

        var surface: PImage = app.createImage(16, 16, PConstants.RGB)

        init {
            for (x in 0 until surface.width) {
                for (y in 0 until surface.height) {
                    val i: Int = x + y * surface.width

                    surface.pixels[i] = 0xffffff
                }
            }
        }
    }

    private val size = 16
    private val scale = 10f

    private var north = PImage()
    private var south = PImage()
    private var east = PImage()
    private var west = PImage()
    private var obverse = PImage()
    private var reverse = PImage()

    private var walker = Pair(Pair(IntVector(8, 8), Face.Obverse), Directions.Right)

    init {
        north = createSurface()
        south = createSurface()
        east = createSurface()
        west = createSurface()
        obverse = createSurface()
        reverse = createSurface()
    }

    private fun createSurface(): PImage {
        val surface = app.createImage(size, size, PConstants.RGB)
        surface.loadPixels()

        for (x in 0 until surface.width) {
            for (y in 0 until surface.height) {
                val i: Int = x + y * surface.width

                surface.pixels[i] = 0x888888
            }
        }

        return surface
    }

    fun display(position: PVector) {
        app.image(Face.Obverse.surface, position.x, position.y, size * scale, size * scale)
        app.image(Face.North.surface, position.x, position.y - size * scale, size * scale, size * scale)
        app.image(Face.South.surface, position.x, position.y + size * scale, size * scale, size * scale)
        app.image(Face.East.surface, position.x + size * scale, position.y, size * scale, size * scale)
        app.image(Face.West.surface, position.x - size * scale, position.y, size * scale, size * scale)
        app.image(Face.Reverse.surface, position.x + size * scale * 2, position.y, size * scale, size * scale)
    }

    fun walk() {
//        if (app.frameCount % 5 != 0) return

        if (app.frameCount % 5 == 0) {
            walker = Pair(walker.first, Directions.values()[app.random(4f).toInt()])
        }

        walker = Pair(walker.first + walker.second, walker.second)
        walker.first.second.surface.pixels[walker.first.first.x + walker.first.first.y * size] =
            when (walker.second) {
                Directions.Up -> Color.MAGENTA.rgb
                Directions.Down -> Color.RED.rgb
                Directions.Right -> Color.BLUE.rgb
                Directions.Left -> Color.GREEN.rgb
            }
        walker.first.second.surface.updatePixels()
    }

    operator fun Position.plus(direction: Directions): Position {
        if (this.second == Face.North || this.second == Face.South) {
            val quadrant = getQuadrant(this.first, direction)
            return when (direction) {
                Directions.Right -> Pair(
                    when (quadrant) {
                        Directions.Down -> this.first + Directions.Right
                        Directions.Right -> this.first + Directions.Up
                        Directions.Up -> this.first + Directions.Left
                        Directions.Left -> this.first + Directions.Down
                    },
                    this.second
                )
                Directions.Left -> Pair(
                    when (quadrant) {
                        Directions.Down -> this.first + Directions.Left
                        Directions.Right -> this.first + Directions.Down
                        Directions.Up -> this.first + Directions.Right
                        Directions.Left -> this.first + Directions.Up
                    },
                    this.second
                )
                Directions.Up ->
                    if (this.second == Face.North) {
                        Pair(when (quadrant) {
                            Directions.Down -> this.first + Directions.Up
                            Directions.Right -> this.first + Directions.Left
                            Directions.Up -> this.first + Directions.Down
                            Directions.Left -> this.first + Directions.Right
                        },
                        this.second)
                    } else {
                        when (quadrant) {
                            Directions.Down ->
                                if (this.first.y == size - 1) {
                                    Pair(IntVector(size - 1 - this.first.x, size - 1), Face.Reverse)
                                } else {
                                    Pair(this.first + Directions.Down, this.second)
                                }
                            Directions.Right ->
                                if (this.first.x == size - 1) {
                                    Pair(IntVector(this.first.y, size - 1), Face.East)
                                } else {
                                    Pair(this.first + Directions.Right, this.second)
                                }
                            Directions.Up ->
                                if (this.first.y == 0) {
                                    Pair(IntVector(this.first.x, size - 1), Face.Obverse)
                                } else {
                                    Pair(this.first + Directions.Up, this.second)
                                }
                            Directions.Left ->
                                if (this.first.x == 0) {
                                    Pair(IntVector(size - 1 - this.first.y, size - 1), Face.West)
                                } else {
                                    Pair(this.first + Directions.Left, this.second)
                                }
                        }
                    }
                Directions.Down ->
                    if (this.second == Face.North) {
                        when (quadrant) {
                            Directions.Down ->
                                if (this.first.y == size - 1) {
                                    Pair(IntVector(this.first.x, 0), Face.Obverse)
                                } else {
                                    Pair(this.first + Directions.Down, this.second)
                                }
                            Directions.Right ->
                                if (this.first.x == size - 1) {
                                    Pair(IntVector(size - 1 - this.first.y, 0), Face.East)
                                } else {
                                    Pair(this.first + Directions.Right, this.second)
                                }
                            Directions.Up ->
                                if (this.first.y == 0) {
                                    Pair(IntVector(size - 1 - this.first.x, 0), Face.Reverse)
                                } else {
                                    Pair(this.first + Directions.Up, this.second)
                                }
                            Directions.Left ->
                                if (this.first.x == 0) {
                                    Pair(IntVector(this.first.y, 0), Face.West)
                                } else {
                                    Pair(this.first + Directions.Left, this.second)
                                }
                        }
                    } else {
                        Pair(when (quadrant) {
                            Directions.Down -> this.first + Directions.Up
                            Directions.Right -> this.first + Directions.Left
                            Directions.Up -> this.first + Directions.Down
                            Directions.Left -> this.first + Directions.Right
                        },
                        this.second)
                    }
            }
        } else {
            // Right across edge
            if (this.first.x == size - 1 && direction == Directions.Right) {
                return Pair(IntVector(0, this.first.y), this.second + direction)
            } // Left across edge
            else if (this.first.x == 0 && direction == Directions.Left) {
                return Pair(IntVector(size - 1, this.first.y), this.second + direction)
            } // Up across edge
            else if (this.first.y == 0 && direction == Directions.Up) {
                return Pair(
                    when (this.second) {
                        Face.Obverse -> IntVector(this.first.x, size - 1)
                        Face.East -> IntVector(size - 1, size - 1 - this.first.x)
                        Face.Reverse -> IntVector(size - 1 - this.first.x, 0)
                        Face.West -> IntVector(0, this.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    this.second + direction
                )
            } // Down across edge
            else if (this.first.y == size - 1 && direction == Directions.Down) {
                return Pair(
                    when (this.second) {
                        Face.Obverse -> IntVector(this.first.x, 0)
                        Face.East -> IntVector(size - 1, this.first.x)
                        Face.Reverse -> IntVector(size - 1 - this.first.x, size - 1)
                        Face.West -> IntVector(0, size - 1 - this.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    this.second + direction
                )
            } // Within board
            else return Pair(this.first + direction, this.second)
        }
    }

    private fun getQuadrant(position: IntVector, direction: Directions): Directions {
        return if (position.x > position.y) {
            if (position.x > size - 1 - position.y) {
                Directions.Right
            } else if (position.x < size - 1 - position.y) {
                Directions.Up
            } else {
                if (direction == Directions.Left) Directions.Right
                else Directions.Up
            }
        } else if (position.x < position.y) {
            if (position.x > size - 1 - position.y) {
                Directions.Down
            } else if (position.x < size - 1 - position.y) {
                Directions.Left
            } else {
                if (direction == Directions.Left) Directions.Left
                else Directions.Down
            }
        } else {
            if (position.x > size - 1 - position.y) {
                if (direction == Directions.Left) Directions.Down
                else Directions.Right
            } else if (position.x < size - 1 - position.y) {
                if (direction == Directions.Left) Directions.Up
                else Directions.Left
            } else Directions.Right // very center, only possible if size is odd
        }
    }

    operator fun IntVector.plus(direction: Directions): IntVector {
        return when (direction) {
            Directions.Up -> IntVector(this.x, this.y - 1)
            Directions.Down -> IntVector(this.x, this.y + 1)
            Directions.Left -> IntVector(this.x - 1, this.y)
            Directions.Right -> IntVector(this.x + 1, this.y)
        }
    }

    operator fun Face.plus(direction: Directions): Face {
        return when (this) {
            Face.North -> {
                when (direction) {
                    Directions.Right -> Face.East
                    Directions.Left -> Face.West
                    Directions.Up -> Face.Reverse
                    Directions.Down -> Face.Obverse
                }
            }
            Face.South -> {
                when (direction) {
                    Directions.Right -> Face.East
                    Directions.Left -> Face.West
                    Directions.Up -> Face.Obverse
                    Directions.Down -> Face.Reverse
                }
            }
            Face.Obverse -> {
                when (direction) {
                    Directions.Right -> Face.East
                    Directions.Left -> Face.West
                    Directions.Up -> Face.North
                    Directions.Down -> Face.South
                }
            }
            Face.West -> {
                when (direction) {
                    Directions.Right -> Face.Obverse
                    Directions.Left -> Face.Reverse
                    Directions.Up -> Face.North
                    Directions.Down -> Face.South
                }
            }
            Face.Reverse -> {
                when (direction) {
                    Directions.Right -> Face.West
                    Directions.Left -> Face.East
                    Directions.Up -> Face.North
                    Directions.Down -> Face.South
                }
            }
            Face.East -> {
                when (direction) {
                    Directions.Right -> Face.Reverse
                    Directions.Left -> Face.Obverse
                    Directions.Up -> Face.North
                    Directions.Down -> Face.South
                }
            }
        }
    }
}