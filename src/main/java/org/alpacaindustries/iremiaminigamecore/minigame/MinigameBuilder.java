package org.alpacaindustries.iremiaminigamecore.minigame;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Builder pattern for configuring minigames
 */
public class MinigameBuilder {
    private int minPlayers = 1;
    private int maxPlayers = 16;
    private boolean allowJoinDuringGame = false;
    private Optional<Location> spawnPoint = Optional.empty();

    /**
     * Set the minimum number of players.
     */
    public @NotNull MinigameBuilder minPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
        return this;
    }

    /**
     * Set the maximum number of players.
     */
    public @NotNull MinigameBuilder maxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        return this;
    }

    /**
     * Set whether players can join during the game.
     */
    public @NotNull MinigameBuilder allowJoinDuringGame(boolean allow) {
        this.allowJoinDuringGame = allow;
        return this;
    }

    /**
     * Set the spawn point for the minigame.
     */
    public @NotNull MinigameBuilder spawnPoint(@Nullable Location spawnPoint) {
        this.spawnPoint = Optional.ofNullable(spawnPoint);
        return this;
    }

    /**
     * Apply the builder's configuration to a Minigame instance.
     */
    public void applyTo(@NotNull Minigame minigame) {
        minigame.setMinPlayers(minPlayers);
        minigame.setMaxPlayers(maxPlayers);
        minigame.setAllowJoinDuringGame(allowJoinDuringGame);
        minigame.setSpawnPoint(spawnPoint.orElse(null));
    }
}
