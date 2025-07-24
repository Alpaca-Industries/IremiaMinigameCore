package org.alpacaindustries.iremiaminigamecore.minigame;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerQuitEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Objects;

/**
 * Base abstract class for all minigames.
 * Provides the core functionality and lifecycle management for minigames.
 * <p>
 * This class is thread-safe for all public methods unless otherwise noted.
 */
public abstract class Minigame implements Listener {

  private final @NotNull String id;
  private final @NotNull String displayName;
  private final @NotNull MinigameManager manager;
  private final Set<UUID> players = Collections.synchronizedSet(new HashSet<>());
  private final List<Consumer<Minigame>> endListeners = Collections.synchronizedList(new ArrayList<>());
  private final Map<UUID, Player> playerCache = Collections.synchronizedMap(new HashMap<>());

  private volatile Optional<Location> spawnPoint = Optional.empty();
  private volatile MinigameState state = MinigameState.WAITING;
  private volatile int minPlayers;
  private volatile int maxPlayers;
  private volatile boolean allowJoinDuringGame = false;
  private boolean shouldLoop = false;
  private long loopDelayTicks = 60L; // Default: 3 seconds (20 ticks = 1 second)

  /**
   * Create a new minigame.
   *
   * @param id          Unique identifier for this minigame
   * @param displayName User-friendly name for this minigame
   * @param manager     The manager that created this minigame
   * @throws IllegalArgumentException if any parameter is null or invalid
   */
  protected Minigame(@NotNull String id, @NotNull String displayName, @NotNull MinigameManager manager) {
    this.id = Objects.requireNonNull(id, "Minigame ID cannot be null").trim();
    if (this.id.isEmpty()) {
      throw new IllegalArgumentException("Minigame ID cannot be empty");
    }
    this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null").trim();
    if (this.displayName.isEmpty()) {
      throw new IllegalArgumentException("Display name cannot be empty");
    }
    this.manager = Objects.requireNonNull(manager, "MinigameManager cannot be null");
    this.minPlayers = MinigameConfig.getDefaultMinPlayers();
    this.maxPlayers = MinigameConfig.getDefaultMaxPlayers();
  }

  /**
   * Initialize the minigame. Registers event listeners and sets initial state.
   */
  public void initialize() {
    setState(MinigameState.WAITING);
    getManager().getPlugin().getServer().getPluginManager().registerEvents(this, getManager().getPlugin());
  }

  /**
   * Start the minigame. Only allowed from WAITING or COUNTDOWN state.
   */
  public synchronized void start() {
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
   * End the minigame and clean up resources.
   */
  public synchronized void end() {
    if (state == MinigameState.ENDED) {
      return;
    }
    setState(MinigameState.ENDED);
    try {
      onEnd();
    } catch (Exception e) {
      manager.getPlugin().getLogger().warning("Error during minigame end: " + e.getMessage());
    }
    for (Consumer<Minigame> listener : new ArrayList<>(endListeners)) {
      try {
        listener.accept(this);
      } catch (Exception e) {
        manager.getPlugin().getLogger().warning("Error in end listener: " + e.getMessage());
      }
    }

    //HandlerList.unregisterAll(this);


    // Clear caches and collections to prevent memory leaks
    playerCache.clear();
    endListeners.clear();
    players.clear();
    // Loop logic: schedule restart if enabled
    if (shouldLoop) {
      manager.getPlugin().getServer().getScheduler().runTaskLater(
          manager.getPlugin(),
          this::restartMinigame,
          loopDelayTicks
      );
    }
  }

  /**
   * Restart the minigame for looping.
   * This resets state and calls initialize/start.
   */
  private void restartMinigame() {
    initialize();
    start();
  }

  /**
   * Add a player to the minigame.
   *
   * @param player Player to add
   * @return true if player was successfully added
   */
  public boolean addPlayer(@NotNull Player player) {
    Objects.requireNonNull(player, "Player cannot be null");
    synchronized (players) {
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
      players.add(player.getUniqueId());
      playerCache.put(player.getUniqueId(), player);
      spawnPoint.ifPresent(player::teleport);
      try {
        onPlayerJoin(player);
      } catch (Exception e) {
        manager.getPlugin().getLogger().warning("Error in onPlayerJoin for " + player.getName() + ": " + e.getMessage());
      }
      if (state == MinigameState.WAITING && players.size() >= minPlayers) {
        startCountdown();
      }
      return true;
    }
  }

  /**
   * Remove a player from the minigame.
   *
   * @param player Player to remove
   */
  public void removePlayer(@NotNull Player player) {
    Objects.requireNonNull(player, "Player cannot be null");
    synchronized (players) {
      if (!players.contains(player.getUniqueId())) {
        return;
      }
      players.remove(player.getUniqueId());
      onPlayerCleanup(player);
      onPlayerLeave(player);
      if (state == MinigameState.RUNNING && players.size() < minPlayers) {
        broadcastMessage(Component.text(MinigameConfig.getMsgPlayersRemaining()));
        end();
      }
      if (state == MinigameState.COUNTDOWN && players.size() < minPlayers) {
        setState(MinigameState.WAITING);
        broadcastMessage(Component.text(MinigameConfig.getMsgCountdownCancelled()));
      }
    }
  }

  /**
   * Send a message to all players in the minigame.
   *
   * @param message Message to send
   */
  public void broadcastMessage(@NotNull Component message) {
    Objects.requireNonNull(message, "Message cannot be null");
    synchronized (players) {
      for (UUID uuid : players) {
        Player player = getPlayerById(uuid);
        if (player != null) {
          player.sendMessage(message);
        }
      }
    }
  }

  /**
   * Start the countdown to begin the game.
   */
  public void startCountdown() {
    setState(MinigameState.COUNTDOWN);
    onCountdownStart();
  }

  /**
   * Add a listener that will be called when this minigame ends.
   *
   * @param listener Consumer that takes the ended minigame
   */
  public void addEndListener(@NotNull Consumer<Minigame> listener) {
    Objects.requireNonNull(listener, "Listener cannot be null");
    endListeners.add(listener);
  }

  /**
   * Set whether this minigame should automatically loop (restart) after ending.
   *
   * @param shouldLoop true to enable looping
   */
  public void setShouldLoop(boolean shouldLoop) {
    this.shouldLoop = shouldLoop;
  }

  /**
   * Set the delay (in ticks) before restarting the minigame if looping is enabled.
   *
   * @param delayTicks Delay in server ticks (20 ticks = 1 second)
   */
  public void setLoopDelayTicks(long delayTicks) {
    this.loopDelayTicks = delayTicks;
  }

  /**
   * Get whether this minigame is set to loop.
   */
  public boolean isShouldLoop() {
    return shouldLoop;
  }

  /**
   * Get the loop delay in ticks.
   */
  public long getLoopDelayTicks() {
    return loopDelayTicks;
  }

  /**
   * Get a player by UUID with caching for performance.
   *
   * @param uuid Player UUID
   * @return Player instance or null if not online
   */
  protected @Nullable Player getPlayerById(@NotNull UUID uuid) {
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
   * Validate if a player is in a valid state for this minigame.
   *
   * @param player Player to validate
   * @return true if player is valid and can participate
   */
  protected boolean isPlayerValid(@Nullable Player player) {
    return player != null && player.isOnline() && players.contains(player.getUniqueId());
  }

  /**
   * Clean up expired entries from player cache.
   * Should be called periodically to prevent memory leaks.
   */
  public void cleanupPlayerCache() {
    playerCache.entrySet().removeIf(entry -> {
      Player player = entry.getValue();
      boolean shouldRemove = player == null || !player.isOnline() || !this.players.contains(entry.getKey());

      if (shouldRemove && player != null) {
        this.getManager().getPlugin().getLogger().fine(
            "Cleaning up cached player: " + player.getName() + " from minigame " + this.getId()
        );
      }
      return shouldRemove;
    });
  }

  protected void refreshAllOnlinePlayers() {
    for (UUID uuid : new HashSet<>(this.players)) {
      Player player = this.getPlayerById(uuid);
      if (player == null || !player.isOnline()) {
        // Player is offline, remove them
        this.players.remove(uuid);
        this.playerCache.remove(uuid);
      } else {
        // Refresh cache
        this.playerCache.put(uuid, player);
      }
    }
  }

  protected void onStateChange(MinigameState oldState, MinigameState newState) {
    this.getManager().getPlugin().getLogger().fine(
        "Minigame " + this.getId() + " state changed from " + oldState + " to " + newState
    );
  }

  public List<Player> getValidOnlinePlayers() {
    List<Player> validPlayers = new ArrayList<>();

    for (UUID uuid : new HashSet<>(this.players)) {
      Player player = this.getPlayerById(uuid);
      if (player != null && player.isOnline()) {
        validPlayers.add(player);
      } else {
        // Remove invalid players
        this.players.remove(uuid);
        this.playerCache.remove(uuid);
      }
    }

    return validPlayers;
  }

  public void validateGameState() {
    // Remove offline players
    this.refreshAllOnlinePlayers();

    // Check if game should end due to player count
    if (this.state == MinigameState.RUNNING && this.players.size() < this.minPlayers) {
      this.getManager().getPlugin().getLogger().warning(
          "Minigame " + this.getId() + " has too few players (" + this.players.size() +
              "/" + this.minPlayers + "), ending game"
      );
      this.end();
    }
  }

  public void performHealthCheck() {
    this.validateGameState();
    this.cleanupPlayerCache();
  }

  // Abstract methods that minigames must implement

  /**
   * Called when the minigame starts.
   * Override this method and call super.onStart() to maintain base functionality.
   */
  protected void onStart() {
    getManager().getPlugin().getLogger().info("Minigame " + getId() + " started with " + getPlayerCount() + " players");
  }

  /**
   * Called when the minigame ends.
   * Override this method and call super.onEnd() to maintain base functionality.
   */
  protected void onEnd() {
    cleanupPlayerCache();
    refreshAllOnlinePlayers();

    getManager().getPlugin().getLogger().info("Minigame " + getId() + " ended");
  }

  /**
   * Called when a player joins the minigame.
   * Override this method and call super.onPlayerJoin(player) to maintain base functionality.
   *
   * @param player Player who joined
   */
  protected void onPlayerJoin(@NotNull Player player) {
    playerCache.put(player.getUniqueId(), player);
    getManager().getPlugin().getLogger().info("Player " + player.getName() + " joined minigame " + getId());
  }

  /**
   * Called when a player leaves the minigame.
   * Override this method and call super.onPlayerLeave(player) to maintain base functionality.
   *
   * @param player Player who left
   */
  protected void onPlayerLeave(@NotNull Player player) {
    onPlayerCleanup(player);
    getManager().getPlugin().getLogger().info("Player " + player.getName() + " left minigame " + getId());
  }


  protected void onPlayerCleanup(Player player) {
    playerCache.remove(player.getUniqueId());
  }

  /**
   * Called when the countdown starts.
   * Override this method and call super.onCountdownStart() to maintain base functionality.
   */
  protected void onCountdownStart() {
    getManager().getPlugin().getLogger().info("Countdown started for minigame " + getId());
  }

  // Event handlers

  @EventHandler
  public void onPlayerQuitServer(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (this.players.contains(player.getUniqueId())) {
      this.getManager().getPlugin().getLogger().info(
          "Player " + player.getName() + " quit server while in minigame " + this.getId()
      );

      // Schedule removal on next tick to avoid concurrent modification
      Bukkit.getScheduler().runTask(this.getManager().getPlugin(), () -> {
        this.removePlayer(player);
      });
    }
  }

  // Getters and Setters

  public @NotNull String getId() {
    return id;
  }

  public @NotNull String getDisplayName() {
    return displayName;
  }

  public @NotNull MinigameManager getManager() {
    return manager;
  }

  public @NotNull Set<UUID> getPlayers() {
    return Collections.unmodifiableSet(players);
  }

  public int getPlayerCount() {
    return players.size();
  }

  public @NotNull MinigameState getState() {
    return state;
  }


  public void setState(@NotNull MinigameState state) {
    MinigameState oldState = this.state;
    this.state = Objects.requireNonNull(state, "State cannot be null");

    // Call state change hook
    this.onStateChange(oldState, state);
  }

  public @NotNull Optional<Location> getSpawnPoint() {
    return spawnPoint;
  }

  public void setSpawnPoint(@Nullable Location spawnPoint) {
    this.spawnPoint = Optional.ofNullable(spawnPoint);
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

  protected boolean shouldAutoRemoveOnQuit(Player player) {
    return true; // Default behavior - can be overridden
  }

  protected boolean shouldAutoStart() {
    return true;
  }

  public boolean isAllowJoinDuringGame() {
    return allowJoinDuringGame;
  }

  public void setAllowJoinDuringGame(boolean allowJoinDuringGame) {
    this.allowJoinDuringGame = allowJoinDuringGame;
  }
}




