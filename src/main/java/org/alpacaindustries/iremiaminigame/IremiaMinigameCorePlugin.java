package org.alpacaindustries.iremiaminigame;

import org.alpacaindustries.iremiaminigame.api.IremiaMinigameAPI;
import org.alpacaindustries.iremiaminigame.api.impl.IremiaMinigameAPIImpl;
import org.alpacaindustries.iremiaminigame.command.MinigameCommand;
import org.alpacaindustries.iremiaminigame.minigame.MinigameConfig;
import org.alpacaindustries.iremiaminigame.minigame.MinigameManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * IremiaMinigameCore - Core minigame management system
 * Provides the foundation for creating and managing minigames
 *
 * This plugin serves as the core API and framework that other minigame
 * plugins can depend on to create their specific game implementations.
 */
public class IremiaMinigameCorePlugin extends JavaPlugin implements Listener {

  private MinigameManager minigameManager;
  private IremiaMinigameAPI api;

  @Override
  public void onEnable() {
    getLogger().info("Starting IremiaMinigameCore...");

    // Initialize configuration
    MinigameConfig.initialize(this);
    getLogger().info("Configuration initialized");

    // Initialize the minigame manager
    minigameManager = new MinigameManager(this);
    getLogger().info("MinigameManager initialized");

    // Initialize the API implementation
    api = new IremiaMinigameAPIImpl(minigameManager, getLogger());
    getLogger().info("API implementation initialized");

    // Register commands
    new MinigameCommand(this);
    getLogger().info("Commands registered");

    // Register event listeners
    getServer().getPluginManager().registerEvents(this, this);
    getLogger().info("Event listeners registered");

    getLogger().info("========================================");
    getLogger().info("  IremiaMinigameCore has been enabled!");
    getLogger().info("  Version: " + getPluginMeta().getVersion());
    getLogger().info("  API ready for external plugins");
    getLogger().info("========================================");
  }

  @Override
  public void onDisable() {
    getLogger().info("Shutting down IremiaMinigameCore...");

    // Clean up any active games
    if (minigameManager != null) {
      int activeGames = minigameManager.getActiveGames().size();
      if (activeGames > 0) {
        getLogger().info("Ending " + activeGames + " active games...");
        minigameManager.getActiveGames().forEach((id, game) -> {
          try {
            game.end();
            getLogger().info("Ended game: " + id);
          } catch (Exception e) {
            getLogger().warning("Error ending game " + id + ": " + e.getMessage());
          }
        });
      }
    }

    getLogger().info("IremiaMinigameCore has been disabled!");
  }

  /**
   * Get the minigame manager instance
   * This is the main entry point for managing minigames
   *
   * @return The minigame manager
   */
  public MinigameManager getMinigameManager() {
    return minigameManager;
  }

  /**
   * Get the API instance
   * This provides the full API interface for external plugins
   *
   * @return The API instance
   */
  public IremiaMinigameAPI getAPI() {
    return api;
  }

  /**
   * Get the API instance (static access for other plugins)
   * External plugins should use this method to access the API
   *
   * @return The API instance or null if not available
   */
  public static IremiaMinigameAPI getMinigameAPI() {
    IremiaMinigameCorePlugin plugin = (IremiaMinigameCorePlugin) org.bukkit.Bukkit.getPluginManager()
        .getPlugin("IremiaMinigameCore");
    return plugin != null ? plugin.getAPI() : null;
  }

  /**
   * Check if the API is available
   * Useful for soft dependencies
   *
   * @return true if the API is loaded and available
   */
  public static boolean isAPIAvailable() {
    return getMinigameAPI() != null;
  }
}
