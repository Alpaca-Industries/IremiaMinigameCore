package org.alpacaindustries.iremiaminigamecore.minigame

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
     * Add a player to a minigame.
     *
     * @param player Player to add
     * @param gameId ID of the minigame to add player to
     * @return true if successfully added
     */
    fun addPlayerToGame(player: Player, gameId: String): Boolean {
        if (playerGameMap.containsKey(player.uniqueId)) {
            player.sendMessage("You are already in a game! Leave it first.")
            return false
        }

        val game = activeGames[gameId]
        if (game == null) {
            player.sendMessage("That game doesn't exist!")
            return false
        }

        return if (game.addPlayer(player)) {
            playerGameMap[player.uniqueId] = gameId
            true
        } else {
            false
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
            playerGameMap.remove(player.uniqueId)
            return false
        }

        game.removePlayer(player)
        playerGameMap.remove(player.uniqueId)
        return true
    }

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
}
