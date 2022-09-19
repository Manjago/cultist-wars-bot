import java.util.Scanner

/**
 * Convert neutral units and attack enemy ones
 **/


class Bot(private val myId: Int, private val width: Int, private val height: Int) {

    private val board: Array<Array<Item>> = Array(height) { Array(width) { EmptyItem.INSTANCE } }
    private val visited = mutableSetOf<Int>()
    private val allCultists = mutableMapOf<Int, Cultist>()
    private val allCultLeaders = mutableMapOf<Int, CultLeader>()

    fun answer(): Move {
        return WaitMove.INSTANCE
    }

    fun fillBoardLine(y: Int, source: String) {
        check(source.length == width) { "source.length == width" }
        for (i in source.indices) {
            when (source[i]) {
                'x' -> board[y][i] = ObstacleItem.INSTANCE
                '.' -> board[y][i] = EmptyItem.INSTANCE
                else -> throw IllegalArgumentException("${source[i]} at $y $i")
            }
        }
    }

    fun clear() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (board[y][x] !is ObstacleItem) {
                    board[y][x] = EmptyItem.INSTANCE
                }
            }
        }
        visited.clear()
    }

    fun setItem(itemId: Int, itemType: Int, x: Int, y: Int, hp: Int, owner: Int) {
        when (itemType) {
            0 -> {
                val cultist = allCultists[itemId]
                when {
                    cultist != null -> cultist.update(x, y, hp, int2Owner(owner))
                    else -> allCultists[itemId] = Cultist(itemId, hp, int2Owner(owner), x, y, true)
                }
            }

            1 -> {
                val cultLeader = allCultLeaders[itemId]
                when {
                    cultLeader != null -> cultLeader.update(x, y, hp, int2Owner(owner))
                    else -> allCultLeaders[itemId] = CultLeader(itemId, hp, int2Owner(owner), x, y, true)
                }
            }
        }
        visited.add(itemId)
    }

    fun markDead() {
        allCultists.values.forEach { it.kill(visited) }
        allCultLeaders.values.forEach { it.kill(visited) }
    }

    private fun int2Owner(value: Int): Owner =
        when (value) {
            NEUTRAL_VALUE -> Owner.NEUTRAL
            myId -> Owner.ME
            else -> Owner.ENEMY
        }

    companion object {
        private const val NEUTRAL_VALUE = 2
    }
}

sealed interface Item

class EmptyItem : Item {
    companion object {
        val INSTANCE = EmptyItem()
    }
}

class ObstacleItem : Item {
    companion object {
        val INSTANCE = ObstacleItem()
    }
}

abstract class Unit(val id: Int, var hp: Int, var owner: Owner, var x: Int, var y: Int, var alive: Boolean) : Item {
    fun update(x: Int, y: Int, hp: Int, owner: Owner) {
        this.x = x
        this.y = y
        this.hp = hp
        this.owner = owner
    }

    fun kill(visited: Set<Int>) {
        if (!visited.contains(this.id)) {
            this.alive = false
        }
    }
}

class Cultist(id: Int, hp: Int, owner: Owner, x: Int, y: Int, alive: Boolean) : Unit(id, hp, owner, x, y, alive)
class CultLeader(id: Int, hp: Int, owner: Owner, x: Int, y: Int, alive: Boolean) : Unit(id, hp, owner, x, y, alive)

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

        bot.clear()

        val numOfUnits = input.nextInt() // The total number of units on the board
        for (i in 0 until numOfUnits) {
            val unitId = input.nextInt() // The unit's ID
            val unitType = input.nextInt() // The unit's type: 0 = Cultist, 1 = Cult Leader
            val hp = input.nextInt() // Health points of the unit
            val x = input.nextInt() // X coordinate of the unit
            val y = input.nextInt() // Y coordinate of the unit
            val owner = input.nextInt() // id of owner player
            bot.setItem(unitId, unitType, x, y, hp, owner)
        }

        bot.markDead()
        // Write an action using println()
        // To debug: System.err.println("Debug messages...");


        // WAIT | unitId MOVE x y | unitId SHOOT target| unitId CONVERT target
        println(bot.answer())
    }
}