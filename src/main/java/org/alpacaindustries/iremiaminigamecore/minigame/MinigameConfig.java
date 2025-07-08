package org.alpacaindustries.iremiaminigamecore.minigame;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Configuration manager for minigames that reads from plugin config
 */
public final class MinigameConfig {

  private static Plugin plugin;
  private static FileConfiguration config;

  /**
   * Initialize the config with the plugin instance
   */
  public static void initialize(Plugin pluginInstance) {
    if (pluginInstance == null) {
      throw new IllegalArgumentException("Plugin instance cannot be null");
    }

    plugin = pluginInstance;
    config = plugin.getConfig();

    // Save default config if it doesn't exist
    plugin.saveDefaultConfig();

    // Reload to ensure we have latest values
    plugin.reloadConfig();
    config = plugin.getConfig();
  }

  // Default player limits
  public static int getDefaultMinPlayers() {
    return config.getInt("minigames.default-min-players", 1);
  }

  public static int getDefaultMaxPlayers() {
    return config.getInt("minigames.default-max-players", 16);
  }

  // Timing constants
  public static int getDefaultCountdownSeconds() {
    return config.getInt("minigames.default-countdown-seconds", 10);
  }

  public static int getDefaultGameDurationSeconds() {
    return config.getInt("minigames.default-game-duration-seconds", 300);
  }

  // Performance tuning
  public static int getPlayerCacheCleanupInterval() {
    return config.getInt("minigames.player-cache-cleanup-interval", 30);
  }

  public static int getMaxCachedPlayers() {
    return config.getInt("minigames.max-cached-players", 100);
  }

  // Error messages
  public static String getMsgGameFull() {
    return config.getString("minigames.messages.game-full", "This game is full!");
  }

  public static String getMsgGameInProgress() {
    return config.getString("minigames.messages.game-in-progress", "This game is already in progress!");
  }

  public static String getMsgNotEnoughPlayers() {
    return config.getString("minigames.messages.not-enough-players", "Not enough players to start!");
  }

  public static String getMsgPlayersRemaining() {
    return config.getString("minigames.messages.players-remaining", "Not enough players remaining! Ending game.");
  }

  public static String getMsgCountdownCancelled() {
    return config.getString("minigames.messages.countdown-cancelled", "Countdown cancelled - not enough players!");
  }

  /**
   * Reload configuration from file
   */
  public static void reload() {
    if (plugin != null) {
      plugin.reloadConfig();
      config = plugin.getConfig();
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
