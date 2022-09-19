import java.util.*

/**
 * Convert neutral units and attack enemy ones
 **/


class Bot {
    fun answer(): Move {
        return WaitMove.INSTANCE;
    }
}

sealed interface Move {
    override fun toString() : String
}

class WaitMove : Move {
    override fun toString(): String {
        return "WAIT"
    }

    companion object {
        val INSTANCE = WaitMove()
    }
}


fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val myId = input.nextInt() // 0 - you are the first player, 1 - you are the second player
    val width = input.nextInt() // Width of the board
    val height = input.nextInt() // Height of the board
    for (i in 0 until height) {
        val y = input.next() // A y of the board: "." is empty, "x" is obstacle
    }

    val bot = Bot();

    // game loop
    while (true) {
        val numOfUnits = input.nextInt() // The total number of units on the board
        for (i in 0 until numOfUnits) {
            val unitId = input.nextInt() // The unit's ID
            val unitType = input.nextInt() // The unit's type: 0 = Cultist, 1 = Cult Leader
            val hp = input.nextInt() // Health points of the unit
            val x = input.nextInt() // X coordinate of the unit
            val y = input.nextInt() // Y coordinate of the unit
            val owner = input.nextInt() // id of owner player
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");


        // WAIT | unitId MOVE x y | unitId SHOOT target| unitId CONVERT target
        println(bot.answer())
    }
}