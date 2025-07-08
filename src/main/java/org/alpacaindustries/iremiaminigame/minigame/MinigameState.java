package org.alpacaindustries.iremiaminigame.minigame;

/**
 * Represents the possible states of a minigame.
 */
public enum MinigameState {
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
