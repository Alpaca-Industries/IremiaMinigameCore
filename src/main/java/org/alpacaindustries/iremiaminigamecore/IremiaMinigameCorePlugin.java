package org.alpacaindustries.iremiaminigamecore;

import org.alpacaindustries.iremiaminigamecore.api.IremiaMinigameAPI;
import org.alpacaindustries.iremiaminigamecore.api.impl.IremiaMinigameAPIImpl;
import org.alpacaindustries.iremiaminigamecore.command.MinigameCommand;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameConfig;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * IremiaMinigameCore - Core minigame management system
 * Provides the foundation for creating and managing minigames
 *
 * This plugin serves as the core API and framework that other minigame
 * plugins can depend on to create their specific game implementations.
 */
public class IremiaMinigameCorePlugin extends JavaPlugin implements Listener {

  private @Nullable MinigameManager minigameManager;
  private @Nullable IremiaMinigameAPI api;

  @Override
  public void onEnable() {
    getLogger().info("Starting IremiaMinigameCore...");
    try {
      // Initialize configuration
      MinigameConfig.initialize(this);
      getLogger().info("Configuration initialized");

      // Initialize the minigame manager
      this.minigameManager = new MinigameManager(this);
      getLogger().info("MinigameManager initialized");

      // Initialize the API implementation
      this.api = new IremiaMinigameAPIImpl(minigameManager, getLogger());
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
    } catch (Exception e) {
      getLogger().severe("Failed to enable IremiaMinigameCore: " + e.getMessage());
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    getLogger().info("Shutting down IremiaMinigameCore...");
    try {
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
        if (api instanceof IremiaMinigameAPIImpl) {
          getLogger().info("Cleaned up API registrations");
        }
      }
      getLogger().info("IremiaMinigameCore has been disabled!");
    } catch (Exception e) {
      getLogger().severe("Error during plugin shutdown: " + e.getMessage());
    }
  }

  /**
   * Get the minigame manager instance.
   * This is the main entry point for managing minigames.
   *
   * @return The minigame manager, or null if not initialized
   */
  public @Nullable MinigameManager getMinigameManager() {
    return minigameManager;
  }

  /**
   * Get the API instance.
   * This provides the full API interface for external plugins.
   *
   * @return The API instance, or null if not initialized
   */
  public @Nullable IremiaMinigameAPI getAPI() {
    return api;
  }

  /**
   * Get the API instance (static access for other plugins).
   * External plugins should use this method to access the API.
   *
   * @return The API instance or null if not available
   */
  public static @Nullable IremiaMinigameAPI getMinigameAPI() {
    IremiaMinigameCorePlugin plugin = (IremiaMinigameCorePlugin) org.bukkit.Bukkit.getPluginManager()
        .getPlugin("IremiaMinigameCore");
    return plugin != null ? plugin.getAPI() : null;
  }

  /**
   * Check if the API is available.
   * Useful for soft dependencies.
   *
   * @return true if the API is loaded and available
   */
  public static boolean isAPIAvailable() {
    return getMinigameAPI() != null;
  }
}
