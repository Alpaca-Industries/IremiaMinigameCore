package org.alpacaindustries.iremiaminigame.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Thread-safe async operation utilities with automatic exception handling
 * Bridges Bukkit's scheduler with CompletableFuture for modern async patterns
 */
public class AsyncUtils {

  /**
   * Execute supplier asynchronously with exception safety
   * Automatically handles thread context switching and error propagation
   */
  public static <T> CompletableFuture<T> runAsync(Plugin plugin, Supplier<T> task) {
    CompletableFuture<T> future = new CompletableFuture<>();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        T result = task.get();
        future.complete(result);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  /**
   * Execute runnable asynchronously with void return type
   */
  public static CompletableFuture<Void> runAsync(Plugin plugin, Runnable task) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        task.run();
        future.complete(null);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  /**
   * Run a task on the main thread after async completion
   */
  public static <T> CompletableFuture<T> runAsyncThenSync(Plugin plugin, Supplier<T> asyncTask, Runnable syncTask) {
    return runAsync(plugin, asyncTask).thenApply(result -> {
      Bukkit.getScheduler().runTask(plugin, syncTask);
      return result;
    });
  }
}
