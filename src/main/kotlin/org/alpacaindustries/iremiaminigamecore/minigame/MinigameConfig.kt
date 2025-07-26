package org.alpacaindustries.iremiaminigamecore.minigame

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.Plugin

/**
 * Configuration manager for minigames that reads from plugin config
 */
object MinigameConfig {

    private var plugin: Plugin? = null
    private var config: FileConfiguration? = null

    /**
     * Initialize the config with the plugin instance.
     *
     * @param pluginInstance the plugin instance (must not be null)
     * @throws IllegalArgumentException if pluginInstance is null
     */
    @JvmStatic
    fun initialize(pluginInstance: Plugin) {
        plugin = pluginInstance
        config = pluginInstance.config
        pluginInstance.saveDefaultConfig()
        pluginInstance.reloadConfig()
        config = pluginInstance.config
    }

    /**
     * Get the default minimum number of players for a minigame.
     *
     * @return the default minimum number of players
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getDefaultMinPlayers(): Int {
        checkInitialized()
        return config!!.getInt("minigames.default-min-players", 1)
    }

    /**
     * Get the default maximum number of players for a minigame.
     *
     * @return the default maximum number of players
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getDefaultMaxPlayers(): Int {
        checkInitialized()
        return config!!.getInt("minigames.default-max-players", 16)
    }

    /**
     * Get the default countdown time in seconds.
     *
     * @return the default countdown time in seconds
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getDefaultCountdownSeconds(): Int {
        checkInitialized()
        return config!!.getInt("minigames.default-countdown-seconds", 10)
    }

    /**
     * Get the default game duration in seconds.
     *
     * @return the default game duration in seconds
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getDefaultGameDurationSeconds(): Int {
        checkInitialized()
        return config!!.getInt("minigames.default-game-duration-seconds", 300)
    }

    /**
     * Get the player cache cleanup interval in seconds.
     *
     * @return the player cache cleanup interval
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getPlayerCacheCleanupInterval(): Int {
        checkInitialized()
        return config!!.getInt("minigames.player-cache-cleanup-interval", 30)
    }

    /**
     * Get the maximum number of cached players.
     *
     * @return the maximum number of cached players
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMaxCachedPlayers(): Int {
        checkInitialized()
        return config!!.getInt("minigames.max-cached-players", 100)
    }

    /**
     * Get the message for a full game.
     *
     * @return the message for a full game
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgGameFull(): String {
        checkInitialized()
        return config!!.getString("minigames.messages.game-full") ?: "This game is full!"
    }

    /**
     * Get the message for a game in progress.
     *
     * @return the message for a game in progress
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgGameInProgress(): String {
        checkInitialized()
        return config!!.getString("minigames.messages.game-in-progress") ?: "This game is already in progress!"
    }

    /**
     * Get the message for not enough players.
     *
     * @return the message for not enough players
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgNotEnoughPlayers(): String {
        checkInitialized()
        return config!!.getString("minigames.messages.not-enough-players") ?: "Not enough players to start!"
    }

    /**
     * Get the message for not enough players remaining.
     *
     * @return the message for not enough players remaining
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgPlayersRemaining(): String {
        checkInitialized()
        return config!!.getString("minigames.messages.players-remaining") ?: "Not enough players remaining! Ending game."
    }

    /**
     * Get the message for countdown cancelled.
     *
     * @return the message for countdown cancelled
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgCountdownCancelled(): String {
        checkInitialized()
        return config!!.getString("minigames.messages.countdown-cancelled") ?: "Countdown cancelled - not enough players!"
    }

    /**
     * Reload configuration from file.
     */
    @JvmStatic
    fun reload() {
        plugin?.let {
            it.reloadConfig()
            config = it.config
        }
    }

    /**
     * Throws if config is not initialized.
     */
    private fun checkInitialized() {
        if (config == null) {
            throw IllegalStateException("MinigameConfig is not initialized. Call MinigameConfig.initialize(plugin) first.")
        }
    }

    // Legacy constants for backward compatibility (deprecated)
    @Deprecated("Use getDefaultMinPlayers() instead")
    const val DEFAULT_MIN_PLAYERS = 1

    @Deprecated("Use getDefaultMaxPlayers() instead")
    const val DEFAULT_MAX_PLAYERS = 16

    @Deprecated("Use getDefaultCountdownSeconds() instead")
    const val DEFAULT_COUNTDOWN_SECONDS = 10

    @Deprecated("Use getDefaultGameDurationSeconds() instead")
    const val DEFAULT_GAME_DURATION_SECONDS = 300

    @Deprecated("Use getPlayerCacheCleanupInterval() instead")
    const val PLAYER_CACHE_CLEANUP_INTERVAL = 30

    @Deprecated("Use getMaxCachedPlayers() instead")
    const val MAX_CACHED_PLAYERS = 100

    @Deprecated("Use getMsgGameFull() instead")
    const val MSG_GAME_FULL = "This game is full!"

    @Deprecated("Use getMsgGameInProgress() instead")
    const val MSG_GAME_IN_PROGRESS = "This game is already in progress!"

    @Deprecated("Use getMsgNotEnoughPlayers() instead")
    const val MSG_NOT_ENOUGH_PLAYERS = "Not enough players to start!"

    @Deprecated("Use getMsgPlayersRemaining() instead")
    const val MSG_PLAYERS_REMAINING = "Not enough players remaining! Ending game."

    @Deprecated("Use getMsgCountdownCancelled() instead")
    const val MSG_COUNTDOWN_CANCELLED = "Countdown cancelled - not enough players!"
}
