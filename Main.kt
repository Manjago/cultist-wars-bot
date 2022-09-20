import java.util.Scanner

/**
 * Convert neutral units and attack enemy ones
 **/


class Bot(private val myId: Int, private val width: Int, private val height: Int) {

    private val board = mutableMapOf<Cell, Item>()

    fun answer(): Move {
        return WaitMove.INSTANCE
    }

    fun fillBoardLine(y: Int, source: String) {
        check(source.length == width) { "source.length == width violated" }
        for (x in source.indices) {
            when (source[x]) {
                'x' -> board.put( Cell(x, y), ObstacleItem.INSTANCE)
                '.' -> board.put( Cell(x, y), EmptyItem.INSTANCE)
                else -> throw IllegalArgumentException("${source[x]} at $y $x")
            }
        }
    }

    fun clearBoard() {
        board.replaceAll { _, item ->
            if ((item !is ObstacleItem) && (item !is EmptyItem)) {
                EmptyItem.INSTANCE
            } else {
                item
            }
        }
    }

    fun setItemToBoard(itemId: Int, itemType: Int, x: Int, y: Int, hp: Int, owner: Int) {
        when (itemType) {
            0 -> board.put(Cell(x, y), Cultist(itemId, hp, int2Owner(owner)))
            1 -> board.put(Cell(x, y), CultLeader(itemId, hp, int2Owner(owner)))
        }
    }

    private fun int2Owner(value: Int): Owner =
        when (value) {
            NEUTRAL_VALUE -> Owner.NEUTRAL
            myId -> Owner.ME
            else -> Owner.ENEMY
        }


    private fun isValid(x: Int, y: Int): Boolean {
        return (x in 0 until width) && (y in 0 until height)
    }


    companion object {
        private const val NEUTRAL_VALUE = 2
    }
}

data class Cell(val x: Int, val y: Int)

sealed interface Item

class EmptyItem : Item {
    companion object {
        val INSTANCE = EmptyItem()
    }
}

class ObstacleItem: Item {
    companion object {
        val INSTANCE = ObstacleItem()
    }
}

abstract class Unit(val id: Int, var hp: Int, var owner: Owner) : Item {
    fun update(hp: Int, owner: Owner) {
        this.hp = hp
        this.owner = owner
    }
}

class Cultist(id: Int, hp: Int, owner: Owner) : Unit(id, hp, owner)
class CultLeader(id: Int, hp: Int, owner: Owner) : Unit(id, hp, owner)

sealed interface Move {
    override fun toString(): String
}

class WaitMove : Move {
    override fun toString(): String {
        return "WAIT"
    }

    companion object {
        val INSTANCE = WaitMove()
    }
}

enum class Owner {
    ME, ENEMY, NEUTRAL
}

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val myId = input.nextInt() // 0 - you are the first player, 1 - you are the second player
    val width = input.nextInt() // Width of the board
    val height = input.nextInt() // Height of the board

    val bot = Bot(myId, width, height)

    for (i in 0 until height) {
        val y = input.next() // A y of the board: "." is empty, "x" is obstacle
        bot.fillBoardLine(i, y)
    }


    // game loop
    while (true) {

        bot.clearBoard()

        val numOfUnits = input.nextInt() // The total number of units on the board
        for (i in 0 until numOfUnits) {
            val unitId = input.nextInt() // The unit's ID
            val unitType = input.nextInt() // The unit's type: 0 = Cultist, 1 = Cult Leader
            val hp = input.nextInt() // Health points of the unit
            val x = input.nextInt() // X coordinate of the unit
            val y = input.nextInt() // Y coordinate of the unit
            val owner = input.nextInt() // id of owner player
            bot.setItemToBoard(unitId, unitType, x, y, hp, owner)
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");


        // WAIT | unitId MOVE x y | unitId SHOOT target| unitId CONVERT target
        println(bot.answer())
    }
}