package org.alpacaindustries.iremiaminigamecore.system.elements;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import java.util.HashSet;
import java.util.Set;

public class EventManager {
	private final Plugin plugin;
	private final Set<Listener> registeredListeners = new HashSet<>();

	public EventManager(Plugin plugin) {
		this.plugin = plugin;
	}

	public void registerListener(Listener listener) {
		plugin.getServer().getPluginManager().registerEvents(listener, plugin);
		registeredListeners.add(listener);
	}

	public void unregisterListener(Listener listener) {
		org.bukkit.event.HandlerList.unregisterAll(listener);
		registeredListeners.remove(listener);
	}

	public void unregisterAll() {
		for (Listener listener : registeredListeners) {
			org.bukkit.event.HandlerList.unregisterAll(listener);
		}
		registeredListeners.clear();
	}

	public boolean isRegistered(Listener listener) {
		return registeredListeners.contains(listener);
	}

	public Set<Listener> getRegisteredListeners() {
		return Set.copyOf(registeredListeners);
	}
}
