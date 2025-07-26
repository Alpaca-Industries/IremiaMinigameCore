package org.alpacaindustries.iremiaminigamecore.minigame

/**
 * Represents the possible states of a minigame.
 */
enum class MinigameState {
    /**
     * Game is waiting for players to join
     */
    WAITING,

    /**
     * Game is in countdown phase before starting
     */
    COUNTDOWN,

    /**
     * Game is currently running
     */
    RUNNING,

    /**
     * Game has ended
     */
    ENDED
}
