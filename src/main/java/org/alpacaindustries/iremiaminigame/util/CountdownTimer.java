package org.alpacaindustries.iremiaminigame.util;

import org.alpacaindustries.iremiaminigame.IremiaMinigameCorePlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/**
 * Chainable countdown timer with callback support for minigame events
 * Provides fluent API for timer configuration and automatic task cleanup
 */
public class CountdownTimer {
  private final IremiaMinigameCorePlugin plugin;
  private final int startSeconds;
  private int secondsLeft;
  private BukkitTask task;

  private Consumer<Integer> onCount;
  private Runnable onFinish;

  public CountdownTimer(IremiaMinigameCorePlugin plugin, int seconds) {
    this.plugin = plugin;
    this.startSeconds = seconds;
    this.secondsLeft = seconds;
  }

  /**
   * Set callback for each countdown tick - enables real-time updates
   */
  public CountdownTimer onCount(Consumer<Integer> onCount) {
    this.onCount = onCount;
    return this;
  }

  /**
   * Set callback for countdown completion - triggers final actions
   */
  public CountdownTimer onFinish(Runnable onFinish) {
    this.onFinish = onFinish;
    return this;
  }

  /**
   * Start the countdown with automatic task management
   * Prevents multiple concurrent timers from same instance
   */
  public CountdownTimer start() {
    // Cancel any existing task
    if (task != null) {
      task.cancel();
    }

    // Reset seconds
    this.secondsLeft = startSeconds;

    // Create new task
    task = new BukkitRunnable() {
      @Override
      public void run() {
        // Call count callback if set
        if (onCount != null) {
          onCount.accept(secondsLeft);
        }

        // Decrement counter
        secondsLeft--;

        // Check if finished
        if (secondsLeft < 0) {
          cancel();
          if (onFinish != null) {
            onFinish.run();
          }
          task = null;
        }
      }
    }.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)

    return this;
  }

  /**
   * Stop the countdown
   *
   * @return The remaining seconds when stopped
   */
  public int stop() {
    if (task != null) {
      task.cancel();
      task = null;
    }
    return secondsLeft;
  }

  /**
   * Check if the countdown is running
   *
   * @return true if the countdown is active
   */
  public boolean isRunning() {
    return task != null;
  }

  /**
   * Get the seconds left in the countdown
   *
   * @return Seconds remaining
   */
  public int getSecondsLeft() {
    return secondsLeft;
  }
}
