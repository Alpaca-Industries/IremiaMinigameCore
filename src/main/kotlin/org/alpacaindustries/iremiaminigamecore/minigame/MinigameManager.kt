package org.alpacaindustries.iremiaminigamecore.minigame

import net.kyori.adventure.text.minimessage.MiniMessage
import org.alpacaindustries.iremiaminigamecore.IremiaMinigameCorePlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Central coordinator for all minigame instances and type registration
 * Handles lifecycle management and player-to-game mapping
 */
class MinigameManager(val plugin: IremiaMinigameCorePlugin) {

    private val gameFactories = ConcurrentHashMap<String, MinigameFactory>()
    private val activeGames = ConcurrentHashMap<String, Minigame>()
    private val playerGameMap = ConcurrentHashMap<UUID, String>()

    init {
        startHealthCheckScheduler()
    }

    /**
     * Register a minigame type for later instantiation.
     * Thread-safe for dynamic registration during runtime.
     */
    fun registerMinigame(id: String, factory: MinigameFactory) {
        val key = id.trim().lowercase()
        if (key.isEmpty()) {
            plugin.logger.warning("Cannot register minigame: ID is empty")
            return
        }
        gameFactories[key] = factory
        plugin.logger.info("Registered minigame type: $id")
    }

    /**
     * Create a new instance of a registered minigame type
     *
     * @param typeId     The type ID of minigame to create
     * @param instanceId A unique ID for this specific game instance
     * @return The created minigame or null if type wasn't found
     */
    fun createMinigame(typeId: String, instanceId: String): Minigame? {
        val typeKey = typeId.trim().lowercase()
        val instKey = instanceId.trim()

        if (typeKey.isEmpty() || instKey.isEmpty()) {
            plugin.logger.warning("Cannot create minigame: typeId or instanceId is empty")
            return null
        }

        val factory = gameFactories[typeKey]
        if (factory == null) {
            plugin.logger.warning("Unknown minigame type: $typeId")
            return null
        }

        val fullId = "$typeKey-$instKey"
        if (activeGames.containsKey(fullId)) {
            plugin.logger.warning("Minigame with ID $fullId already exists!")
            return null
        }

        return try {
            val minigame = factory.createMinigame(fullId, this)
            if (minigame == null) {
                plugin.logger.warning("Factory returned null minigame for type: $typeId")
                return null
            }

            activeGames[fullId] = minigame
            minigame.addEndListener { cleanupEndedGame(it) }
            minigame.initialize()
            minigame
        } catch (e: Exception) {
            plugin.logger.severe("Error creating minigame $fullId: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Check if a player is currently in any minigame.
     * @param player The player to check
     * @return true if the player is in a minigame
     */
    fun isPlayerInGame(player: Player): Boolean = playerGameMap.containsKey(player.uniqueId)

    /**
     * Add a player to a minigame, with rich error handling.
     *
     * @param player Player to add
     * @param gameId ID of the minigame to add player to
     * @return result of the join attempt
     */
    fun addPlayerToGame(player: Player, gameId: MinigameId): AddPlayerResult {
        val miniMessage = MiniMessage.miniMessage()
        if (isPlayerInGame(player)) {
            player.sendMessage(
                MinigameConfig.getMsgGameInProgress()
                    .append(miniMessage.deserialize(" <red>You are already in a game! Leave it first."))
            )
            return AddPlayerResult.AlreadyInGame
        }

        val game = activeGames[gameId]
        if (game == null) {
            player.sendMessage(
                MinigameConfig.getMsgGameInProgress()
                    .append(miniMessage.deserialize(" <red>That game doesn't exist!"))
            )
            return AddPlayerResult.GameNotFound
        }

        return if (game.addPlayer(player)) {
            playerGameMap[player.uniqueId] = gameId
            AddPlayerResult.Success
        } else {
            AddPlayerResult.Failed
        }
    }

    /**
     * Remove a player from their current minigame.
     *
     * @param player Player to remove
     * @return true if player was in a game and removed
     */
    fun removePlayerFromGame(player: Player): Boolean {
        val gameId = playerGameMap[player.uniqueId] ?: return false
        val game = activeGames[gameId]
        if (game == null) {
            plugin.logger.warning("Player ${player.name} was mapped to non-existent game $gameId. Cleaning up mapping.")
            playerGameMap.remove(player.uniqueId)
            return false
        }
        val removed = game.removePlayer(player)
        if (removed) {
            playerGameMap.remove(player.uniqueId)
        }
        return removed
    }

    /**
     * End and clean up a minigame.
     *
     * @param game The minigame to clean up
     */
    private fun cleanupEndedGame(game: Minigame) {
        val gameId = game.id
        val gamePlayers = game.players.toSet()

        gamePlayers.forEach { playerId ->
            playerGameMap.remove(playerId)
        }

        activeGames.remove(gameId)
        plugin.logger.info("Minigame $gameId has ended and been cleaned up")
    }

    fun startHealthCheckScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            activeGames.values.forEach { minigame ->
                minigame.performHealthCheck()
            }
        }, 20L * 30, 20L * 30) // Every 30 seconds
    }

    /**
     * Get all active minigames.
     *
     * @return Map of game IDs to minigame instances
     */
    fun getActiveGames(): Map<String, Minigame> = activeGames.toMap()

    /**
     * Get registered minigame types.
     *
     * @return Set of minigame type IDs
     */
    fun getMinigameTypes(): Set<String> = gameFactories.keys.toSet()

    /**
     * Get the minigame a player is currently in.
     *
     * @param player The player
     * @return The minigame or null if not in a game
     */
    fun getPlayerGame(player: Player): Minigame? {
        val gameId = playerGameMap[player.uniqueId] ?: return null
        return activeGames[gameId]
    }
}

/**
 * Type alias for minigame IDs for clarity.
 */
typealias MinigameId = String

/**
 * Result of attempting to add a player to a minigame.
 */
sealed class AddPlayerResult {
    object Success : AddPlayerResult()
    object AlreadyInGame : AddPlayerResult()
    object GameNotFound : AddPlayerResult()
    object Failed : AddPlayerResult()
}
