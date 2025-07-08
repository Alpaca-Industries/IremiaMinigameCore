package org.alpacaindustries.iremiaminigamecore.minigame;

import org.alpacaindustries.iremiaminigamecore.IremiaMinigameCorePlugin;
import org.alpacaindustries.iremiaminigamecore.api.factory.MinigameFactory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central coordinator for all minigame instances and type registration
 * Handles lifecycle management and player-to-game mapping
 */
public class MinigameManager {

  private final IremiaMinigameCorePlugin plugin;
  private final Map<String, MinigameFactory> gameFactories = new ConcurrentHashMap<>();
  private final Map<String, Minigame> activeGames = new ConcurrentHashMap<>();
  private final Map<UUID, String> playerGameMap = new ConcurrentHashMap<>(); // Track which game each player is in

  public MinigameManager(IremiaMinigameCorePlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Register a minigame type for later instantiation
   * Thread-safe for dynamic registration during runtime
   */
  public void registerMinigame(String id, MinigameFactory factory) {
    if (id == null || id.trim().isEmpty()) {
      plugin.getLogger().warning("Cannot register minigame: ID is null or empty");
      return;
    }
    if (factory == null) {
      plugin.getLogger().warning("Cannot register minigame: factory is null");
      return;
    }

    gameFactories.put(id.toLowerCase(), factory);
    plugin.getLogger().info("Registered minigame type: " + id);
  }

  /**
   * Create a new instance of a registered minigame type
   *
   * @param typeId     The type ID of minigame to create
   * @param instanceId A unique ID for this specific game instance
   * @return The created minigame or null if type wasn't found
   */
  public Minigame createMinigame(String typeId, String instanceId) {
    if (typeId == null || typeId.trim().isEmpty()) {
      plugin.getLogger().warning("Cannot create minigame: typeId is null or empty");
      return null;
    }
    if (instanceId == null || instanceId.trim().isEmpty()) {
      plugin.getLogger().warning("Cannot create minigame: instanceId is null or empty");
      return null;
    }

    MinigameFactory factory = gameFactories.get(typeId.toLowerCase());
    if (factory == null) {
      plugin.getLogger().warning("Unknown minigame type: " + typeId);
      return null;
    }

    // Create unique instance ID if multiple of same type
    String fullId = typeId.toLowerCase() + "-" + instanceId;

    if (activeGames.containsKey(fullId)) {
      plugin.getLogger().warning("Minigame with ID " + fullId + " already exists!");
      return null;
    }

    try {
      Minigame minigame = factory.createMinigame(fullId, this);
      if (minigame == null) {
        plugin.getLogger().warning("Factory returned null minigame for type: " + typeId);
        return null;
      }

      activeGames.put(fullId, minigame);

      // Add auto-cleanup when game ends
      minigame.addEndListener(this::cleanupEndedGame);

      // Initialize the minigame
      minigame.initialize();

      return minigame;
    } catch (Exception e) {
      plugin.getLogger().severe("Error creating minigame " + fullId + ": " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Add a player to a minigame
   *
   * @param player Player to add
   * @param gameId ID of the minigame to add player to
   * @return true if successfully added
   */
  public boolean addPlayerToGame(Player player, String gameId) {
    // Check if player is already in a game
    if (playerGameMap.containsKey(player.getUniqueId())) {
      player.sendMessage("You are already in a game! Leave it first.");
      return false;
    }

    Minigame game = activeGames.get(gameId);
    if (game == null) {
      player.sendMessage("That game doesn't exist!");
      return false;
    }

    if (game.addPlayer(player)) {
      playerGameMap.put(player.getUniqueId(), gameId);
      return true;
    }

    return false;
  }

  /**
   * Remove a player from their current minigame
   *
   * @param player Player to remove
   * @return true if player was in a game and removed
   */
  public boolean removePlayerFromGame(Player player) {
    String gameId = playerGameMap.get(player.getUniqueId());
    if (gameId == null) {
      return false;
    }

    Minigame game = activeGames.get(gameId);
    if (game == null) {
      playerGameMap.remove(player.getUniqueId());
      return false;
    }

    game.removePlayer(player);
    playerGameMap.remove(player.getUniqueId());
    return true;
  }

  /**
   * Get the minigame a player is currently in
   *
   * @param player The player
   * @return The minigame or null if not in a game
   */
  public Minigame getPlayerGame(Player player) {
    String gameId = playerGameMap.get(player.getUniqueId());
    if (gameId == null) {
      return null;
    }
    return activeGames.get(gameId);
  }

  /**
   * End and clean up a minigame
   *
   * @param game The minigame to clean up
   */
  private void cleanupEndedGame(Minigame game) {
    String gameId = game.getId();

    // Remove all players from the game tracking
    Set<UUID> gamePlayers = new HashSet<>(game.getPlayers());
    for (UUID playerId : gamePlayers) {
      playerGameMap.remove(playerId);
    }

    // Remove from active games
    activeGames.remove(gameId);
    plugin.getLogger().info("Minigame " + gameId + " has ended and been cleaned up");
  }

  /**
   * Get all active minigames
   *
   * @return Map of game IDs to minigame instances
   */
  public Map<String, Minigame> getActiveGames() {
    return new HashMap<>(activeGames);
  }

  /**
   * Get registered minigame types
   *
   * @return Set of minigame type IDs
   */
  public Set<String> getMinigameTypes() {
    return new HashSet<>(gameFactories.keySet());
  }

  /**
   * Get the plugin instance
   *
   * @return The plugin instance
   */
  public IremiaMinigameCorePlugin getPlugin() {
    return plugin;
  }
}
