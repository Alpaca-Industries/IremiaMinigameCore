package org.alpacaindustries.iremiaminigame.api.impl;

import org.alpacaindustries.iremiaminigame.api.factory.MinigameFactory;
import org.alpacaindustries.iremiaminigame.minigame.Minigame;
import org.alpacaindustries.iremiaminigame.minigame.MinigameManager;

/**
 * Wrapper factory that adds event notifications to externally registered
 * minigames
 */
class EventNotifyingFactory implements MinigameFactory {

	private final MinigameFactory delegate;
	private final IremiaMinigameAPIImpl api;

	public EventNotifyingFactory(MinigameFactory delegate, IremiaMinigameAPIImpl api) {
		this.delegate = delegate;
		this.api = api;
	}

	@Override
	public Minigame createMinigame(String id, MinigameManager manager) {
		Minigame minigame = delegate.createMinigame(id, manager);

		if (minigame != null) {
			// Wrap the minigame with event notifications
			return new EventNotifyingMinigame(minigame, api);
		}

		return null;
	}
}
