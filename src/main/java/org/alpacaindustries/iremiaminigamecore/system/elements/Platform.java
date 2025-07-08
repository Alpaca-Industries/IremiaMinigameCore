package org.alpacaindustries.iremiaminigamecore.system.elements;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a circular platform in the game world
 * Handles creation, modification, and cleanup of platform blocks
 */
public class Platform {
  private final Location origin;
  private final World world;
  private int radius;
  private Material platformMaterial;
  private final Set<Location> platformBlocks = new HashSet<>(); // Track blocks for cleanup

  public Location getCenter() {
    return origin;
  }

  public Platform(Location origin, World world) {
    this(origin, world, 2, Material.GRASS_BLOCK);
  }

  public Platform(Location origin, World world, int radius, Material material) {
    this.origin = origin;
    this.world = world;
    this.radius = radius;
    this.platformMaterial = material;
    createPlatform();
  }

  /**
   * Creates a square platform centered at the origin
   */
  public void createPlatform() {
    platformBlocks.clear();
    int ox = origin.getBlockX();
    int oy = origin.getBlockY();
    int oz = origin.getBlockZ();

    // Create a square platform
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        Location blockLoc = new Location(world, ox + x, oy, oz + z);
        platformBlocks.add(blockLoc);
        world.getBlockAt(blockLoc).setType(platformMaterial);
      }
    }
  }


  /**
   * Change the material of all platform blocks
   *
   * @param material The new material to use
   */
  public void setPlatformBlock(Material material) {
    this.platformMaterial = material;
    for (Location loc : platformBlocks) {
      world.getBlockAt(loc).setType(material);
    }
  }

  /**
   * Resize the platform to a new radius
   *
   * @param newRadius The new radius
   */
  public void resize(int newRadius) {
    // If shrinking, remove blocks outside new radius
    if (newRadius < radius) {
      int ox = origin.getBlockX();
      int oy = origin.getBlockY();
      int oz = origin.getBlockZ();

      Set<Location> toRemove = new HashSet<>();
      for (Location loc : platformBlocks) {
        int x = loc.getBlockX() - ox;
        int z = loc.getBlockZ() - oz;

        if (x * x + z * z > newRadius * newRadius) {
          world.getBlockAt(loc).setType(Material.AIR);
          toRemove.add(loc);
        }
      }
      platformBlocks.removeAll(toRemove);
    }
    // If expanding, add new blocks
    else if (newRadius > radius) {
      int ox = origin.getBlockX();
      int oy = origin.getBlockY();
      int oz = origin.getBlockZ();

      for (int x = -newRadius; x <= newRadius; x++) {
        for (int z = -newRadius; z <= newRadius; z++) {
          // Only process blocks not already in the platform
          int distanceSquared = x * x + z * z;
          if (distanceSquared <= newRadius * newRadius && distanceSquared > radius * radius) {
            Location blockLoc = new Location(world, ox + x, oy, oz + z);
            platformBlocks.add(blockLoc);
            world.getBlockAt(blockLoc).setType(platformMaterial);
          }
        }
      }
    }

    this.radius = newRadius;
  }

  /**
   * Clear the platform by replacing all blocks with air
   */
  public void clear() {
    for (Location loc : platformBlocks) {
      world.getBlockAt(loc).setType(Material.AIR);
    }
    platformBlocks.clear();
  }

  /**
   * Get the current radius of the platform
   */
  public int getRadius() {
    return radius;
  }
}
