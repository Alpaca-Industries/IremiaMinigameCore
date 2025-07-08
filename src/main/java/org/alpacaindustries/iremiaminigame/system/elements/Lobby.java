package org.alpacaindustries.iremiaminigame.system.elements;

import org.alpacaindustries.iremiaminigame.IremiaMinigamePlugin;
import org.alpacaindustries.iremiaminigame.core.listeners.JoinListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;

public class Lobby {
  private static final int LOBBY_SIDE_LENGTH = 16;

  public Lobby(Location center, IremiaMinigamePlugin plugin) {
    String worldName = plugin.getWorldName();
    World world = plugin.getServer().getWorld(worldName);
    if (world == null) {
      plugin.getLogger().warning("World '" + worldName + "' not found. Lobby not created.");
      return;
    }

    int halfLength = LOBBY_SIDE_LENGTH / 2;
    int y = center.getBlockY();
    int centerX = center.getBlockX();
    int centerZ = center.getBlockZ();

    for (int x = centerX - halfLength; x <= centerX + halfLength; x++) {
      for (int z = centerZ - halfLength; z <= centerZ + halfLength; z++) {
        world.getBlockAt(x, y, z).setType(Material.STONE);
      }
    }

    final Location spawnPoint = new Location(world, centerX, y + 1, centerZ);
    world.setSpawnLocation(spawnPoint);
    PluginManager pm = plugin.getServer().getPluginManager();
    pm.registerEvents(new JoinListener(spawnPoint), plugin);
  }
}
