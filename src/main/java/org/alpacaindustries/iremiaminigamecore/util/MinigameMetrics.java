package org.alpacaindustries.iremiaminigamecore.util;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring and metrics for minigames
 */
public class MinigameMetrics {

  private final Minigame minigame;
  private final AtomicLong playerLookups = new AtomicLong(0);
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong cacheMisses = new AtomicLong(0);

  public MinigameMetrics(Minigame minigame) {
    this.minigame = minigame;
  }

  public void recordPlayerLookup() {
    playerLookups.incrementAndGet();
  }

  public void recordCacheHit() {
    cacheHits.incrementAndGet();
  }

  public void recordCacheMiss() {
    cacheMisses.incrementAndGet();
  }

  /**
   * Get cache hit ratio as percentage
   */
  public double getCacheHitRatio() {
    long hits = cacheHits.get();
    long total = hits + cacheMisses.get();
    return total > 0 ? (double) hits / total * 100 : 0;
  }

  /**
   * Get performance summary
   */
  public String getPerformanceSummary() {
    return String.format(
        "Minigame %s - Lookups: %d, Cache Hit Ratio: %.2f%%, Players: %d",
        minigame.getId(),
        playerLookups.get(),
        getCacheHitRatio(),
        minigame.getPlayerCount());
  }

  /**
   * Reset all metrics
   */
  public void reset() {
    playerLookups.set(0);
    cacheHits.set(0);
    cacheMisses.set(0);
  }
}
