package main.kotlin

import processing.core.PConstants
import processing.core.PImage
import processing.core.PVector
import java.awt.Color

class Cube {

    enum class Directions {
        Right,
        Left,
        Up,
        Down
    }

    enum class Face(file: String) {
        North("n"),
        South("s"),
        East("e"),
        West("w"),
        Obverse("o"),
        Reverse("r");

//        var surface: PImage = app.loadImage("sprites/$file.png")
        var surface: PImage = app.createImage(SIZE, SIZE, PConstants.ARGB)

        init {
            surface.loadPixels()
            for (i in 0 until SIZE * SIZE) {
                surface.pixels[i] =
                    when (file) {
                        "n" -> Color.RED.rgb
                        "e" -> Color.BLUE.rgb
                        "w" -> Color.GREEN.rgb
                        "o" -> Color.YELLOW.rgb
                        "r" -> Color.MAGENTA.rgb
                        else -> Color.BLACK.rgb
                    }
            }
        }
    }

    companion object {
        // Must be even
        const val SIZE = 16
    }

    private val scale = 10f

    fun display(position: PVector) {
        app.image(Face.Obverse.surface, position.x, position.y, SIZE * scale, SIZE * scale)
        app.image(Face.North.surface, position.x, position.y - SIZE * scale, SIZE * scale, SIZE * scale)
        app.image(Face.South.surface, position.x, position.y + SIZE * scale, SIZE * scale, SIZE * scale)
        app.image(Face.East.surface, position.x + SIZE * scale, position.y, SIZE * scale, SIZE * scale)
        app.image(Face.West.surface, position.x - SIZE * scale, position.y, SIZE * scale, SIZE * scale)
        app.image(Face.Reverse.surface, position.x + SIZE * scale * 2, position.y, SIZE * scale, SIZE * scale)
    }

    fun netPixel(x: Int, y: Int): Int {
        val position = IntVector(x % SIZE, y % SIZE)
        val face = when {
            x < SIZE -> Face.West
            x > SIZE * 3 -> Face.Reverse
            x > SIZE * 2 -> Face.East
            y < SIZE -> Face.North
            y > SIZE * 2 -> Face.South
            else -> Face.Obverse
        }
        return face.surface.pixels[position.x + position.y * SIZE]
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
                                if (this.first.y == SIZE - 1) {
                                    Pair(IntVector(SIZE - 1 - this.first.x, SIZE - 1), Face.Reverse)
                                } else {
                                    Pair(this.first + Directions.Down, this.second)
                                }
                            Directions.Right ->
                                if (this.first.x == SIZE - 1) {
                                    Pair(IntVector(this.first.y, SIZE - 1), Face.East)
                                } else {
                                    Pair(this.first + Directions.Right, this.second)
                                }
                            Directions.Up ->
                                if (this.first.y == 0) {
                                    Pair(IntVector(this.first.x, SIZE - 1), Face.Obverse)
                                } else {
                                    Pair(this.first + Directions.Up, this.second)
                                }
                            Directions.Left ->
                                if (this.first.x == 0) {
                                    Pair(IntVector(SIZE - 1 - this.first.y, SIZE - 1), Face.West)
                                } else {
                                    Pair(this.first + Directions.Left, this.second)
                                }
                        }
                    }
                Directions.Down ->
                    if (this.second == Face.North) {
                        when (quadrant) {
                            Directions.Down ->
                                if (this.first.y == SIZE - 1) {
                                    Pair(IntVector(this.first.x, 0), Face.Obverse)
                                } else {
                                    Pair(this.first + Directions.Down, this.second)
                                }
                            Directions.Right ->
                                if (this.first.x == SIZE - 1) {
                                    Pair(IntVector(SIZE - 1 - this.first.y, 0), Face.East)
                                } else {
                                    Pair(this.first + Directions.Right, this.second)
                                }
                            Directions.Up ->
                                if (this.first.y == 0) {
                                    Pair(IntVector(SIZE - 1 - this.first.x, 0), Face.Reverse)
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
            if (this.first.x == SIZE - 1 && direction == Directions.Right) {
                return Pair(IntVector(0, this.first.y), this.second + direction)
            } // Left across edge
            else if (this.first.x == 0 && direction == Directions.Left) {
                return Pair(IntVector(SIZE - 1, this.first.y), this.second + direction)
            } // Up across edge
            else if (this.first.y == 0 && direction == Directions.Up) {
                return Pair(
                    when (this.second) {
                        Face.Obverse -> IntVector(this.first.x, SIZE - 1)
                        Face.East -> IntVector(SIZE - 1, SIZE - 1 - this.first.x)
                        Face.Reverse -> IntVector(SIZE - 1 - this.first.x, 0)
                        Face.West -> IntVector(0, this.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    this.second + direction
                )
            } // Down across edge
            else if (this.first.y == SIZE - 1 && direction == Directions.Down) {
                return Pair(
                    when (this.second) {
                        Face.Obverse -> IntVector(this.first.x, 0)
                        Face.East -> IntVector(SIZE - 1, this.first.x)
                        Face.Reverse -> IntVector(SIZE - 1 - this.first.x, SIZE - 1)
                        Face.West -> IntVector(0, SIZE - 1 - this.first.x)
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
            if (position.x > SIZE - 1 - position.y) {
                Directions.Right
            } else if (position.x < SIZE - 1 - position.y) {
                Directions.Up
            } else {
                if (direction == Directions.Left) Directions.Right
                else Directions.Up
            }
        } else if (position.x < position.y) {
            if (position.x > SIZE - 1 - position.y) {
                Directions.Down
            } else if (position.x < SIZE - 1 - position.y) {
                Directions.Left
            } else {
                if (direction == Directions.Left) Directions.Left
                else Directions.Down
            }
        } else {
            if (position.x > SIZE - 1 - position.y) {
                if (direction == Directions.Left) Directions.Down
                else Directions.Right
            } else if (position.x < SIZE - 1 - position.y) {
                if (direction == Directions.Left) Directions.Up
                else Directions.Left
            } else Directions.Right // very center, only possible if SIZE is odd
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