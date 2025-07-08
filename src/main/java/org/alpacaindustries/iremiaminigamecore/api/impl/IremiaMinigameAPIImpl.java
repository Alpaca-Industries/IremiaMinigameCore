package org.alpacaindustries.iremiaminigamecore.api.impl;

import org.alpacaindustries.iremiaminigamecore.api.IremiaMinigameAPI;
import org.alpacaindustries.iremiaminigamecore.api.MinigameEventListener;
import org.alpacaindustries.iremiaminigamecore.api.MinigameTypeInfo;
import org.alpacaindustries.iremiaminigamecore.api.factory.MinigameFactory;
import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Implementation of the IremiaMinigameAPI
 * Provides the actual functionality for external plugin integration
 */
public class IremiaMinigameAPIImpl implements IremiaMinigameAPI {

	private static final String API_VERSION = "1.0.0";

	private final MinigameManager minigameManager;
	private final Logger logger;

	// Track which plugins registered which types for proper cleanup
	private final Map<String, Plugin> typeOwners = new ConcurrentHashMap<>();
	private final Map<String, MinigameTypeInfo> typeInfoMap = new ConcurrentHashMap<>();

	// Global event listeners
	private final Map<Plugin, Set<MinigameEventListener>> globalListeners = new ConcurrentHashMap<>();

	public IremiaMinigameAPIImpl(MinigameManager minigameManager, Logger logger) {
		this.minigameManager = minigameManager;
		this.logger = logger;
	}

	@Override
	public String getAPIVersion() {
		return API_VERSION;
	}

	@Override
	public boolean registerMinigameType(Plugin plugin, String typeId, MinigameFactory factory, String displayName) {
		if (plugin == null || typeId == null || factory == null) {
			logger.warning("Invalid parameters for minigame type registration");
			return false;
		}

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
	public boolean unregisterMinigameType(Plugin plugin, String typeId) {
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
	public Minigame createMinigame(String typeId, String instanceId) {
		return minigameManager.createMinigame(typeId, instanceId);
	}

	@Override
	public Map<String, Minigame> getActiveMinigames() {
		return minigameManager.getActiveGames();
	}

	@Override
	public Set<String> getRegisteredTypes() {
		return new HashSet<>(typeOwners.keySet());
	}

	@Override
	public Minigame getPlayerMinigame(Player player) {
		return minigameManager.getPlayerGame(player);
	}

	@Override
	public boolean addPlayerToMinigame(Player player, String minigameId) {
		return minigameManager.addPlayerToGame(player, minigameId);
	}

	@Override
	public boolean removePlayerFromMinigame(Player player) {
		return minigameManager.removePlayerFromGame(player);
	}

	@Override
	public MinigameManager getMinigameManager() {
		return minigameManager;
	}

	@Override
	public boolean isMinigameTypeRegistered(String typeId) {
		return typeOwners.containsKey(typeId);
	}

	@Override
	public MinigameTypeInfo getMinigameTypeInfo(String typeId) {
		return typeInfoMap.get(typeId);
	}

	@Override
	public void registerGlobalMinigameListener(Plugin plugin, MinigameEventListener listener) {
		globalListeners.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(listener);
		logger.info("Registered global minigame listener for plugin " + plugin.getName());
	}

	@Override
	public void unregisterGlobalMinigameListener(Plugin plugin, MinigameEventListener listener) {
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
	public void cleanupPlugin(Plugin plugin) {
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
	public void notifyListeners(String eventType, Object... args) {
		for (Set<MinigameEventListener> listeners : globalListeners.values()) {
			for (MinigameEventListener listener : listeners) {
				try {
					switch (eventType) {
						case "created":
							listener.onMinigameCreated((Minigame) args[0]);
							break;
						case "started":
							listener.onMinigameStart((Minigame) args[0]);
							break;
						case "ended":
							listener.onMinigameEnd((Minigame) args[0]);
							break;
						case "playerJoin":
							listener.onPlayerJoinMinigame((Player) args[0], (Minigame) args[1]);
							break;
						case "playerLeave":
							listener.onPlayerLeaveMinigame((Player) args[0], (Minigame) args[1]);
							break;
						case "playerEliminated":
							listener.onPlayerEliminated((Player) args[0], (Minigame) args[1], (String) args[2]);
							break;
						case "countdownStart":
							listener.onCountdownStart((Minigame) args[0]);
							break;
					}
				} catch (Exception e) {
					logger.warning("Error in global minigame listener: " + e.getMessage());
				}
			}
		}
	}
}
