package org.alpacaindustries.iremiaminigamecore.minigame;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Configuration manager for minigames that reads from plugin config
 */
public final class MinigameConfig {

  private static Plugin plugin;
  private static FileConfiguration config;

  /**
   * Initialize the config with the plugin instance.
   *
   * @param pluginInstance the plugin instance (must not be null)
   * @throws IllegalArgumentException if pluginInstance is null
   */
  public static void initialize(@NotNull Plugin pluginInstance) {
    Objects.requireNonNull(pluginInstance, "Plugin instance cannot be null");
    plugin = pluginInstance;
    config = plugin.getConfig();
    plugin.saveDefaultConfig();
    plugin.reloadConfig();
    config = plugin.getConfig();
  }

  /**
   * Get the default minimum number of players for a minigame.
   *
   * @return the default minimum number of players
   * @throws IllegalStateException if config is not initialized
   */
  public static int getDefaultMinPlayers() {
    checkInitialized();
    return config.getInt("minigames.default-min-players", 1);
  }

  /**
   * Get the default maximum number of players for a minigame.
   *
   * @return the default maximum number of players
   * @throws IllegalStateException if config is not initialized
   */
  public static int getDefaultMaxPlayers() {
    checkInitialized();
    return config.getInt("minigames.default-max-players", 16);
  }

  /**
   * Get the default countdown time in seconds.
   *
   * @return the default countdown time in seconds
   * @throws IllegalStateException if config is not initialized
   */
  public static int getDefaultCountdownSeconds() {
    checkInitialized();
    return config.getInt("minigames.default-countdown-seconds", 10);
  }

  /**
   * Get the default game duration in seconds.
   *
   * @return the default game duration in seconds
   * @throws IllegalStateException if config is not initialized
   */
  public static int getDefaultGameDurationSeconds() {
    checkInitialized();
    return config.getInt("minigames.default-game-duration-seconds", 300);
  }

  /**
   * Get the player cache cleanup interval in seconds.
   *
   * @return the player cache cleanup interval
   * @throws IllegalStateException if config is not initialized
   */
  public static int getPlayerCacheCleanupInterval() {
    checkInitialized();
    return config.getInt("minigames.player-cache-cleanup-interval", 30);
  }

  /**
   * Get the maximum number of cached players.
   *
   * @return the maximum number of cached players
   * @throws IllegalStateException if config is not initialized
   */
  public static int getMaxCachedPlayers() {
    checkInitialized();
    return config.getInt("minigames.max-cached-players", 100);
  }

  /**
   * Get the message for a full game.
   *
   * @return the message for a full game
   * @throws IllegalStateException if config is not initialized
   */
  public static @NotNull String getMsgGameFull() {
    checkInitialized();
    return Objects.requireNonNullElse(config.getString("minigames.messages.game-full"), "This game is full!");
  }

  /**
   * Get the message for a game in progress.
   *
   * @return the message for a game in progress
   * @throws IllegalStateException if config is not initialized
   */
  public static @NotNull String getMsgGameInProgress() {
    checkInitialized();
    return Objects.requireNonNullElse(config.getString("minigames.messages.game-in-progress"), "This game is already in progress!");
  }

  /**
   * Get the message for not enough players.
   *
   * @return the message for not enough players
   * @throws IllegalStateException if config is not initialized
   */
  public static @NotNull String getMsgNotEnoughPlayers() {
    checkInitialized();
    return Objects.requireNonNullElse(config.getString("minigames.messages.not-enough-players"), "Not enough players to start!");
  }

  /**
   * Get the message for not enough players remaining.
   *
   * @return the message for not enough players remaining
   * @throws IllegalStateException if config is not initialized
   */
  public static @NotNull String getMsgPlayersRemaining() {
    checkInitialized();
    return Objects.requireNonNullElse(config.getString("minigames.messages.players-remaining"), "Not enough players remaining! Ending game.");
  }

  /**
   * Get the message for countdown cancelled.
   *
   * @return the message for countdown cancelled
   * @throws IllegalStateException if config is not initialized
   */
  public static @NotNull String getMsgCountdownCancelled() {
    checkInitialized();
    return Objects.requireNonNullElse(config.getString("minigames.messages.countdown-cancelled"), "Countdown cancelled - not enough players!");
  }

  /**
   * Reload configuration from file.
   */
  public static void reload() {
    if (plugin != null) {
      plugin.reloadConfig();
      config = plugin.getConfig();
    }
  }

  /**
   * Throws if config is not initialized.
   */
  private static void checkInitialized() {
    if (config == null) {
      throw new IllegalStateException("MinigameConfig is not initialized. Call MinigameConfig.initialize(plugin) first.");
    }
  }

  // Legacy constants for backward compatibility (deprecated)
  @Deprecated
  public static final int DEFAULT_MIN_PLAYERS = 1;
  @Deprecated
  public static final int DEFAULT_MAX_PLAYERS = 16;
  @Deprecated
  public static final int DEFAULT_COUNTDOWN_SECONDS = 10;
  @Deprecated
  public static final int DEFAULT_GAME_DURATION_SECONDS = 300;
  @Deprecated
  public static final int PLAYER_CACHE_CLEANUP_INTERVAL = 30;
  @Deprecated
  public static final int MAX_CACHED_PLAYERS = 100;
  @Deprecated
  public static final String MSG_GAME_FULL = "This game is full!";
  @Deprecated
  public static final String MSG_GAME_IN_PROGRESS = "This game is already in progress!";
  @Deprecated
  public static final String MSG_NOT_ENOUGH_PLAYERS = "Not enough players to start!";
  @Deprecated
  public static final String MSG_PLAYERS_REMAINING = "Not enough players remaining! Ending game.";
  @Deprecated
  public static final String MSG_COUNTDOWN_CANCELLED = "Countdown cancelled - not enough players!";

  private MinigameConfig() {
    // Utility class
  }
}
