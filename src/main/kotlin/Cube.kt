package main.kotlin

import processing.core.PApplet
import processing.core.PConstants
import processing.core.PImage
import processing.core.PVector
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.pow

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

        val scale = 10 / (size / 16f)
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

    fun randomPosition(): Position {
        return Pair(
            IntVector(app.random(size.toFloat()).toInt(), app.random(size.toFloat()).toInt()),
            FaceType.values()[app.random(6f).toInt()]
        )
    }

    /**
     * Gets the hemisphere of a pixel
     *
     * @param position position of pixel
     * @return North or South
     */
    fun getHemisphere(position: Position): FaceType {
        return when (position.second) {
            FaceType.North -> FaceType.North
            FaceType.South -> FaceType.South
            else -> {
                if (position.first.y < size / 2) FaceType.North
                else FaceType.South
            }
        }
    }

    /**
     * Gets the latitude of a pixel, with 0 at the equator and 1 at the poles
     *
     * @param position position of pixel
     * @return latitude, with 0 at the equator and 1 at the poles
     */
    fun getLatitude(position: Position): Float {
        val poleEdge = PApplet.sqrt((size / 2f).pow(2) * 2)
        val odd = if (size % 2 == 0) 0.5f else 0f
        val distance = if (position.second == FaceType.North || position.second == FaceType.South) {
            // Square
            max((size / 2 - position.first.x).absoluteValue, (size / 2 - position.first.y).absoluteValue).toFloat()
        } else {
            // Linear
            size / 2 - (position.first.y + odd - size / 2).absoluteValue + poleEdge
        }
        return PApplet.map(distance, size / 2f - odd + poleEdge, 0f, 0f, 1f)
    }

    fun changePositionSpherical(position: Position, change: IntVector): Position {
        var pos = position.copy()
        if (change.x < 0) {
            for (i in change.x until  0) {
                pos = changePositionSpherical(pos, Directions.Left)
            }
        } else if (change.x > 0) {
            for (i in 0 until  change.x) {
                pos = changePositionSpherical(pos, Directions.Right)
            }
        }
        if (change.y < 0) {
            for (i in change.y until  0) {
                pos = changePositionSpherical(pos, Directions.Up)
            }
        } else if (change.y > 0) {
            for (i in 0 until  change.y) {
                pos = changePositionSpherical(pos, Directions.Down)
            }
        }
        return pos
    }

    fun changePositionSpherical(position: Position, direction: Directions): Position {
        if (position.second == FaceType.North || position.second == FaceType.South) {
            val quadrant = getQuadrant(position.first, direction)
            return when (direction) {
                Directions.Right -> Pair(
                    when (quadrant) {
                        Directions.Down -> position.first + Directions.Right
                        Directions.Right -> position.first + Directions.Up
                        Directions.Up -> position.first + Directions.Left
                        Directions.Left -> position.first + Directions.Down
                    },
                    position.second
                )
                Directions.Left -> Pair(
                    when (quadrant) {
                        Directions.Down -> position.first + Directions.Left
                        Directions.Right -> position.first + Directions.Down
                        Directions.Up -> position.first + Directions.Right
                        Directions.Left -> position.first + Directions.Up
                    },
                    position.second
                )
                Directions.Up ->
                    if (position.second == FaceType.North) {
                        Pair(when (quadrant) {
                            Directions.Down -> position.first + Directions.Up
                            Directions.Right -> position.first + Directions.Left
                            Directions.Up -> position.first + Directions.Down
                            Directions.Left -> position.first + Directions.Right
                        },
                        position.second)
                    } else {
                        when (quadrant) {
                            Directions.Down ->
                                if (position.first.y == size - 1) {
                                    Pair(IntVector(size - 1 - position.first.x, size - 1), FaceType.Reverse)
                                } else {
                                    Pair(position.first + Directions.Down, position.second)
                                }
                            Directions.Right ->
                                if (position.first.x == size - 1) {
                                    Pair(IntVector(position.first.y, size - 1), FaceType.East)
                                } else {
                                    Pair(position.first + Directions.Right, position.second)
                                }
                            Directions.Up ->
                                if (position.first.y == 0) {
                                    Pair(IntVector(position.first.x, size - 1), FaceType.Obverse)
                                } else {
                                    Pair(position.first + Directions.Up, position.second)
                                }
                            Directions.Left ->
                                if (position.first.x == 0) {
                                    Pair(IntVector(size - 1 - position.first.y, size - 1), FaceType.West)
                                } else {
                                    Pair(position.first + Directions.Left, position.second)
                                }
                        }
                    }
                Directions.Down ->
                    if (position.second == FaceType.North) {
                        when (quadrant) {
                            Directions.Down ->
                                if (position.first.y == size - 1) {
                                    Pair(IntVector(position.first.x, 0), FaceType.Obverse)
                                } else {
                                    Pair(position.first + Directions.Down, position.second)
                                }
                            Directions.Right ->
                                if (position.first.x == size - 1) {
                                    Pair(IntVector(size - 1 - position.first.y, 0), FaceType.East)
                                } else {
                                    Pair(position.first + Directions.Right, position.second)
                                }
                            Directions.Up ->
                                if (position.first.y == 0) {
                                    Pair(IntVector(size - 1 - position.first.x, 0), FaceType.Reverse)
                                } else {
                                    Pair(position.first + Directions.Up, position.second)
                                }
                            Directions.Left ->
                                if (position.first.x == 0) {
                                    Pair(IntVector(position.first.y, 0), FaceType.West)
                                } else {
                                    Pair(position.first + Directions.Left, position.second)
                                }
                        }
                    } else {
                        Pair(when (quadrant) {
                            Directions.Down -> position.first + Directions.Up
                            Directions.Right -> position.first + Directions.Left
                            Directions.Up -> position.first + Directions.Down
                            Directions.Left -> position.first + Directions.Right
                        },
                        position.second)
                    }
            }
        } else {
            // Right across edge
            if (position.first.x == size - 1 && direction == Directions.Right) {
                return Pair(IntVector(0, position.first.y), position.second + direction)
            } // Left across edge
            else if (position.first.x == 0 && direction == Directions.Left) {
                return Pair(IntVector(size - 1, position.first.y), position.second + direction)
            } // Up across edge
            else if (position.first.y == 0 && direction == Directions.Up) {
                return Pair(
                    when (position.second) {
                        FaceType.Obverse -> IntVector(position.first.x, size - 1)
                        FaceType.East -> IntVector(size - 1, size - 1 - position.first.x)
                        FaceType.Reverse -> IntVector(size - 1 - position.first.x, 0)
                        FaceType.West -> IntVector(0, position.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    position.second + direction
                )
            } // Down across edge
            else if (position.first.y == size - 1 && direction == Directions.Down) {
                return Pair(
                    when (position.second) {
                        FaceType.Obverse -> IntVector(position.first.x, 0)
                        FaceType.East -> IntVector(size - 1, position.first.x)
                        FaceType.Reverse -> IntVector(size - 1 - position.first.x, size - 1)
                        FaceType.West -> IntVector(0, size - 1 - position.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    position.second + direction
                )
            } // Within board
            else return Pair(position.first + direction, position.second)
        }
    }

    fun changePositionCubical(position: Position, change: IntVector): Position {
        var pos = position.copy()
        if (change.x < 0) {
            for (i in change.x until  0) {
                pos = changePositionCubical(pos, Directions.Left)
            }
        } else if (change.x > 0) {
            for (i in 0 until  change.x) {
                pos = changePositionCubical(pos, Directions.Right)
            }
        }
        if (change.y < 0) {
            for (i in change.y until  0) {
                pos = changePositionCubical(pos, Directions.Up)
            }
        } else if (change.y > 0) {
            for (i in 0 until  change.y) {
                pos = changePositionCubical(pos, Directions.Down)
            }
        }
        return pos
    }

    fun changePositionCubical(position: Position, direction: Directions): Position {
        if (position.second == FaceType.North) {
            // Right across edge
            return if (position.first.x == size - 1 && direction == Directions.Right) {
                Pair(IntVector(size - 1 - position.first.y, 0), position.second + direction)
            } // Left across edge
            else if (position.first.x == 0 && direction == Directions.Left) {
                Pair(IntVector(position.first.y, 0), position.second + direction)
            } // Up across edge
            else if (position.first.y == 0 && direction == Directions.Up) {
                Pair(IntVector(size - 1 - position.first.x, size - 1), position.second + direction)
            } // Down across edge
            else if (position.first.y == size - 1 && direction == Directions.Down) {
                Pair(IntVector(position.first.x, size - 1), position.second + direction)
            } // Within board
            else Pair(position.first + direction, position.second)
        } else if (position.second == FaceType.South) {
            // Right across edge
            return if (position.first.x == size - 1 && direction == Directions.Right) {
                Pair(IntVector(position.first.y, size - 1), position.second + direction)
            } // Left across edge
            else if (position.first.x == 0 && direction == Directions.Left) {
                Pair(IntVector(size - 1 - position.first.y, size - 1), position.second + direction)
            } // Up across edge
            else if (position.first.y == 0 && direction == Directions.Up) {
                Pair(IntVector(position.first.x, size - 1), position.second + direction)
            } // Down across edge
            else if (position.first.y == size - 1 && direction == Directions.Down) {
                Pair(IntVector(size - 1 - position.first.x, size - 1), position.second + direction)
            } // Within board
            else Pair(position.first + direction, position.second)
        } else {
            // Right across edge
            if (position.first.x == size - 1 && direction == Directions.Right) {
                return Pair(IntVector(0, position.first.y), position.second + direction)
            } // Left across edge
            else if (position.first.x == 0 && direction == Directions.Left) {
                return Pair(IntVector(size - 1, position.first.y), position.second + direction)
            } // Up across edge
            else if (position.first.y == 0 && direction == Directions.Up) {
                return Pair(
                    when (position.second) {
                        FaceType.Obverse -> IntVector(position.first.x, size - 1)
                        FaceType.East -> IntVector(size - 1, size - 1 - position.first.x)
                        FaceType.Reverse -> IntVector(size - 1 - position.first.x, 0)
                        FaceType.West -> IntVector(0, position.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    position.second + direction
                )
            } // Down across edge
            else if (position.first.y == size - 1 && direction == Directions.Down) {
                return Pair(
                    when (position.second) {
                        FaceType.Obverse -> IntVector(position.first.x, 0)
                        FaceType.East -> IntVector(size - 1, position.first.x)
                        FaceType.Reverse -> IntVector(size - 1 - position.first.x, size - 1)
                        FaceType.West -> IntVector(0, size - 1 - position.first.x)
                        else -> throw RuntimeException("Should not be possible")
                    },
                    position.second + direction
                )
            } // Within board
            else return Pair(position.first + direction, position.second)
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