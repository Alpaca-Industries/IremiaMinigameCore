package org.alpacaindustries.iremiaminigamecore.minigame

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.Plugin

object MinigameConfig {

    private var plugin: Plugin? = null
    private var config: FileConfiguration? = null
    private val miniMessage = MiniMessage.miniMessage()
    private val PREFIX = "<gray>[<gold><b>Minigame</b></gold>] </gray>"

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

    private fun getMessage(path: String, fallback: String): Component {
        checkInitialized()
        val raw = config!!.getString(path) ?: fallback
        return miniMessage.deserialize(PREFIX + raw)
    }

    /**
     * Get the message for a full game.
     *
     * @return the message for a full game
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgGameFull(): Component =
        getMessage("minigames.messages.game-full", "<red>This game is full!")

    /**
     * Get the message for a game in progress.
     *
     * @return the message for a game in progress
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgGameInProgress(): Component = getMessage(
        "minigames.messages.game-in-progress",
        "<yellow>This game is already in progress!"
    )

    /**
     * Get the message for not enough players.
     *
     * @return the message for not enough players
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgNotEnoughPlayers(): Component =
        getMessage("minigames.messages.not-enough-players", "<yellow>Not enough players to start!")

    /**
     * Get the message for not enough players remaining.
     *
     * @return the message for not enough players remaining
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgPlayersRemaining(): Component = getMessage(
        "minigames.messages.players-remaining",
        "<red>Not enough players remaining! Ending game."
    )

    /**
     * Get the message for countdown cancelled.
     *
     * @return the message for countdown cancelled
     * @throws IllegalStateException if config is not initialized
     */
    @JvmStatic
    fun getMsgCountdownCancelled(): Component = getMessage(
        "minigames.messages.countdown-cancelled",
        "<yellow>Countdown cancelled - not enough players!"
    )

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
}
