package org.alpacaindustries.iremiaminigame.api;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Main plugin class for the Iremia Minigame API
 * This plugin provides the API framework for external plugins to create
 * minigames
 */
public class IremiaMinigameAPIPlugin extends JavaPlugin {

	private static IremiaMinigameAPIPlugin instance;
	private Logger logger;

	@Override
	public void onEnable() {
		instance = this;
		logger = getLogger();

		logger.info("IremiaMinigameAPI v" + getDescription().getVersion() + " enabled!");
		logger.info("API is ready for external plugins to register minigame types.");

		// Register the API service for other plugins to access
		getServer().getServicesManager().register(
				IremiaMinigameAPI.class,
				new IremiaMinigameAPIStub(),
				this,
				ServicePriority.Normal);
	}

	@Override
	public void onDisable() {
		logger.info("IremiaMinigameAPI disabled!");
		instance = null;
	}

	/**
	 * Get the plugin instance
	 * 
	 * @return The plugin instance
	 */
	public static IremiaMinigameAPIPlugin getInstance() {
		return instance;
	}

	/**
	 * Stub implementation of the API that can be extended by the main plugin
	 */
	private static class IremiaMinigameAPIStub implements IremiaMinigameAPI {
		@Override
		public String getAPIVersion() {
			return "1.0.0";
		}

		// All other methods will throw UnsupportedOperationException
		// The main plugin will replace this service with a full implementation

		@Override
		public boolean registerMinigameType(org.bukkit.plugin.Plugin plugin, String typeId,
				org.alpacaindustries.iremiaminigame.api.factory.MinigameFactory factory,
				String displayName) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public boolean unregisterMinigameType(org.bukkit.plugin.Plugin plugin, String typeId) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public org.alpacaindustries.iremiaminigame.minigame.Minigame createMinigame(String typeId, String instanceId) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public java.util.Map<String, org.alpacaindustries.iremiaminigame.minigame.Minigame> getActiveMinigames() {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public java.util.Set<String> getRegisteredTypes() {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public org.alpacaindustries.iremiaminigame.minigame.Minigame getPlayerMinigame(
				org.bukkit.entity.Player player) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public boolean addPlayerToMinigame(org.bukkit.entity.Player player, String minigameId) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public boolean removePlayerFromMinigame(org.bukkit.entity.Player player) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public org.alpacaindustries.iremiaminigame.minigame.MinigameManager getMinigameManager() {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public boolean isMinigameTypeRegistered(String typeId) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public MinigameTypeInfo getMinigameTypeInfo(String typeId) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public void registerGlobalMinigameListener(org.bukkit.plugin.Plugin plugin, MinigameEventListener listener) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}

		@Override
		public void unregisterGlobalMinigameListener(org.bukkit.plugin.Plugin plugin, MinigameEventListener listener) {
			throw new UnsupportedOperationException(
					"Main minigame plugin not loaded. API stub only provides interfaces.");
		}
	}
}
