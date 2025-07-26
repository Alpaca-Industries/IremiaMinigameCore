package org.alpacaindustries.iremiaminigamecore.minigame

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Base abstract class for all minigames.
 * Provides the core functionality and lifecycle management for minigames.
 *
 * This class is thread-safe for all public methods unless otherwise noted.
 */
abstract class Minigame(
    val id: String,
    val displayName: String,
    val manager: MinigameManager
) : Listener {

    private val _players = Collections.synchronizedSet(mutableSetOf<UUID>())
    private val endListeners = Collections.synchronizedList(mutableListOf<Consumer<Minigame>>())
    private val playerCache = ConcurrentHashMap<UUID, Player>()
    private val isInitialized = AtomicBoolean(false)
    private val isDestroyed = AtomicBoolean(false)

    @Volatile
    var spawnPoint: Location? = null

    @Volatile
    var state: MinigameState = MinigameState.WAITING
        private set

    @Volatile
    var minPlayers: Int = MinigameConfig.getDefaultMinPlayers()
        set(value) {
            require(value >= 1) { "Minimum players must be at least 1" }
            require(value <= maxPlayers) { "Minimum players cannot exceed maximum players" }
            field = value
        }

    @Volatile
    var maxPlayers: Int = MinigameConfig.getDefaultMaxPlayers()
        set(value) {
            require(value >= 1) { "Maximum players must be at least 1" }
            require(value >= minPlayers) { "Maximum players cannot be less than minimum players" }
            field = value
        }

    @Volatile
    var isAllowJoinDuringGame: Boolean = false

    var shouldLoop: Boolean = false
        private set

    var loopDelayTicks: Long = 60L // Default: 3 seconds (20 ticks = 1 second)
        private set

    init {
        require(id.trim().isNotEmpty()) { "Minigame ID cannot be empty" }
        require(displayName.trim().isNotEmpty()) { "Display name cannot be empty" }

        // Validate initial configuration
        require(minPlayers <= maxPlayers) { "Minimum players ($minPlayers) cannot exceed maximum players ($maxPlayers)" }
    }

    /**
     * Get an unmodifiable view of the players
     */
    val players: Set<UUID>
        get() = Collections.unmodifiableSet(_players)

    /**
     * Get the current player count
     */
    val playerCount: Int
        get() = _players.size

    /**
     * Check if the minigame has been properly initialized
     */
    val initialized: Boolean
        get() = isInitialized.get()

    /**
     * Check if the minigame has been destroyed
     */
    val destroyed: Boolean
        get() = isDestroyed.get()

    /**
     * Initialize the minigame. Registers event listeners and sets initial state.
     * This method is idempotent - calling it multiple times has no additional effect.
     */
    open fun initialize() {
        if (!isInitialized.compareAndSet(false, true)) {
            manager.plugin.logger.warning("Minigame $id is already initialized")
            return
        }

        if (isDestroyed.get()) {
            throw IllegalStateException("Cannot initialize destroyed minigame $id")
        }

        setState(MinigameState.WAITING)
        manager.plugin.server.pluginManager.registerEvents(this, manager.plugin)
        manager.plugin.logger.fine("Minigame $id initialized successfully")
    }

    /**
     * Start the minigame. Only allowed from WAITING or COUNTDOWN state.
     */
    @Synchronized
    open fun start() {
        checkNotDestroyed()

        if (state != MinigameState.WAITING && state != MinigameState.COUNTDOWN) {
            manager.plugin.logger.warning("Cannot start minigame $id from state $state")
            return
        }

        if (_players.size < minPlayers) {
            broadcastMessage(Component.text(MinigameConfig.getMsgNotEnoughPlayers()))
            return
        }

        setState(MinigameState.RUNNING)
        onStart()
    }

    /**
     * End the minigame and clean up resources.
     */
    @Synchronized
    open fun end() {
        if (state == MinigameState.ENDED) {
            return
        }

        setState(MinigameState.ENDED)

        try {
            onEnd()
        } catch (e: Exception) {
            manager.plugin.logger.warning("Error during minigame end: ${e.message}")
            e.printStackTrace()
        }

        // Notify all end listeners with error handling
        val listenersSnapshot = endListeners.toList()
        listenersSnapshot.forEach { listener ->
            try {
                listener.accept(this)
            } catch (e: Exception) {
                manager.plugin.logger.warning("Error in end listener: ${e.message}")
                e.printStackTrace()
            }
        }

        // Clear caches and collections to prevent memory leaks
        cleanup()

        // Loop logic: schedule restart if enabled
        if (shouldLoop && !isDestroyed.get()) {
            scheduleRestart()
        }
    }

    /**
     * Destroy the minigame and free all resources.
     * This is irreversible - the minigame cannot be used after destruction.
     */
    fun destroy() {
        if (!isDestroyed.compareAndSet(false, true)) {
            return // Already destroyed
        }

        try {
            // End the game if it's still running
            if (state != MinigameState.ENDED) {
                end()
            }

            // Unregister event listeners
            HandlerList.unregisterAll(this)

            // Final cleanup
            cleanup()

            manager.plugin.logger.info("Minigame $id destroyed successfully")
        } catch (e: Exception) {
            manager.plugin.logger.severe("Error destroying minigame $id: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Internal cleanup method
     */
    private fun cleanup() {
        playerCache.clear()
        endListeners.clear()
        // _players.clear()
    }

    /**
     * Schedule a restart for looping minigames
     */
    private fun scheduleRestart() {
        manager.plugin.server.scheduler.runTaskLater(
            manager.plugin,
            Runnable {
                if (!isDestroyed.get()) {
                    restartMinigame()
                }
            },
            loopDelayTicks
        )
    }

    /**
     * Restart the minigame for looping.
     * This resets state and calls initialize/start.
     */
    private fun restartMinigame() {
        try {
            if (isDestroyed.get()) {
                manager.plugin.logger.warning("Cannot restart destroyed minigame $id")
                return
            }

            // Reset initialization state for restart
            isInitialized.set(false)
            initialize()
            start()
        } catch (e: Exception) {
            manager.plugin.logger.severe("Error restarting minigame $id: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Add a player to the minigame.
     *
     * @param player Player to add
     * @return true if player was successfully added
     */
    open fun addPlayer(player: Player): Boolean {
        checkNotDestroyed()

        if (!player.isOnline) {
            return false
        }

        synchronized(_players) {
            if (_players.contains(player.uniqueId)) {
                return false
            }

            if (_players.size >= maxPlayers) {
                player.sendMessage(MinigameConfig.getMsgGameFull())
                return false
            }

            if (state == MinigameState.RUNNING && !isAllowJoinDuringGame) {
                player.sendMessage(MinigameConfig.getMsgGameInProgress())
                return false
            }

            _players.add(player.uniqueId)
            playerCache[player.uniqueId] = player
            spawnPoint?.let {
                try {
                    player.teleport(it)
                } catch (e: Exception) {
                    manager.plugin.logger.warning("Failed to teleport ${player.name} to spawn point: ${e.message}")
                }
            }

            try {
                onPlayerJoin(player)
            } catch (e: Exception) {
                manager.plugin.logger.warning("Error in onPlayerJoin for ${player.name}: ${e.message}")
                e.printStackTrace()
            }

            if (state == MinigameState.WAITING && _players.size >= minPlayers && shouldAutoStart()) {
                startCountdown()
            }
            return true
        }
    }

    /**
     * Remove a player from the minigame.
     *
     * @param player Player to remove
     */
    open fun removePlayer(player: Player) {
        synchronized(_players) {
            if (!_players.contains(player.uniqueId)) {
                return
            }

            _players.remove(player.uniqueId)

            try {
                onPlayerCleanup(player)
                onPlayerLeave(player)
            } catch (e: Exception) {
                manager.plugin.logger.warning("Error during player leave for ${player.name}: ${e.message}")
                e.printStackTrace()
            }

            // Check game state after player removal
            if (!isDestroyed.get()) {
                checkGameStateAfterPlayerLeave()
            }
        }
    }

    /**
     * Check game state after a player leaves and take appropriate action
     */
    private fun checkGameStateAfterPlayerLeave() {
        when (state) {
            MinigameState.RUNNING -> {
                if (_players.size < minPlayers) {
                    broadcastMessage(Component.text(MinigameConfig.getMsgPlayersRemaining()))
                    end()
                }
            }
            MinigameState.COUNTDOWN -> {
                if (_players.size < minPlayers) {
                    setState(MinigameState.WAITING)
                    broadcastMessage(Component.text(MinigameConfig.getMsgCountdownCancelled()))
                }
            }
            else -> { /* No action needed for other states */ }
        }
    }

    /**
     * Send a message to all players in the minigame.
     *
     * @param message Message to send
     */
    fun broadcastMessage(message: Component) {
        val playersSnapshot = _players.toSet()
        playersSnapshot.forEach { uuid ->
            try {
                getPlayerById(uuid)?.sendMessage(message)
            } catch (e: Exception) {
                manager.plugin.logger.fine("Failed to send message to player $uuid: ${e.message}")
            }
        }
    }

    /**
     * Start the countdown to begin the game.
     */
    open fun startCountdown() {
        checkNotDestroyed()
        setState(MinigameState.COUNTDOWN)
        onCountdownStart()
    }

    /**
     * Add a listener that will be called when this minigame ends.
     *
     * @param listener Consumer that takes the ended minigame
     */
    fun addEndListener(listener: Consumer<Minigame>) {
        checkNotDestroyed()
        endListeners.add(listener)
    }

    /**
     * Remove an end listener.
     *
     * @param listener Consumer to remove
     * @return true if the listener was removed
     */
    fun removeEndListener(listener: Consumer<Minigame>): Boolean {
        return endListeners.remove(listener)
    }

    /**
     * Check if the minigame has been destroyed and throw if it has
     */
    private fun checkNotDestroyed() {
        if (isDestroyed.get()) {
            throw IllegalStateException("Minigame $id has been destroyed")
        }
    }

    /**
     * Set whether this minigame should automatically loop (restart) after ending.
     *
     * @param shouldLoop true to enable looping
     */
    fun setShouldLoop(shouldLoop: Boolean) {
        checkNotDestroyed()
        this.shouldLoop = shouldLoop
    }

    /**
     * Set the delay (in ticks) before restarting the minigame if looping is enabled.
     *
     * @param delayTicks Delay in server ticks (20 ticks = 1 second)
     */
    fun setLoopDelayTicks(delayTicks: Long) {
        checkNotDestroyed()
        require(delayTicks > 0) { "Loop delay must be positive" }
        this.loopDelayTicks = delayTicks
    }

    /**
     * Get a player by UUID with caching for performance.
     * This method is thread-safe and includes automatic cache cleanup.
     *
     * @param uuid Player UUID
     * @return Player instance or null if not online
     */
    protected fun getPlayerById(uuid: UUID): Player? {
        // Fast path: check cache first
        val cached = playerCache[uuid]
        if (cached?.isOnline == true) {
            return cached
        }

        // Slow path: query server and update cache
        val player = manager.plugin.server.getPlayer(uuid)
        return if (player?.isOnline == true) {
            playerCache[uuid] = player
            player
        } else {
            // Clean up stale cache entry
            playerCache.remove(uuid)
            null
        }
    }

    /**
     * Validate if a player is in a valid state for this minigame.
     *
     * @param player Player to validate
     * @return true if player is valid and can participate
     */
    protected fun isPlayerValid(player: Player?): Boolean {
        return player?.isOnline == true && _players.contains(player.uniqueId)
    }

    /**
     * Clean up expired entries from player cache.
     * Should be called periodically to prevent memory leaks.
     * This method is optimized to minimize performance impact.
     */
    fun cleanupPlayerCache() {
        if (playerCache.isEmpty()) return

        val toRemove = mutableListOf<UUID>()

        playerCache.forEach { (uuid, player) ->
            if (!player.isOnline || !_players.contains(uuid)) {
                toRemove.add(uuid)
            }
        }

        toRemove.forEach { uuid ->
            playerCache.remove(uuid)
            manager.plugin.logger.fine("Cleaned up cached player: $uuid from minigame $id")
        }
    }

    /**
     * Refresh all online players and remove offline ones.
     * This method ensures data consistency between player list and actual online status.
     */
    protected fun refreshAllOnlinePlayers() {
        val toRemove = mutableSetOf<UUID>()

        _players.forEach { uuid ->
            val player = getPlayerById(uuid)
            if (player?.isOnline != true) {
                toRemove.add(uuid)
            } else {
                playerCache[uuid] = player
            }
        }

        // Remove offline players
        toRemove.forEach { uuid ->
            _players.remove(uuid)
            playerCache.remove(uuid)
        }

        if (toRemove.isNotEmpty()) {
            manager.plugin.logger.fine("Removed ${toRemove.size} offline players from minigame $id")
        }
    }

    /**
     * Handle state changes with proper logging and validation.
     */
    protected open fun onStateChange(oldState: MinigameState, newState: MinigameState) {
        manager.plugin.logger.fine("Minigame $id state changed from $oldState to $newState")

        // Validate state transitions
        when (oldState to newState) {
            MinigameState.WAITING to MinigameState.COUNTDOWN,
            MinigameState.COUNTDOWN to MinigameState.RUNNING,
            MinigameState.COUNTDOWN to MinigameState.WAITING,
            MinigameState.RUNNING to MinigameState.ENDED -> { /* Valid transitions */ }
            MinigameState.WAITING to MinigameState.RUNNING -> { /* Direct start - valid */ }
            else -> {
                if (newState != MinigameState.ENDED) { // Ending is always valid
                    manager.plugin.logger.warning("Unusual state transition in minigame $id: $oldState -> $newState")
                }
            }
        }
    }

    /**
     * Get all valid online players with automatic cleanup.
     * This method is thread-safe and performs inline cleanup of offline players.
     */
    fun getValidOnlinePlayers(): List<Player> {
        val validPlayers = mutableListOf<Player>()
        val toRemove = mutableSetOf<UUID>()

        _players.forEach { uuid ->
            val player = getPlayerById(uuid)
            if (player?.isOnline == true) {
                validPlayers.add(player)
            } else {
                toRemove.add(uuid)
            }
        }

        // Clean up offline players
        toRemove.forEach { uuid ->
            _players.remove(uuid)
            playerCache.remove(uuid)
        }

        return validPlayers
    }

    /**
     * Validate the current game state and take corrective action if needed.
     * This includes checking player counts and online status.
     */
    fun validateGameState() {
        if (isDestroyed.get()) return

        refreshAllOnlinePlayers()

        if (state == MinigameState.RUNNING && _players.size < minPlayers) {
            manager.plugin.logger.warning(
                "Minigame $id has too few players (${_players.size}/$minPlayers), ending game"
            )
            end()
        }
    }

    /**
     * Perform a comprehensive health check of the minigame.
     * This should be called periodically by the manager.
     */
    fun performHealthCheck() {
        if (isDestroyed.get()) return

        try {
            validateGameState()
            cleanupPlayerCache()

            // Log health status for debugging
            manager.plugin.logger.fine(
                "Health check for minigame $id: State=$state, Players=${_players.size}/$maxPlayers, Cache=${playerCache.size}"
            )
        } catch (e: Exception) {
            manager.plugin.logger.warning("Error during health check for minigame $id: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Set the minigame state and trigger state change hook.
     * This method ensures proper state transition validation.
     */
    protected fun setState(newState: MinigameState) {
        val oldState = this.state
        this.state = newState
        onStateChange(oldState, newState)
    }

    /**
     * Get detailed status information about this minigame.
     * Useful for debugging and monitoring.
     */
    fun getStatusInfo(): String {
        return buildString {
            append("Minigame[$id]: ")
            append("State=$state, ")
            append("Players=${_players.size}/$maxPlayers, ")
            append("Initialized=${isInitialized.get()}, ")
            append("Destroyed=${isDestroyed.get()}, ")
            append("Loop=$shouldLoop")
        }
    }

    // Abstract methods that minigames must implement

    /**
     * Called when the minigame starts.
     * Override this method and call super.onStart() to maintain base functionality.
     */
    protected open fun onStart() {
        manager.plugin.logger.info("Minigame $id started with $playerCount players")
    }

    /**
     * Called when the minigame ends.
     * Override this method and call super.onEnd() to maintain base functionality.
     */
    protected open fun onEnd() {
        cleanupPlayerCache()
        refreshAllOnlinePlayers()
        manager.plugin.logger.info("Minigame $id ended")
    }

    /**
     * Called when a player joins the minigame.
     * Override this method and call super.onPlayerJoin(player) to maintain base functionality.
     *
     * @param player Player who joined
     */
    protected open fun onPlayerJoin(player: Player) {
        playerCache[player.uniqueId] = player
        manager.plugin.logger.info("Player ${player.name} joined minigame $id")
    }

    /**
     * Called when a player leaves the minigame.
     * Override this method and call super.onPlayerLeave(player) to maintain base functionality.
     *
     * @param player Player who left
     */
    protected open fun onPlayerLeave(player: Player) {
        onPlayerCleanup(player)
        manager.plugin.logger.info("Player ${player.name} left minigame $id")
    }

    /**
     * Called to clean up player-specific resources when they leave.
     * Override this method to add custom cleanup logic.
     *
     * @param player Player being cleaned up
     */
    protected open fun onPlayerCleanup(player: Player) {
        playerCache.remove(player.uniqueId)
    }

    /**
     * Called when the countdown starts.
     * Override this method and call super.onCountdownStart() to maintain base functionality.
     */
    protected open fun onCountdownStart() {
        manager.plugin.logger.info("Countdown started for minigame $id")
    }


    // Event handlers - import missing dependencies

    @EventHandler
    fun onPlayerQuitServer(event: org.bukkit.event.player.PlayerQuitEvent) {
        val player = event.player
        if (_players.contains(player.uniqueId)) {
            manager.plugin.logger.info(
                "Player ${player.name} quit server while in minigame $id"
            )

            // Schedule removal on next tick to avoid concurrent modification
            org.bukkit.Bukkit.getScheduler().runTask(manager.plugin, Runnable {
                removePlayer(player)
            })
        }
    }

    /**
     * Determines if players should be automatically removed when they quit the server.
     * Override this method to customize auto-removal behavior.
     *
     * @param player Player who quit
     * @return true if player should be automatically removed
     */
    protected open fun shouldAutoRemoveOnQuit(player: Player): Boolean = true

    /**
     * Determines if the minigame should automatically start when enough players join.
     * Override this method to customize auto-start behavior.
     *
     * @return true if minigame should auto-start
     */
    protected open fun shouldAutoStart(): Boolean = true
}
