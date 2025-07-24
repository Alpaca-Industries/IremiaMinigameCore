package org.alpacaindustries.iremiaminigamecore.api.impl;

import org.alpacaindustries.iremiaminigamecore.api.IremiaMinigameAPI;
import org.alpacaindustries.iremiaminigamecore.api.MinigameEventListener;
import org.alpacaindustries.iremiaminigamecore.api.MinigameTypeInfo;
import org.alpacaindustries.iremiaminigamecore.api.factory.MinigameFactory;
import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.Objects;

/**
 * Implementation of the IremiaMinigameAPI
 * Provides the actual functionality for external plugin integration
 */
public class IremiaMinigameAPIImpl implements IremiaMinigameAPI {

  private static final String API_VERSION = "1.0.0";

  private final @NotNull MinigameManager minigameManager;
  private final @NotNull Logger logger;

  // Track which plugins registered which types for proper cleanup
  private final Map<String, Plugin> typeOwners = new ConcurrentHashMap<>();
  private final Map<String, MinigameTypeInfo> typeInfoMap = new ConcurrentHashMap<>();

  // Global event listeners
  private final Map<Plugin, Set<MinigameEventListener>> globalListeners = new ConcurrentHashMap<>();

  public IremiaMinigameAPIImpl(@NotNull MinigameManager minigameManager, @NotNull Logger logger) {
    this.minigameManager = Objects.requireNonNull(minigameManager, "MinigameManager cannot be null");
    this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
  }

  @Override
  public @NotNull String getAPIVersion() {
    return API_VERSION;
  }

  @Override
  public boolean registerMinigameType(@NotNull Plugin plugin, @NotNull String typeId, @NotNull MinigameFactory factory, @Nullable String displayName) {
    Objects.requireNonNull(plugin, "Plugin cannot be null");
    Objects.requireNonNull(typeId, "typeId cannot be null");
    Objects.requireNonNull(factory, "MinigameFactory cannot be null");
    // Normalize type ID to include plugin prefix if not present
    String normalizedTypeId = typeId.contains(":") ? typeId : plugin.getName().toLowerCase() + ":" + typeId;

    // Check if type already exists
    if (typeOwners.containsKey(normalizedTypeId)) {
      logger.warning("Minigame type '" + normalizedTypeId + "' is already registered by " +
          typeOwners.get(normalizedTypeId).getName());
      return false;
    }

    try {
      // Create a wrapper factory that includes event notifications
      MinigameFactory wrappedFactory = new EventNotifyingFactory(factory, this);

      // Register with the manager
      minigameManager.registerMinigame(normalizedTypeId, wrappedFactory);

      // Track ownership and info
      typeOwners.put(normalizedTypeId, plugin);
      typeInfoMap.put(normalizedTypeId, new MinigameTypeInfo(
          normalizedTypeId,
          displayName != null ? displayName : typeId,
          plugin,
          "External minigame type registered by " + plugin.getName()));

      logger.info("Successfully registered minigame type '" + normalizedTypeId +
          "' from plugin " + plugin.getName());
      return true;

    } catch (Exception e) {
      logger.severe("Failed to register minigame type '" + normalizedTypeId + "': " + e.getMessage());
      return false;
    }
  }

  @Override
  public boolean unregisterMinigameType(@NotNull Plugin plugin, @NotNull String typeId) {
    Objects.requireNonNull(plugin, "Plugin cannot be null");
    Objects.requireNonNull(typeId, "typeId cannot be null");
    String normalizedTypeId = typeId.contains(":") ? typeId : plugin.getName().toLowerCase() + ":" + typeId;
    Plugin owner = typeOwners.get(normalizedTypeId);
    if (owner == null) {
      return false; // Type doesn't exist
    }

    if (!owner.equals(plugin)) {
      logger.warning("Plugin " + plugin.getName() + " tried to unregister type '" +
          normalizedTypeId + "' owned by " + owner.getName());
      return false;
    }

    // Remove all tracking
    typeOwners.remove(normalizedTypeId);
    typeInfoMap.remove(normalizedTypeId);

    // Note: We don't remove from MinigameManager to avoid breaking active games
    // Active games will continue to run but no new instances can be created

    logger.info("Unregistered minigame type '" + normalizedTypeId + "' from plugin " + plugin.getName());
    return true;
  }

  @Override
  public @Nullable Minigame createMinigame(@NotNull String typeId, @NotNull String instanceId) {
    Objects.requireNonNull(typeId, "typeId cannot be null");
    Objects.requireNonNull(instanceId, "instanceId cannot be null");
    return minigameManager.createMinigame(typeId, instanceId);
  }

  @Override
  public @NotNull Map<String, Minigame> getActiveMinigames() {
    return minigameManager.getActiveGames();
  }

  @Override
  public @NotNull Set<String> getRegisteredTypes() {
    return new HashSet<>(typeOwners.keySet());
  }

  @Override
  public @Nullable Minigame getPlayerMinigame(@NotNull Player player) {
    Objects.requireNonNull(player, "Player cannot be null");
    return minigameManager.getPlayerGame(player);
  }

  @Override
  public boolean addPlayerToMinigame(@NotNull Player player, @NotNull String minigameId) {
    Objects.requireNonNull(player, "Player cannot be null");
    Objects.requireNonNull(minigameId, "minigameId cannot be null");
    return minigameManager.addPlayerToGame(player, minigameId);
  }

  @Override
  public boolean removePlayerFromMinigame(@NotNull Player player) {
    Objects.requireNonNull(player, "Player cannot be null");
    return minigameManager.removePlayerFromGame(player);
  }

  @Override
  public @NotNull MinigameManager getMinigameManager() {
    return minigameManager;
  }

  @Override
  public boolean isMinigameTypeRegistered(@NotNull String typeId) {
    Objects.requireNonNull(typeId, "typeId cannot be null");
    return typeOwners.containsKey(typeId);
  }

  @Override
  public @Nullable MinigameTypeInfo getMinigameTypeInfo(@NotNull String typeId) {
    Objects.requireNonNull(typeId, "typeId cannot be null");
    return typeInfoMap.get(typeId);
  }

  @Override
  public void registerGlobalMinigameListener(@NotNull Plugin plugin, @NotNull MinigameEventListener listener) {
    Objects.requireNonNull(plugin, "Plugin cannot be null");
    Objects.requireNonNull(listener, "Listener cannot be null");
    globalListeners.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(listener);
    logger.info("Registered global minigame listener for plugin " + plugin.getName());
  }

  @Override
  public void unregisterGlobalMinigameListener(@NotNull Plugin plugin, @NotNull MinigameEventListener listener) {
    Objects.requireNonNull(plugin, "Plugin cannot be null");
    Objects.requireNonNull(listener, "Listener cannot be null");
    Set<MinigameEventListener> listeners = globalListeners.get(plugin);
    if (listeners != null) {
      listeners.remove(listener);
      if (listeners.isEmpty()) {
        globalListeners.remove(plugin);
      }
    }
  }

  /**
   * Clean up all registrations for a plugin (called when plugin disables)
   */
  public void cleanupPlugin(@NotNull Plugin plugin) {
    Objects.requireNonNull(plugin, "Plugin cannot be null");
    // Unregister all minigame types
    List<String> typesToRemove = new ArrayList<>();
    for (Map.Entry<String, Plugin> entry : typeOwners.entrySet()) {
      if (entry.getValue().equals(plugin)) {
        typesToRemove.add(entry.getKey());
      }
    }

    for (String typeId : typesToRemove) {
      unregisterMinigameType(plugin, typeId);
    }

    // Remove all global listeners
    globalListeners.remove(plugin);

    logger.info("Cleaned up all minigame registrations for plugin " + plugin.getName());
  }

  /**
   * Notify all global listeners of an event
   */
  public void notifyListeners(@NotNull String eventType, Object... args) {
    Objects.requireNonNull(eventType, "eventType cannot be null");
    for (Set<MinigameEventListener> listeners : globalListeners.values()) {
      for (MinigameEventListener listener : listeners) {
        try {
          switch (eventType) {
            case "created" -> listener.onMinigameCreated((Minigame) args[0]);
            case "started" -> listener.onMinigameStart((Minigame) args[0]);
            case "ended" -> listener.onMinigameEnd((Minigame) args[0]);
            case "playerJoin" -> listener.onPlayerJoinMinigame((Player) args[0], (Minigame) args[1]);
            case "playerLeave" -> listener.onPlayerLeaveMinigame((Player) args[0], (Minigame) args[1]);
            case "playerEliminated" -> listener.onPlayerEliminated((Player) args[0], (Minigame) args[1], (String) args[2]);
            case "countdownStart" -> listener.onCountdownStart((Minigame) args[0]);
          }
        } catch (Exception e) {
          logger.warning("Error in global minigame listener: " + e.getMessage());
        }
      }
    }
  }
}
