package org.alpacaindustries.iremiaminigamecore.api;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.bukkit.entity.Player;

/**
 * Event listener interface for external plugins to hook into minigame events
 *
 * @since 1.0.0
 */
public interface MinigameEventListener {

  /**
   * Called when a new minigame is created
   *
   * @param minigame The minigame that was created
   */
  default void onMinigameCreated(Minigame minigame) {
  }

  /**
   * Called when a minigame starts
   *
   * @param minigame The minigame that started
   */
  default void onMinigameStart(Minigame minigame) {
  }

  /**
   * Called when a minigame ends
   *
   * @param minigame The minigame that ended
   */
  default void onMinigameEnd(Minigame minigame) {
  }

  /**
   * Called when a player joins a minigame
   *
   * @param player   The player who joined
   * @param minigame The minigame they joined
   */
  default void onPlayerJoinMinigame(Player player, Minigame minigame) {
  }

  /**
   * Called when a player leaves a minigame
   *
   * @param player   The player who left
   * @param minigame The minigame they left
   */
  default void onPlayerLeaveMinigame(Player player, Minigame minigame) {
  }

  /**
   * Called when a player is eliminated from a minigame
   *
   * @param player   The player who was eliminated
   * @param minigame The minigame they were eliminated from
   * @param reason   The reason for elimination
   */
  default void onPlayerEliminated(Player player, Minigame minigame, String reason) {
  }

  /**
   * Called when a minigame's countdown starts
   *
   * @param minigame The minigame whose countdown started
   */
  default void onCountdownStart(Minigame minigame) {
  }
}
