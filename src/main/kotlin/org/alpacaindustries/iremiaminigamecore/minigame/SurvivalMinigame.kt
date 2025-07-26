package org.alpacaindustries.iremiaminigamecore.minigame

import org.alpacaindustries.iremiaminigamecore.system.ui.GameScoreboard
import org.alpacaindustries.iremiaminigamecore.util.CountdownTimer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.*

/**
 * Abstract base class for survival-based minigames where players can be eliminated
 * Provides common functionality for player elimination, countdown management, and event handling
 */
abstract class SurvivalMinigame(
    id: String,
    displayName: String,
    manager: MinigameManager,
    countdownSeconds: Int = DEFAULT_COUNTDOWN_SECONDS
) : Minigame(id, displayName, manager) {

    protected val countdownTimer: CountdownTimer = CountdownTimer(manager.plugin, countdownSeconds)
        .onCount(::onCountdown)
        .onFinish(::start)

    protected val scoreboard: GameScoreboard = GameScoreboard(Component.text(displayName, NamedTextColor.GOLD))

    val alivePlayers: MutableList<UUID> = mutableListOf()

    companion object {
        private const val DEFAULT_COUNTDOWN_SECONDS = 10
        private const val DEFAULT_Y_THRESHOLD = 70
    }

    // Secondary constructor for default countdown
    constructor(id: String, displayName: String, manager: MinigameManager) :
        this(id, displayName, manager, DEFAULT_COUNTDOWN_SECONDS)

    override fun onStart() {
        super.onStart()

        alivePlayers.clear()
        getValidOnlinePlayers().forEach { player ->
            alivePlayers.add(player.uniqueId)
            preparePlayer(player)
        }

        updateScoreboard()
        broadcastGameStart()
    }

    override fun onEnd() {
        super.onEnd()

        countdownTimer.stop()
        handleGameEnd()
        cleanupPlayers()
    }

    override fun onPlayerJoin(player: Player) {
        super.onPlayerJoin(player)

        when (state) {
            MinigameState.WAITING, MinigameState.COUNTDOWN -> {
                updateWaitingScoreboard()
                scoreboard.showTo(player)
            }
            MinigameState.RUNNING -> {
                player.gameMode = GameMode.SPECTATOR
                scoreboard.showTo(player)
                player.sendMessage(gamePrefix.append(Component.text("Game in progress! You are now spectating.")))
            }
            MinigameState.ENDED -> {
                // Game has ended, handle accordingly
            }
        }
    }

    override fun onPlayerLeave(player: Player) {
        super.onPlayerLeave(player)

        alivePlayers.remove(player.uniqueId)
        scoreboard.hideFrom(player)
        player.gameMode = GameMode.ADVENTURE

        onPlayerEliminated(player)
        checkWinCondition()
    }

    override fun onCountdownStart() {
        super.onCountdownStart()
        countdownTimer.start()
    }

    /**
     * Handle player elimination from the game
     */
    protected fun eliminatePlayer(player: Player, reason: String) {
        if (!alivePlayers.contains(player.uniqueId)) {
            return
        }

        alivePlayers.remove(player.uniqueId)
        player.gameMode = GameMode.SPECTATOR

        broadcastMessage(
            gamePrefix.append(
                Component.text(player.name, NamedTextColor.YELLOW)
                    .append(Component.text(" $reason!", NamedTextColor.RED))
            )
        )

        onPlayerEliminated(player)
        updateScoreboard()
        checkWinCondition()
    }

    /**
     * Check if the game should end based on remaining players
     */
    protected fun checkWinCondition() {
        if (state != MinigameState.RUNNING) {
            return
        }

        if (shouldEndGame()) {
            end()
        }
    }

    // Event Handlers
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (isPlayerInGame(player)) {
            eliminatePlayer(player, "died")
        }
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return

        if (isPlayerInGame(entity)) {
            if (event.cause == EntityDamageEvent.DamageCause.VOID) {
                event.isCancelled = true
                eliminatePlayer(entity, "fell into the void")
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!isPlayerInGame(player) || state != MinigameState.RUNNING) {
            return
        }

        if (player.location.y <= yThreshold) {
            eliminatePlayer(player, "fell off")
        }
    }

    // Helper methods
    protected fun isPlayerInGame(player: Player): Boolean {
        return players.contains(player.uniqueId) && alivePlayers.contains(player.uniqueId)
    }

    protected fun cleanupPlayers() {
        // Clean up ALL players who were ever in this game, not just online ones
        players.forEach { playerId ->
            getPlayerById(playerId)?.let { player ->
                if (player.isOnline) {
                    // Reset game mode
                    player.gameMode = GameMode.ADVENTURE

                    // Hide scoreboard FIRST before any other cleanup
                    try {
                        scoreboard.hideFrom(player)
                    } catch (e: Exception) {
                        // Fallback: Force reset to main scoreboard if hideFrom fails
                        player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
                    }

                    // Teleport player to world spawn
                    val worldSpawn = player.world.spawnLocation
                    player.teleport(worldSpawn)

                    // Play success sound
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f)
                }
            }
        }

        // Also clean up any remaining online players just in case
        getValidOnlinePlayers().forEach { player ->
            try {
                scoreboard.hideFrom(player)
            } catch (e: Exception) {
                player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
            }
        }
    }

    protected fun updateWaitingScoreboard() {
        scoreboard.setLine(1, Component.text("Players: ", NamedTextColor.YELLOW)
            .append(Component.text("$playerCount/$maxPlayers", NamedTextColor.WHITE)))
        scoreboard.setLine(2, Component.text("Status: ", NamedTextColor.YELLOW)
            .append(Component.text("Waiting for players", NamedTextColor.WHITE)))
        scoreboard.setLine(3, Component.text("Minimum: ", NamedTextColor.YELLOW)
            .append(Component.text("$minPlayers players", NamedTextColor.WHITE)))
    }

    // Abstract methods that subclasses must implement
    protected abstract val gamePrefix: Component
    protected abstract fun preparePlayer(player: Player)
    protected abstract fun onCountdown(secondsLeft: Int)
    protected abstract fun updateScoreboard()
    protected abstract fun broadcastGameStart()
    protected abstract fun handleGameEnd()
    protected abstract fun shouldEndGame(): Boolean
    protected abstract fun onPlayerEliminated(player: Player)

    // Optional overrides with default implementations
    protected open val yThreshold: Int
        get() = DEFAULT_Y_THRESHOLD
}
