package org.alpacaindustries.iremiaminigamecore.minigame

import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Example implementation of a Kotlin minigame that demonstrates
 * how to extend the SurvivalMinigameKt class
 */
class ExampleKotlinMinigame(
    id: String,
    manager: MinigameManager
) : SurvivalMinigame(id, "Example Game", manager) {

    override val gamePrefix: Component = Component.text("[Example] ", NamedTextColor.GREEN)

    override fun preparePlayer(player: Player) {
        player.health = 20.0
        player.foodLevel = 20
        player.saturation = 20.0f
        player.sendMessage(gamePrefix.append(Component.text("Game starting! Good luck!")))
    }

    override fun onCountdown(secondsLeft: Int) {
        broadcastMessage(gamePrefix.append(Component.text("Game starts in $secondsLeft seconds!")))
    }

    override fun updateScoreboard() {
        scoreboard.setLine(1, Component.text("Alive: ", NamedTextColor.GREEN)
            .append(Component.text("${alivePlayers.size}", NamedTextColor.WHITE)))
        scoreboard.setLine(2, Component.text("Total: ", NamedTextColor.YELLOW)
            .append(Component.text("$playerCount", NamedTextColor.WHITE)))
    }

    override fun broadcastGameStart() {
        broadcastMessage(gamePrefix.append(Component.text("The game has begun! Last player standing wins!")))
    }

    override fun handleGameEnd() {
        val winner = alivePlayers.firstOrNull()?.let { getPlayerById(it) }

        if (winner != null) {
            broadcastMessage(gamePrefix.append(
                Component.text("${winner.name} wins!", NamedTextColor.GOLD)
            ))
        } else {
            broadcastMessage(gamePrefix.append(
                Component.text("No winner - game ended!", NamedTextColor.RED)
            ))
        }
    }

    override fun shouldEndGame(): Boolean {
        return alivePlayers.size <= 1
    }

    override fun onPlayerEliminated(player: Player) {
        player.sendMessage(gamePrefix.append(
            Component.text("You have been eliminated!", NamedTextColor.RED)
        ))
    }
}

/**
 * Factory for creating example minigames
 */
class ExampleMinigameFactory : MinigameFactory {
    override fun createMinigame(id: String, manager: MinigameManager): Minigame {
        return ExampleKotlinMinigame(id, manager)
    }
}
