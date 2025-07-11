package org.alpacaindustries.iremiaminigamecore.api.factory;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;

/**
 * Factory interface for creating minigame instances
 */
public interface MinigameFactory {

	/**
	 * Create a new instance of a minigame
	 *
	 * @param id      The unique identifier for this minigame instance
	 * @param manager The MinigameManager that created this game
	 * @return A new minigame instance
	 */
	Minigame createMinigame(String id, MinigameManager manager);
}
