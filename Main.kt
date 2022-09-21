import java.util.Scanner

/**
 * Convert neutral units and attack enemy ones
 **/

class Bot(private val board: Board) {
    fun answer(): Move {

        val allCultLeaders = board.allMyCultLeaders()
        allCultLeaders.forEach {

            val nearestVictims: List<ItemWithCell<Cultist>> = board.nearestVictimsForCultLeader(it.cell)
            if (nearestVictims.isNotEmpty()) {
                val nearVictim = nearestVictims[0]
                System.err.println("near found: ${nearVictim}")
                return ConvertMove(it.item.id, nearVictim.item.id)
            }

            val victim = board.pathToNearestVictimForCultLeader(it.cell)
            System.err.println("c ${it.cell} ${it.item.id} found $victim")
            if (victim != null) {
                return ConvertMove(it.item.id, victim.item.id)
            }
        }

        return WaitMove.INSTANCE
    }
}

class Board(private val myId: Int, private val width: Int, private val height: Int) {

    private val board = mutableMapOf<Cell, Item>()

    fun fillBoardLine(y: Int, source: String) {
        check(source.length == width) { "source.length == width violated" }
        for (x in source.indices) {
            when (source[x]) {
                'x' -> board.put(Cell(x, y), ObstacleItem.INSTANCE)
                '.' -> board.put(Cell(x, y), EmptyItem.INSTANCE)
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

    fun Cell.top() = Cell(x, y - 1).checkBounds()
    fun Cell.right() = Cell(x + 1, y).checkBounds()
    fun Cell.bottom() = Cell(x, y + 1).checkBounds()
    fun Cell.left() = Cell(x - 1, y).checkBounds()

    private fun Cell.checkBounds(): Cell? {
        if (board[this] != null) {
            return this
        } else {
            return null
        }
    }

    fun allMyCultLeaders(): List<ItemWithCell<CultLeader>> = board.entries.asSequence().filter {
        val value = it.value
        value is CultLeader && value.owner == Owner.ME
    }.map { ItemWithCell(it.key, it.value as CultLeader) }
        .toList()

    fun pathToNearestVictimForCultLeader(initial: Cell): ItemWithCell<Cultist>? {

        val visited = mutableSetOf<Cell>()
        val queue = ArrayDeque<Cell>()
        queue += initial
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (visited.contains(cell)) {
                continue
            }
            visited += cell

            val item = board[cell]
            when (item) {
                is EmptyItem -> queue += getNeighbors(cell)
                is ObstacleItem -> continue
                is Unit -> {
                    when {
                        (item is Cultist) && (item.owner != Owner.ME) -> return ItemWithCell(cell, item)
                        else -> continue
                    }
                }

                null -> continue
            }

            queue += getNeighbors(cell)
        }

        return null
    }

    private fun getNeighbors(cell: Cell): List<Cell> {
        val result = mutableListOf<Cell>()

        cell.top()?.let { result.add(it) }
        cell.bottom()?.let { result.add(it) }
        cell.left()?.let { result.add(it) }
        cell.right()?.let { result.add(it) }

        return result
    }

    fun nearestVictimsForCultLeader(cell: Cell): List<ItemWithCell<Cultist>> {
        val result = mutableListOf<ItemWithCell<Cultist>>()

        cell.top()?.let {
            val item = board[cell]
            if (item is Cultist && item.owner != Owner.ME) {
                result.add(ItemWithCell(it, item))
            }
        }
        cell.bottom()?.let {
            val item = board[cell]
            if (item is Cultist && item.owner != Owner.ME) {
                result.add(ItemWithCell(it, item))
            }
        }
        cell.left()?.let {
            val item = board[cell]
            if (item is Cultist && item.owner != Owner.ME) {
                result.add(ItemWithCell(it, item))
            }
        }
        cell.right()?.let {
            val item = board[cell]
            if (item is Cultist && item.owner != Owner.ME) {
                result.add(ItemWithCell(it, item))
            }
        }

        return result
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

class ObstacleItem : Item {
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

class ItemWithCell<T : Item>(val cell: Cell, val item: T)

sealed interface Move {
    override fun toString(): String
}

class WaitMove : Move {
    override fun toString(): String = "WAIT"

    companion object {
        val INSTANCE = WaitMove()
    }
}

class ConvertMove(private val unitId: Int, private val targetId: Int) : Move {
    override fun toString(): String {
        return "$unitId CONVERT $targetId"
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

    val board = Board(myId, width, height)

    for (i in 0 until height) {
        val y = input.next() // A y of the board: "." is empty, "x" is obstacle
        board.fillBoardLine(i, y)
    }


    // game loop
    while (true) {

        board.clearBoard()

        val numOfUnits = input.nextInt() // The total number of units on the board
        for (i in 0 until numOfUnits) {
            val unitId = input.nextInt() // The unit's ID
            val unitType = input.nextInt() // The unit's type: 0 = Cultist, 1 = Cult Leader
            val hp = input.nextInt() // Health points of the unit
            val x = input.nextInt() // X coordinate of the unit
            val y = input.nextInt() // Y coordinate of the unit
            val owner = input.nextInt() // id of owner player
            board.setItemToBoard(unitId, unitType, x, y, hp, owner)
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");


        // WAIT | unitId MOVE x y | unitId SHOOT target| unitId CONVERT target
        val bot = Bot(board)
        println(bot.answer())
    }
}