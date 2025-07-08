package org.alpacaindustries.iremiaminigame.util;

import org.alpacaindustries.iremiaminigame.minigame.Minigame;
import org.alpacaindustries.iremiaminigame.minigame.MinigameState;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class with optimized helper methods for minigames
 */
public final class MinigameUtils {

  private MinigameUtils() {
    // Utility class
  }

  /**
   * Check if a minigame can accept new players
   */
  public static boolean canAcceptPlayers(Minigame minigame) {
    return minigame.getState() == MinigameState.WAITING ||
        (minigame.getState() == MinigameState.COUNTDOWN && minigame.getPlayerCount() < minigame.getMaxPlayers())
        ||
        (minigame.getState() == MinigameState.RUNNING && minigame.isAllowJoinDuringGame());
  }

  /**
   * Check if a minigame is ready to start
   */
  public static boolean isReadyToStart(Minigame minigame) {
    return minigame.getState() == MinigameState.WAITING &&
        minigame.getPlayerCount() >= minigame.getMinPlayers();
  }

  /**
   * Get player names from UUIDs efficiently
   */
  public static List<String> getPlayerNames(Collection<UUID> playerIds, Minigame minigame) {
    return playerIds.stream()
        .map(uuid -> {
          Player player = minigame.getManager().getPlugin().getServer().getPlayer(uuid);
          return player != null ? player.getName() : "Unknown";
        })
        .collect(Collectors.toList());
  }

  /**
   * Format time in MM:SS format
   */
  public static String formatTime(int totalSeconds) {
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;
    return String.format("%02d:%02d", minutes, seconds);
  }

  /**
   * Calculate percentage with safe division
   */
  public static double calculatePercentage(int value, int total) {
    return total > 0 ? (double) value / total * 100.0 : 0.0;
  }
}
