package org.alpacaindustries.iremiaminigamecore.api;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameFactory;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;

/**
 * Main API interface for external plugins to interact with the Iremia Minigame
 * System
 *
 * This API allows other plugins to:
 * - Register their own minigame types
 * - Create and manage minigame instances
 * - Hook into minigame events
 * - Access player and game state information
 *
 * @since 1.0.0
 */
public interface IremiaMinigameAPI {

  /**
   * Get the API version for compatibility checking
   *
   * @return API version string (e.g., "1.0.0")
   */
  String getAPIVersion();

  /**
   * Register a new minigame type that can be created by players
   *
   * @param plugin      The plugin registering this minigame type
   * @param typeId      Unique identifier for this minigame type (e.g.,
   *                    "myplugin:customgame")
   * @param factory     Factory that creates instances of this minigame
   * @param displayName User-friendly name for this minigame type
   * @return true if registration was successful, false if type already exists
   */
  boolean registerMinigameType(Plugin plugin, String typeId, MinigameFactory factory, String displayName);

  /**
   * Unregister a minigame type (usually called on plugin disable)
   *
   * @param plugin The plugin that registered this type
   * @param typeId The type ID to unregister
   * @return true if unregistration was successful
   */
  boolean unregisterMinigameType(Plugin plugin, String typeId);

  /**
   * Create a new minigame instance
   *
   * @param typeId     The type of minigame to create
   * @param instanceId Unique ID for this specific instance
   * @return The created minigame instance, or null if type doesn't exist
   */
  Minigame createMinigame(String typeId, String instanceId);

  /**
   * Get all active minigame instances
   *
   * @return Map of instance IDs to minigame instances
   */
  Map<String, Minigame> getActiveMinigames();

  /**
   * Get all registered minigame types
   *
   * @return Set of registered type IDs
   */
  Set<String> getRegisteredTypes();

  /**
   * Get the minigame a player is currently in
   *
   * @param player The player to check
   * @return The minigame instance, or null if not in a game
   */
  Minigame getPlayerMinigame(Player player);

  /**
   * Add a player to a specific minigame
   *
   * @param player     The player to add
   * @param minigameId The ID of the minigame instance
   * @return true if player was successfully added
   */
  boolean addPlayerToMinigame(Player player, String minigameId);

  /**
   * Remove a player from their current minigame
   *
   * @param player The player to remove
   * @return true if player was successfully removed
   */
  boolean removePlayerFromMinigame(Player player);

  /**
   * Get the underlying MinigameManager (for advanced use cases)
   *
   * @return The MinigameManager instance
   */
  MinigameManager getMinigameManager();

  /**
   * Check if a minigame type is registered
   *
   * @param typeId The type ID to check
   * @return true if the type is registered
   */
  boolean isMinigameTypeRegistered(String typeId);

  /**
   * Get information about a registered minigame type
   *
   * @param typeId The type ID to get info for
   * @return MinigameTypeInfo object, or null if type doesn't exist
   */
  MinigameTypeInfo getMinigameTypeInfo(String typeId);

  /**
   * Register a global event listener for minigame events
   *
   * @param plugin   The plugin registering the listener
   * @param listener The event listener
   */
  void registerGlobalMinigameListener(Plugin plugin, MinigameEventListener listener);

  /**
   * Unregister a global event listener
   *
   * @param plugin   The plugin that registered the listener
   * @param listener The event listener to remove
   */
  void unregisterGlobalMinigameListener(Plugin plugin, MinigameEventListener listener);
}
