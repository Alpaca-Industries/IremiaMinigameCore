package org.alpacaindustries.iremiaminigame.system;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Utility class for managing player state in minigames
 * Provides standardized methods for preparing players and cleaning up state
 */
public class PlayerManager {

  /**
   * Prepare a player for minigame participation with standard settings
   */
  public static void preparePlayer(Player player) {
    preparePlayer(player, null);
  }

  /**
   * Prepare a player for minigame participation
   */
  public static void preparePlayer(Player player, Location spawnLocation) {
    player.setGameMode(GameMode.ADVENTURE);
    player.setHealth(20.0);
    player.setFoodLevel(20);
    player.setSaturation(20.0f);
    player.setExhaustion(0.0f);
    player.getInventory().clear();
    player.setExp(0);
    player.setLevel(0);

    // Remove all potion effects
    clearPotionEffects(player);

    // Give basic food
    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 5));

    if (spawnLocation != null) {
      player.teleport(spawnLocation);
    }
  }

  /**
   * Clear all potion effects from a player
   */
  public static void clearPotionEffects(Player player) {
    for (PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
  }

  /**
   * Reset player to default state after minigame
   */
  public static void resetPlayer(Player player) {
    player.setGameMode(GameMode.ADVENTURE);
    player.setHealth(20.0);
    player.setFoodLevel(20);
    player.setSaturation(20.0f);
    player.setExhaustion(0.0f);
    clearPotionEffects(player);
    player.setFireTicks(0);
    player.setFallDistance(0);
  }

  /**
   * Make player a spectator with proper setup
   */
  public static void makeSpectator(Player player) {
    player.setGameMode(GameMode.SPECTATOR);
    player.setHealth(20.0);
    player.setFoodLevel(20);
    clearPotionEffects(player);
    player.setFireTicks(0);
  }
}
