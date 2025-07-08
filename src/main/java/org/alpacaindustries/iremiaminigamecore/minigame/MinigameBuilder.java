package org.alpacaindustries.iremiaminigamecore.minigame;

import org.bukkit.Location;

/**
 * Builder pattern for configuring minigames
 */
public class MinigameBuilder {
    private int minPlayers = 1;
    private int maxPlayers = 16;
    private boolean allowJoinDuringGame = false;
    private Location spawnPoint;

    public MinigameBuilder minPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
        return this;
    }

    public MinigameBuilder maxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        return this;
    }

    public MinigameBuilder allowJoinDuringGame(boolean allow) {
        this.allowJoinDuringGame = allow;
        return this;
    }

    public MinigameBuilder spawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
        return this;
    }

    public void applyTo(Minigame minigame) {
        minigame.setMinPlayers(minPlayers);
        minigame.setMaxPlayers(maxPlayers);
        minigame.setAllowJoinDuringGame(allowJoinDuringGame);
        if (spawnPoint != null) {
            minigame.setSpawnPoint(spawnPoint);
        }
    }
}
