package org.alpacaindustries.iremiaminigamecore.minigame;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base abstract class for all minigames.
 * Provides the core functionality and lifecycle management for minigames.
 */
public abstract class Minigame implements Listener {

  private final String id;
  private final String displayName;
  private final MinigameManager manager;
  private final Set<UUID> players = new HashSet<>();
  private final List<Consumer<Minigame>> endListeners = new ArrayList<>();
  // Add player cache for performance
  private final Map<UUID, Player> playerCache = new HashMap<>();

  private Location spawnPoint;
  private MinigameState state = MinigameState.WAITING;
  private int minPlayers;
  private int maxPlayers;
  private boolean allowJoinDuringGame = false;

  /**
   * Create a new minigame
   *
   * @param id          Unique identifier for this minigame
   * @param displayName User-friendly name for this minigame
   * @param manager     The manager that created this minigame
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  protected Minigame(String id, String displayName, MinigameManager manager) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Minigame ID cannot be null or empty");
    }
    if (displayName == null || displayName.trim().isEmpty()) {
      throw new IllegalArgumentException("Display name cannot be null or empty");
    }
    if (manager == null) {
      throw new IllegalArgumentException("MinigameManager cannot be null");
    }

    this.id = id.trim();
    this.displayName = displayName.trim();
    this.manager = manager;
    this.minPlayers = MinigameConfig.getDefaultMinPlayers();
    this.maxPlayers = MinigameConfig.getDefaultMaxPlayers();
  }

  /**
   * Initialize the minigame
   */
  public void initialize() {
    setState(MinigameState.WAITING);
    getManager().getPlugin().getServer().getPluginManager().registerEvents(this, getManager().getPlugin());
  }

  /**
   * Start the minigame
   */
  public void start() {
    if (state != MinigameState.WAITING && state != MinigameState.COUNTDOWN) {
      return;
    }

    if (players.size() < minPlayers) {
      broadcastMessage(Component.text(MinigameConfig.getMsgNotEnoughPlayers()));
      return;
    }

    setState(MinigameState.RUNNING);
    onStart();
  }

  /**
   * End the minigame and clean up resources
   */
  public void end() {
    if (state == MinigameState.ENDED) {
      return;
    }

    setState(MinigameState.ENDED);

    try {
      onEnd();
    } catch (Exception e) {
      manager.getPlugin().getLogger().warning("Error during minigame end: " + e.getMessage());
    }

    // Notify end listeners FIRST (before clearing them!)
    for (Consumer<Minigame> listener : endListeners) {
      try {
        listener.accept(this);
      } catch (Exception e) {
        manager.getPlugin().getLogger().warning("Error in end listener: " + e.getMessage());
      }
    }

    // THEN unregister events and clear caches
    //HandlerList.unregisterAll(this);

    // Clear caches and collections to prevent memory leaks
    playerCache.clear();
    endListeners.clear();
  }

  /**
   * Add a player to the minigame
   *
   * @param player Player to add
   * @return true if player was successfully added
   */
  public boolean addPlayer(Player player) {
    if (player == null) {
      return false;
    }

    if (players.contains(player.getUniqueId())) {
      return false;
    }

    if (players.size() >= maxPlayers) {
      player.sendMessage(MinigameConfig.getMsgGameFull());
      return false;
    }

    if (state == MinigameState.RUNNING && !allowJoinDuringGame) {
      player.sendMessage(MinigameConfig.getMsgGameInProgress());
      return false;
    }

    // Add player and handle setup
    players.add(player.getUniqueId());
    playerCache.put(player.getUniqueId(), player);

    if (spawnPoint != null) {
      player.teleport(spawnPoint);
    }

    try {
      onPlayerJoin(player);
    } catch (Exception e) {
      manager.getPlugin().getLogger().warning("Error in onPlayerJoin for " + player.getName() + ": " + e.getMessage());
    }

    // Auto-start logic
    if (state == MinigameState.WAITING && players.size() >= minPlayers) {
      startCountdown();
    }

    return true;
  }

  /**
   * Remove a player from the minigame
   *
   * @param player Player to remove
   */
  public void removePlayer(Player player) {
    if (!players.contains(player.getUniqueId())) {
      return;
    }

    players.remove(player.getUniqueId());
    playerCache.remove(player.getUniqueId());
    onPlayerLeave(player);

    // End game if not enough players
    if (state == MinigameState.RUNNING && players.size() < minPlayers) {
      broadcastMessage(Component.text(MinigameConfig.getMsgPlayersRemaining()));
      end();
    }

    // Cancel countdown if not enough players
    if (state == MinigameState.COUNTDOWN && players.size() < minPlayers) {
      setState(MinigameState.WAITING);
      broadcastMessage(Component.text(MinigameConfig.getMsgCountdownCancelled()));
    }
  }

  /**
   * Send a message to all players in the minigame
   *
   * @param message Message to send
   */
  public void broadcastMessage(Component message) {
    for (UUID uuid : players) {
      Player player = getPlayerById(uuid);
      if (player != null) {
        player.sendMessage(message);
      }
    }
  }

  /**
   * Broadcast message to all players with batch optimization
   *
   * @param message Message to send
   */
  public void broadcastMessageBatch(Component message) {
    List<Player> onlinePlayers = getOnlinePlayers();
    onlinePlayers.forEach(player -> player.sendMessage(message));
  }

  /**
   * Start the countdown to begin the game
   */
  public void startCountdown() {
    setState(MinigameState.COUNTDOWN);
    onCountdownStart();
  }

  /**
   * Add a listener that will be called when this minigame ends
   *
   * @param listener Consumer that takes the ended minigame
   */
  public void addEndListener(Consumer<Minigame> listener) {
    endListeners.add(listener);
  }

  /**
   * Get a player by UUID with caching for performance
   */
  protected Player getPlayerById(UUID uuid) {
    Player cached = playerCache.get(uuid);
    if (cached != null && cached.isOnline()) {
      return cached;
    }

    Player player = manager.getPlugin().getServer().getPlayer(uuid);
    if (player != null) {
      playerCache.put(uuid, player);
    } else {
      playerCache.remove(uuid);
    }
    return player;
  }

  /**
   * Get all online players in this minigame
   */
  protected List<Player> getOnlinePlayers() {
    List<Player> onlinePlayers = new ArrayList<>();
    for (UUID uuid : players) {
      Player player = getPlayerById(uuid);
      if (player != null) {
        onlinePlayers.add(player);
      }
    }
    return onlinePlayers;
  }

  /**
   * Validate if a player is in a valid state for this minigame
   *
   * @param player Player to validate
   * @return true if player is valid and can participate
   */
  protected boolean isPlayerValid(Player player) {
    return player != null && player.isOnline() && players.contains(player.getUniqueId());
  }

  /**
   * Clean up expired entries from player cache
   * Should be called periodically to prevent memory leaks
   */
  public void cleanupPlayerCache() {
    playerCache.entrySet().removeIf(entry -> {
      Player player = entry.getValue();
      return player == null || !player.isOnline() || !players.contains(entry.getKey());
    });
  }

  // Abstract methods that minigames must implement

  /**
   * Called when the minigame starts
   * Override this method and call super.onStart() to maintain base functionality
   */
  protected void onStart() {
    // Default implementation - can be overridden
    getManager().getPlugin().getLogger().info("Minigame " + getId() + " started with " + getPlayerCount() + " players");
  }

  /**
   * Called when the minigame ends
   * Override this method and call super.onEnd() to maintain base functionality
   */
  protected void onEnd() {
    // Default implementation - cleanup player cache
    cleanupPlayerCache();
    getManager().getPlugin().getLogger().info("Minigame " + getId() + " ended");
  }

  /**
   * Called when a player joins the minigame
   * Override this method and call super.onPlayerJoin(player) to maintain base
   * functionality
   *
   * @param player Player who joined
   */
  protected void onPlayerJoin(Player player) {
    // Default implementation - update player cache and validate
    playerCache.put(player.getUniqueId(), player);
    getManager().getPlugin().getLogger().info("Player " + player.getName() + " joined minigame " + getId());
  }

  /**
   * Called when a player leaves the minigame
   * Override this method and call super.onPlayerLeave(player) to maintain base
   * functionality
   *
   * @param player Player who left
   */
  protected void onPlayerLeave(Player player) {
    // Default implementation - cleanup player cache
    playerCache.remove(player.getUniqueId());
    getManager().getPlugin().getLogger().info("Player " + player.getName() + " left minigame " + getId());
  }

  /**
   * Called when the countdown starts
   * Override this method and call super.onCountdownStart() to maintain base
   * functionality
   */
  protected void onCountdownStart() {
    // Default implementation - can be overridden
    getManager().getPlugin().getLogger().info("Countdown started for minigame " + getId());
  }

  // Getters and Setters

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public MinigameManager getManager() {
    return manager;
  }

  public Set<UUID> getPlayers() {
    return new HashSet<>(players);
  }

  public int getPlayerCount() {
    return players.size();
  }

  public MinigameState getState() {
    return state;
  }

  public void setState(MinigameState state) {
    this.state = state;
  }

  public Location getSpawnPoint() {
    return spawnPoint;
  }

  public void setSpawnPoint(Location spawnPoint) {
    this.spawnPoint = spawnPoint;
  }

  public int getMinPlayers() {
    return minPlayers;
  }

  public void setMinPlayers(int minPlayers) {
    if (minPlayers < 1) {
      throw new IllegalArgumentException("Minimum players must be at least 1");
    }
    if (minPlayers > maxPlayers) {
      throw new IllegalArgumentException("Minimum players cannot exceed maximum players");
    }
    this.minPlayers = minPlayers;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    if (maxPlayers < 1) {
      throw new IllegalArgumentException("Maximum players must be at least 1");
    }
    if (maxPlayers < minPlayers) {
      throw new IllegalArgumentException("Maximum players cannot be less than minimum players");
    }
    this.maxPlayers = maxPlayers;
  }

  public boolean isAllowJoinDuringGame() {
    return allowJoinDuringGame;
  }

  public void setAllowJoinDuringGame(boolean allowJoinDuringGame) {
    this.allowJoinDuringGame = allowJoinDuringGame;
  }
}
