package org.alpacaindustries.iremiaminigamecore.api.impl;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameFactory;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper factory that adds event notifications to externally registered
 * minigames and bridges Java MinigameFactory to Kotlin MinigameFactoryKt
 */
class EventNotifyingFactory implements MinigameFactory {

	private final MinigameFactory delegate;
	private final IremiaMinigameAPIImpl api;

	public EventNotifyingFactory(MinigameFactory delegate, IremiaMinigameAPIImpl api) {
		this.delegate = delegate;
		this.api = api;
	}

	@Override
	public @Nullable Minigame createMinigame(String id, MinigameManager manager) {
		Minigame minigame = delegate.createMinigame(id, manager);

		if (minigame != null) {
			// Wrap the minigame with event notifications
			return new EventNotifyingMinigame(minigame, api);
		}

		return null;
	}
}
