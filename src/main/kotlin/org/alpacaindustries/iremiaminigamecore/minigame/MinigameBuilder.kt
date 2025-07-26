package org.alpacaindustries.iremiaminigamecore.minigame

import org.bukkit.Location

/**
 * Builder pattern for configuring minigames
 */
class MinigameBuilder {
    private var minPlayers: Int = 1
    private var maxPlayers: Int = 16
    private var allowJoinDuringGame: Boolean = false
    private var spawnPoint: Location? = null

    /**
     * Set the minimum number of players.
     */
    fun minPlayers(minPlayers: Int): MinigameBuilder = apply {
        this.minPlayers = minPlayers
    }

    /**
     * Set the maximum number of players.
     */
    fun maxPlayers(maxPlayers: Int): MinigameBuilder = apply {
        this.maxPlayers = maxPlayers
    }

    /**
     * Set whether players can join during the game.
     */
    fun allowJoinDuringGame(allow: Boolean): MinigameBuilder = apply {
        this.allowJoinDuringGame = allow
    }

    /**
     * Set the spawn point for the minigame.
     */
    fun spawnPoint(spawnPoint: Location?): MinigameBuilder = apply {
        this.spawnPoint = spawnPoint
    }

    /**
     * Apply the builder's configuration to a Kotlin MinigameKt instance.
     */
    fun applyTo(minigame: Minigame) {
        minigame.minPlayers = minPlayers
        minigame.maxPlayers = maxPlayers
        minigame.isAllowJoinDuringGame = allowJoinDuringGame
        minigame.spawnPoint = spawnPoint
    }
}
