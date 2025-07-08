package org.alpacaindustries.iremiaminigame.util;

import org.alpacaindustries.iremiaminigame.minigame.Minigame;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utility class for periodic minigame maintenance tasks
 */
public class MinigameMaintenanceTask extends BukkitRunnable {

  private final Minigame minigame;

  public MinigameMaintenanceTask(Minigame minigame) {
    this.minigame = minigame;
  }

  @Override
  public void run() {
    // Clean up expired player cache entries
    minigame.cleanupPlayerCache();
  }

  /**
   * Start the maintenance task with default interval
   */
  public void start(Plugin plugin) {
    // Run every 30 seconds (600 ticks)
    this.runTaskTimer(plugin, 600L, 600L);
  }
}
