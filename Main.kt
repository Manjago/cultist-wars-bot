import java.lang.Math.abs
import java.util.PriorityQueue
import java.util.Scanner

/**
 * Convert neutral units and attack enemy ones
 **/

class Bot(private val board: Board) {

    private val P_SHOOT = 100_000
    private val P_CONVERT_NEAR = 800_000
    private val P_CONVERT_FAR = 900_000
    private val P_WAIT = 1_000_000

    private val DAMAGE = 7

    fun answer(): Move {

        val pq = PriorityQueue<PqItem>()

        val allCultLeaders = board.allMyCultLeaders()
        allCultLeaders.forEach { tryConvert(it, pq) }

        val allMyCultist = board.allMyCultist()
        val allEnemies = board.allEnemies()
        allMyCultist.forEach { tryShoot(it, pq, allEnemies) }

        pq.add(P_WAIT, WaitMove.INSTANCE)

        return pq.peek().move
    }

    data class UnitAndCellWithDistance(val cell: Cell, val distance: Int, val unit: Unit)

    private fun tryShoot(
        cultist: ItemWithCell<Cultist>,
        pq: PriorityQueue<PqItem>,
        allEnemies: List<ItemWithCell<Unit>>
    ) {

        val pretender: UnitAndCellWithDistance? = allEnemies.asSequence().map {
            UnitAndCellWithDistance(it.cell, it.cell.distance(cultist.cell), it.item)
        }.filter { it.distance < DAMAGE }
            .filter { board.bresIsFree(it.cell, cultist.cell) }
            .sortedBy { it.distance }.firstOrNull()

        if (pretender != null) {
            pq.add(P_SHOOT + pretender.distance, ShootMove(cultist.item.id, pretender.unit.id))
        }
    }

    private fun tryConvert(cultLeader: ItemWithCell<CultLeader>, pq: PriorityQueue<PqItem>) {
        val nearestVictims: List<ItemWithCell<Cultist>> = board.nearestVictimsForCultLeader(cultLeader.cell)
        when {
            nearestVictims.isNotEmpty() -> {
                val nearVictim = nearestVictims[0]
                pq.add(P_CONVERT_NEAR, ConvertMove(cultLeader.item.id, nearVictim.item.id))
            }

            else -> {
                val victim = board.pathToNearestVictimForCultLeader(cultLeader.cell)
                when {
                    victim != null -> {
                        pq.add(P_CONVERT_FAR, ConvertMove(cultLeader.item.id, victim.item.id))
                    }
                }
            }
        }
    }

    private fun PriorityQueue<PqItem>.add(priority: Int, move: Move) {
        debug("pq $priority $move")
        this.add(PqItem(priority, move))
    }

}

class PqItem(val priority: Int, val move: Move) : Comparable<PqItem> {
    override fun compareTo(other: PqItem): Int = priority.compareTo(other.priority)
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

    fun allMyCultist(): List<ItemWithCell<Cultist>> = board.entries.asSequence().filter {
        val value = it.value
        value is Cultist && value.owner == Owner.ME
    }.map { ItemWithCell(it.key, it.value as Cultist) }
        .toList()

    fun allEnemies(): List<ItemWithCell<Unit>> = board.entries.asSequence().filter {
        val value = it.value
        value is Unit && value.owner == Owner.ENEMY
    }.map { ItemWithCell(it.key, it.value as Unit) }
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

            if (cell == initial) {
                queue += getNeighbors(cell)
                continue
            }

            val item = board[cell]
            when (item) {
                is EmptyItem -> {
                    queue += getNeighbors(cell)
                }

                is ObstacleItem -> {
                    continue
                }

                null -> {
                    continue
                }

                is Unit -> {
                    when {
                        (item is Cultist) && (item.owner == Owner.NEUTRAL) -> return ItemWithCell(cell, item)
                        else -> continue
                    }
                }

            }

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
            val item = board[it]
            if (item is Cultist && item.owner == Owner.NEUTRAL) {
                result.add(ItemWithCell(it, item))
            }
        }
        cell.bottom()?.let {
            val item = board[it]
            if (item is Cultist && item.owner == Owner.NEUTRAL) {
                result.add(ItemWithCell(it, item))
            }
        }
        cell.left()?.let {
            val item = board[it]
            if (item is Cultist && item.owner == Owner.NEUTRAL) {
                result.add(ItemWithCell(it, item))
            }
        }
        cell.right()?.let {
            val item = board[it]
            if (item is Cultist && item.owner == Owner.NEUTRAL) {
                result.add(ItemWithCell(it, item))
            }
        }

        return result
    }

    fun bresIsFree(from: Cell, to: Cell): Boolean {
        val br = bres(from, to)
        if (br.size <= 2) {
            return true
        }

        val tested = br.slice(1..br.size - 2)
        val bad = tested.any { board[it] !is EmptyItem }
        return !bad
    }

    companion object {
        private const val NEUTRAL_VALUE = 2
    }
}

data class Cell(val x: Int, val y: Int) {
    fun distance(other: Cell): Int = abs(x - other.x) + abs(y - other.y)
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

abstract class Unit(val id: Int, var hp: Int, var owner: Owner) : Item {
    fun update(hp: Int, owner: Owner) {
        this.hp = hp
        this.owner = owner
    }

    override fun toString(): String {
        return "Unit(id=$id, hp=$hp, owner=$owner)"
    }

}

class Cultist(id: Int, hp: Int, owner: Owner) : Unit(id, hp, owner) {
    override fun toString(): String {
        return "Cultist() ${super.toString()}"
    }
}

class CultLeader(id: Int, hp: Int, owner: Owner) : Unit(id, hp, owner) {
    override fun toString(): String {
        return "CultLeader() ${super.toString()}"
    }
}

class ItemWithCell<T : Item>(val cell: Cell, val item: T) {
    override fun toString(): String {
        return "ItemWithCell(cell=$cell, item=$item)"
    }
}

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

class ShootMove(private val unitId: Int, private val targetId: Int) : Move {
    override fun toString(): String {
        return "$unitId SHOOT $targetId"
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


    val bot = Bot(board)
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


        // WAIT | unitId MOVE x y | unitId SHOOT target| unitId CONVERT target
        println(bot.answer())
    }
}


fun bres(from: Cell, to: Cell): List<Cell> {
    if (from.y > to.y) {
        return bres(to, from)
    }

    val result = mutableListOf<Cell>()
    drawBresenhamLine(from.x, from.y, to.x, to.y, result)
    return result
}

// https://ru.wikibooks.org/wiki/%D0%A0%D0%B5%D0%B0%D0%BB%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D0%B8_%D0%B0%D0%BB%D0%B3%D0%BE%D1%80%D0%B8%D1%82%D0%BC%D0%BE%D0%B2/%D0%90%D0%BB%D0%B3%D0%BE%D1%80%D0%B8%D1%82%D0%BC_%D0%91%D1%80%D0%B5%D0%B7%D0%B5%D0%BD%D1%85%D1%8D%D0%BC%D0%B0#%D0%A0%D0%B5%D0%B0%D0%BB%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8F_%D0%BD%D0%B0_Java
fun drawBresenhamLine(xStart: Int, yStart: Int, xEnd: Int, yEnd: Int, line: MutableList<Cell>) {

    fun sign(x: Int): Int = x.compareTo(0)

    fun plot(x: Int, y: Int) {
        line.add(Cell(x, y))
    }

    var dx: Int
    var dy: Int
    val pdx: Int
    val pdy: Int
    val es: Int
    val el: Int
    dx = xEnd - xStart
    dy = yEnd - yStart
    val incX: Int = sign(dx)
    val incY: Int = sign(dy)
    if (dx < 0) dx = -dx
    if (dy < 0) dy = -dy
    if (dx > dy) {
        pdx = incX
        pdy = 0
        es = dy
        el = dx
    } else {
        pdx = 0
        pdy = incY
        es = dx
        el = dy
    }
    var x: Int = xStart
    var y: Int = yStart
    var err: Int = el / 2
    plot(x, y)
    for (t in 0 until el) {
        err -= es
        if (err < 0) {
            err += el
            x += incX
            y += incY
        } else {
            x += pdx
            y += pdy
        }
        plot(x, y)
    }
}

fun debug(string: String) {
    System.err.println(string)
}