package org.alpacaindustries.iremiaminigame.system.elements;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages platform creation and arrangement for multiplayer games
 * Provides different layout strategies for various game types
 */
public class PlatformManager {

  public enum LayoutType {
    GRID, // Square grid arrangement
    CIRCLE, // Circular arrangement
    LINE, // Single line arrangement
    SCATTERED // Random scattered positions
  }

  /**
   * Create platforms for players using specified layout
   */
  public static Map<UUID, Platform> createPlatforms(
      Iterable<UUID> playerIds,
      Location centerLocation,
      int platformSize,
      Material material,
      LayoutType layout,
      int spacing) {

    Map<UUID, Platform> platforms = new HashMap<>();

    switch (layout) {
      case GRID:
        return createGridLayout(playerIds, centerLocation, platformSize, material, spacing);
      case CIRCLE:
        return createCircularLayout(playerIds, centerLocation, platformSize, material, spacing);
      case LINE:
        return createLineLayout(playerIds, centerLocation, platformSize, material, spacing);
      case SCATTERED:
        return createScatteredLayout(playerIds, centerLocation, platformSize, material, spacing);
      default:
        return createGridLayout(playerIds, centerLocation, platformSize, material, spacing);
    }
  }

  private static Map<UUID, Platform> createGridLayout(
      Iterable<UUID> playerIds,
      Location centerLocation,
      int platformSize,
      Material material,
      int spacing) {

    Map<UUID, Platform> platforms = new HashMap<>();

    // Count players
    int playerCount = 0;
    for (UUID ignored : playerIds)
      playerCount++;

    if (playerCount == 0)
      return platforms;

    int rows = (int) Math.ceil(Math.sqrt(playerCount));
    int cols = (int) Math.ceil((double) playerCount / rows);

    int startX = -(cols - 1) * spacing / 2;
    int startZ = -(rows - 1) * spacing / 2;

    int playerIndex = 0;
    for (UUID playerId : playerIds) {
      if (playerIndex >= playerCount)
        break;

      int row = playerIndex / cols;
      int col = playerIndex % cols;

      double x = centerLocation.getX() + startX + col * spacing;
      double z = centerLocation.getZ() + startZ + row * spacing;

      Location platformCenter = new Location(
          centerLocation.getWorld(),
          x,
          centerLocation.getY(),
          z);

      Platform platform = new Platform(
          platformCenter,
          centerLocation.getWorld(),
          platformSize / 2,
          material);

      platforms.put(playerId, platform);
      playerIndex++;
    }

    return platforms;
  }

  private static Map<UUID, Platform> createCircularLayout(
      Iterable<UUID> playerIds,
      Location centerLocation,
      int platformSize,
      Material material,
      int spacing) {

    Map<UUID, Platform> platforms = new HashMap<>();

    // Count players
    int playerCount = 0;
    for (UUID ignored : playerIds)
      playerCount++;

    if (playerCount == 0)
      return platforms;

    double radius = Math.max(spacing, playerCount * spacing / (2 * Math.PI));
    double angleStep = 2 * Math.PI / playerCount;

    int playerIndex = 0;
    for (UUID playerId : playerIds) {
      double angle = playerIndex * angleStep;

      double x = centerLocation.getX() + radius * Math.cos(angle);
      double z = centerLocation.getZ() + radius * Math.sin(angle);

      Location platformCenter = new Location(
          centerLocation.getWorld(),
          x,
          centerLocation.getY(),
          z);

      Platform platform = new Platform(
          platformCenter,
          centerLocation.getWorld(),
          platformSize / 2,
          material);

      platforms.put(playerId, platform);
      playerIndex++;
    }

    return platforms;
  }

  private static Map<UUID, Platform> createLineLayout(
      Iterable<UUID> playerIds,
      Location centerLocation,
      int platformSize,
      Material material,
      int spacing) {

    Map<UUID, Platform> platforms = new HashMap<>();

    // Count players
    int playerCount = 0;
    for (UUID ignored : playerIds)
      playerCount++;

    if (playerCount == 0)
      return platforms;

    double startX = centerLocation.getX() - (playerCount - 1) * spacing / 2.0;

    int playerIndex = 0;
    for (UUID playerId : playerIds) {
      double x = startX + playerIndex * spacing;

      Location platformCenter = new Location(
          centerLocation.getWorld(),
          x,
          centerLocation.getY(),
          centerLocation.getZ());

      Platform platform = new Platform(
          platformCenter,
          centerLocation.getWorld(),
          platformSize / 2,
          material);

      platforms.put(playerId, platform);
      playerIndex++;
    }

    return platforms;
  }

  private static Map<UUID, Platform> createScatteredLayout(
      Iterable<UUID> playerIds,
      Location centerLocation,
      int platformSize,
      Material material,
      int spacing) {

    Map<UUID, Platform> platforms = new HashMap<>();

    // For scattered layout, we'll place platforms randomly within a larger area
    int maxRadius = spacing * 3;

    for (UUID playerId : playerIds) {
      // Generate random position within radius
      double angle = Math.random() * 2 * Math.PI;
      double distance = Math.random() * maxRadius;

      double x = centerLocation.getX() + distance * Math.cos(angle);
      double z = centerLocation.getZ() + distance * Math.sin(angle);

      Location platformCenter = new Location(
          centerLocation.getWorld(),
          x,
          centerLocation.getY(),
          z);

      Platform platform = new Platform(
          platformCenter,
          centerLocation.getWorld(),
          platformSize / 2,
          material);

      platforms.put(playerId, platform);
    }

    return platforms;
  }

  /**
   * Clear all platforms in the map
   */
  public static void clearPlatforms(Map<UUID, Platform> platforms) {
    for (Platform platform : platforms.values()) {
      platform.clear();
    }
    platforms.clear();
  }
}
