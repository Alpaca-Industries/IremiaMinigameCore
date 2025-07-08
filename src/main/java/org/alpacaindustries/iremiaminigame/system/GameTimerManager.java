package org.alpacaindustries.iremiaminigame.system;

import org.alpacaindustries.iremiaminigame.util.CountdownTimer;
import org.alpacaindustries.iremiaminigame.IremiaMinigamePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages multiple timers for a minigame with automatic cleanup
 * Provides convenient methods for common timer patterns
 */
public class GameTimerManager {

  private final IremiaMinigamePlugin plugin;
  private final Map<String, CountdownTimer> timers = new HashMap<>();

  public GameTimerManager(IremiaMinigamePlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Create a countdown timer with standard announcements
   */
  public CountdownTimer createCountdownTimer(String name, int seconds, Consumer<Integer> onCount, Runnable onFinish) {
    CountdownTimer timer = new CountdownTimer(plugin, seconds)
        .onCount(onCount)
        .onFinish(onFinish);

    timers.put(name, timer);
    return timer;
  }

  /**
   * Create a repeating event timer
   */
  public CountdownTimer createEventTimer(String name, int intervalSeconds, Runnable onEvent) {
    CountdownTimer timer = new CountdownTimer(plugin, intervalSeconds)
        .onFinish(() -> {
          onEvent.run();
          // Restart the timer for continuous events
          if (timers.containsKey(name)) {
            timers.get(name).start();
          }
        });

    timers.put(name, timer);
    return timer;
  }

  /**
   * Start a timer by name
   */
  public void startTimer(String name) {
    CountdownTimer timer = timers.get(name);
    if (timer != null) {
      timer.start();
    }
  }

  /**
   * Stop a timer by name
   */
  public void stopTimer(String name) {
    CountdownTimer timer = timers.get(name);
    if (timer != null) {
      timer.stop();
    }
  }

  /**
   * Stop all timers
   */
  public void stopAllTimers() {
    for (CountdownTimer timer : timers.values()) {
      timer.stop();
    }
  }

  /**
   * Get a timer by name
   */
  public CountdownTimer getTimer(String name) {
    return timers.get(name);
  }

  /**
   * Check if a timer is running
   */
  public boolean isTimerRunning(String name) {
    CountdownTimer timer = timers.get(name);
    return timer != null && timer.isRunning();
  }

  /**
   * Clean up all timers
   */
  public void cleanup() {
    stopAllTimers();
    timers.clear();
  }
}
