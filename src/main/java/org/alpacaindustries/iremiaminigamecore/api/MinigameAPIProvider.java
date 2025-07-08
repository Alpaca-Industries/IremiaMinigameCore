package org.alpacaindustries.iremiaminigamecore.api;

import org.alpacaindustries.iremiaminigamecore.IremiaMinigameCorePlugin;
import org.alpacaindustries.iremiaminigamecore.api.impl.IremiaMinigameAPIImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

/**
 * Provider class for the Iremia Minigame API
 * Handles registration and access to the API for external plugins
 */
public class MinigameAPIProvider {

  private static IremiaMinigameAPI apiInstance;
  private static IremiaMinigameAPIImpl apiImpl;

  /**
   * Initialize the API (called by main plugin on enable)
   */
  public static void initialize(IremiaMinigameCorePlugin plugin) {
    if (apiInstance != null) {
      throw new IllegalStateException("API is already initialized!");
    }

    apiImpl = new IremiaMinigameAPIImpl(plugin.getMinigameManager(), plugin.getLogger());
    apiInstance = apiImpl;

    // Register as a Bukkit service so other plugins can access it
    Bukkit.getServicesManager().register(
        IremiaMinigameAPI.class,
        apiInstance,
        plugin,
        ServicePriority.Normal);

    plugin.getLogger().info("Iremia Minigame API v" + apiInstance.getAPIVersion() + " initialized");
  }

  /**
   * Shutdown the API (called by main plugin on disable)
   */
  public static void shutdown() {
    if (apiInstance != null) {
      // Clean up all plugin registrations
      for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
        if (plugin.isEnabled() && !plugin.equals(Bukkit.getPluginManager().getPlugin("IremiaMinigameCore"))) {
          apiImpl.cleanupPlugin(plugin);
        }
      }

      // Unregister the service
      Bukkit.getServicesManager().unregister(IremiaMinigameAPI.class, apiInstance);

      apiInstance = null;
      apiImpl = null;
    }
  }

  /**
   * Get the API instance (for external plugins)
   *
   * @return The API instance, or null if not initialized
   */
  public static IremiaMinigameAPI getAPI() {
    if (apiInstance == null) {
      // Try to get it from Bukkit services
      return Bukkit.getServicesManager().load(IremiaMinigameAPI.class);
    }
    return apiInstance;
  }

  /**
   * Check if the API is available
   *
   * @return true if the API is initialized and available
   */
  public static boolean isAPIAvailable() {
    return getAPI() != null;
  }

  /**
   * Get the internal API implementation (for main plugin use only)
   */
  public static IremiaMinigameAPIImpl getInternalAPI() {
    return apiImpl;
  }
}
