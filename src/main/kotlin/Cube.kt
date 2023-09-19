package main.kotlin

import processing.core.PConstants
import processing.core.PImage
import processing.core.PVector
import java.awt.Color

class Cube(private val size: Int) {

    enum class Directions {
        Right,
        Left,
        Up,
        Down
    }

    data class Face(val size: Int, val faceType: FaceType) {
        private val img: PImage = app.createImage(size, size, PConstants.ARGB)

        init {
            img.loadPixels()
            for (i in 0 until size * size) {
                img.pixels[i] =
                    when (faceType) {
                        FaceType.North -> Color.RED.rgb
                        FaceType.East -> Color.CYAN.rgb
                        FaceType.West -> Color.GREEN.rgb
                        FaceType.Obverse -> Color.YELLOW.rgb
                        FaceType.Reverse -> Color.MAGENTA.rgb
                        FaceType.South -> Color.BLUE.rgb
                    }
            }
        }

        fun getPixel(position: IntVector): Int {
            return img.pixels[position.x + position.y * size]
        }

        fun setPixel(position: IntVector, color: Int) {
            img.pixels[position.x + position.y * size] = color
            img.updatePixels()
        }
    }

    enum class FaceType {
        North,
        South,
        East,
        West,
        Obverse,
        Reverse;
    }

    private val north = Face(size, FaceType.North)
    private val south = Face(size, FaceType.South)
    private val east = Face(size, FaceType.East)
    private val west = Face(size, FaceType.West)
    private val obverse = Face(size, FaceType.Obverse)
    private val reverse = Face(size, FaceType.Reverse)

    private val scale = 10f

    fun display(position: PVector) {
        val img = app.createImage(size * 4, size * 3, PConstants.ARGB);
        img.loadPixels()

        for (x in 0 until size * 4) {
            for (y in 0 until size * 3) {
                val i = x + y * size * 4

                img.pixels[i] = netPixel(IntVector(x, y))
            }
        }
        img.updatePixels()

        app.image(img, position.x, position.y, size * 4 * scale, size * 3 * scale)
    }

    fun setPixel(position: IntVector, face: FaceType, color: Int) {
        when (face) {
            FaceType.North -> north.setPixel(position, color)
            FaceType.South -> south.setPixel(position, color)
            FaceType.East -> east.setPixel(position, color)
            FaceType.West -> west.setPixel(position, color)
            FaceType.Obverse -> obverse.setPixel(position, color)
            FaceType.Reverse -> reverse.setPixel(position, color)
        }
    }

    fun netPixel(position: IntVector): Int {
        val facePos = IntVector(position.x % size, position.y % size)
        return when {
            position.y < size -> // row 1
                when {
                    position.x < size -> north.getPixel(IntVector(size - 1 - facePos.y, facePos.x))
                    position.x < size * 2 -> north.getPixel(facePos)
                    position.x < size * 3 -> north.getPixel(IntVector(facePos.y, size - 1 - facePos.x))
                    else -> north.getPixel(IntVector(size - 1 - facePos.x, size - 1 - facePos.y))
                }
            position.y < size * 2 -> // row 2
                when {
                    position.x < size -> west.getPixel(facePos)
                    position.x < size * 2 -> obverse.getPixel(facePos)
                    position.x < size * 3 -> east.getPixel(facePos)
                    else -> reverse.getPixel(facePos)
                }
            else -> // row 3
                when {
                    position.x < size -> south.getPixel(IntVector(facePos.y, size - 1 - facePos.x))
                    position.x < size * 2 -> south.getPixel(facePos)
                    position.x < size * 3 -> south.getPixel(IntVector(size - 1 - facePos.y, facePos.x))
                    else -> south.getPixel(IntVector(size - 1 - facePos.x, size - 1 - facePos.y))
                }
        }
    }

    operator fun Position.plus(direction: Directions): Position {
        if (this.second == FaceType.North || this.second == FaceType.South) {
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
                    if (this.second == FaceType.North) {
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
                                    Pair(IntVector(size - 1 - this.first.x, size - 1), FaceType.Reverse)
                                } else {
                                    Pair(this.first + Directions.Down, this.second)
                                }
                            Directions.Right ->
                                if (this.first.x == size - 1) {
                                    Pair(IntVector(this.first.y, size - 1), FaceType.East)
                                } else {
                                    Pair(this.first + Directions.Right, this.second)
                                }
                            Directions.Up ->
                                if (this.first.y == 0) {
                                    Pair(IntVector(this.first.x, size - 1), FaceType.Obverse)
                                } else {
                                    Pair(this.first + Directions.Up, this.second)
                                }
                            Directions.Left ->
                                if (this.first.x == 0) {
                                    Pair(IntVector(size - 1 - this.first.y, size - 1), FaceType.West)
                                } else {
                                    Pair(this.first + Directions.Left, this.second)
                                }
                        }
                    }
                Directions.Down ->
                    if (this.second == FaceType.North) {
                        when (quadrant) {
                            Directions.Down ->
                                if (this.first.y == size - 1) {
                                    Pair(IntVector(this.first.x, 0), FaceType.Obverse)
                                } else {
                                    Pair(this.first + Directions.Down, this.second)
                                }
                            Directions.Right ->
                                if (this.first.x == size - 1) {
                                    Pair(IntVector(size - 1 - this.first.y, 0), FaceType.East)
                                } else {
                                    Pair(this.first + Directions.Right, this.second)
                                }
                            Directions.Up ->
                                if (this.first.y == 0) {
                                    Pair(IntVector(size - 1 - this.first.x, 0), FaceType.Reverse)
                                } else {
                                    Pair(this.first + Directions.Up, this.second)
                                }
                            Directions.Left ->
                                if (this.first.x == 0) {
                                    Pair(IntVector(this.first.y, 0), FaceType.West)
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
                        FaceType.Obverse -> IntVector(this.first.x, size - 1)
                        FaceType.East -> IntVector(size - 1, size - 1 - this.first.x)
                        FaceType.Reverse -> IntVector(size - 1 - this.first.x, 0)
                        FaceType.West -> IntVector(0, this.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    this.second + direction
                )
            } // Down across edge
            else if (this.first.y == size - 1 && direction == Directions.Down) {
                return Pair(
                    when (this.second) {
                        FaceType.Obverse -> IntVector(this.first.x, 0)
                        FaceType.East -> IntVector(size - 1, this.first.x)
                        FaceType.Reverse -> IntVector(size - 1 - this.first.x, size - 1)
                        FaceType.West -> IntVector(0, size - 1 - this.first.x)
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

    operator fun FaceType.plus(direction: Directions): FaceType {
        return when (this) {
            FaceType.North -> {
                when (direction) {
                    Directions.Right -> FaceType.East
                    Directions.Left -> FaceType.West
                    Directions.Up -> FaceType.Reverse
                    Directions.Down -> FaceType.Obverse
                }
            }
            FaceType.South -> {
                when (direction) {
                    Directions.Right -> FaceType.East
                    Directions.Left -> FaceType.West
                    Directions.Up -> FaceType.Obverse
                    Directions.Down -> FaceType.Reverse
                }
            }
            FaceType.Obverse -> {
                when (direction) {
                    Directions.Right -> FaceType.East
                    Directions.Left -> FaceType.West
                    Directions.Up -> FaceType.North
                    Directions.Down -> FaceType.South
                }
            }
            FaceType.West -> {
                when (direction) {
                    Directions.Right -> FaceType.Obverse
                    Directions.Left -> FaceType.Reverse
                    Directions.Up -> FaceType.North
                    Directions.Down -> FaceType.South
                }
            }
            FaceType.Reverse -> {
                when (direction) {
                    Directions.Right -> FaceType.West
                    Directions.Left -> FaceType.East
                    Directions.Up -> FaceType.North
                    Directions.Down -> FaceType.South
                }
            }
            FaceType.East -> {
                when (direction) {
                    Directions.Right -> FaceType.Reverse
                    Directions.Left -> FaceType.Obverse
                    Directions.Up -> FaceType.North
                    Directions.Down -> FaceType.South
                }
            }
        }
    }
}