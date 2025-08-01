package org.alpacaindustries.iremiaminigamecore.api.impl;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Wrapper that adds event notifications to externally created minigames
 * This ensures that all minigame events are properly broadcasted to API
 * listeners
 */
class EventNotifyingMinigame extends Minigame {

  private final Minigame delegate;
  private final IremiaMinigameAPIImpl api;

  public EventNotifyingMinigame(Minigame delegate, IremiaMinigameAPIImpl api) {
    super(delegate.getId(), delegate.getDisplayName(), delegate.getManager());
    this.delegate = delegate;
    this.api = api;

    // Copy settings from delegate
    setMinPlayers(delegate.getMinPlayers());
    setMaxPlayers(delegate.getMaxPlayers());
    setAllowJoinDuringGame(delegate.isAllowJoinDuringGame());
    setSpawnPoint(delegate.getSpawnPoint());

  }

  @Override
  public void initialize() {
    delegate.initialize();
    api.notifyListeners("created", this);
  }

  @Override
  public void start() {
    delegate.start();
    api.notifyListeners("started", this);
  }

  @Override
  public void end() {
    delegate.end();
    api.notifyListeners("ended", this);
  }

  @Override
  public boolean addPlayer(@NotNull Player player) {
    boolean result = delegate.addPlayer(player);
    if (result) {
      api.notifyListeners("playerJoin", player, this);
    }
    return result;
  }

  @Override
  public boolean removePlayer(@NotNull Player player) {
    boolean result = delegate.removePlayer(player);
    api.notifyListeners("playerLeave", player, this);
    return result;
  }

  @Override
  public void startCountdown() {
    delegate.startCountdown();
    api.notifyListeners("countdownStart", this);
  }

  // Delegate all other methods to the wrapped minigame
  @Override
  protected void onStart() {
    // This will be called by the delegate
  }

  @Override
  protected void onEnd() {
    // This will be called by the delegate
  }

  @Override
  protected void onPlayerJoin(@NotNull Player player) {
    // This will be called by the delegate
  }

  @Override
  protected void onPlayerLeave(@NotNull Player player) {
    // This will be called by the delegate
  }

  @Override
  protected void onCountdownStart() {
    // This will be called by the delegate
  }

  // These methods access the delegate's properties since they are final in the parent class
  @NotNull
  public Set<UUID> getDelegatePlayes() {
    return delegate.getPlayers();
  }

  public int getDelegatePlayerCount() {
    return delegate.getPlayerCount();
  }

  @NotNull
  public MinigameState getDelegateState() {
    return delegate.getState();
  }

  @NotNull
  public Optional<Location> getDelegateSpawnPoint() {
    return Optional.ofNullable(delegate.getSpawnPoint());
  }

  // Add method to expose elimination events
  public void notifyPlayerEliminated(Player player, String reason) {
    api.notifyListeners("playerEliminated", player, this, reason);
  }

  // Expose the original minigame for advanced access
  public Minigame getDelegate() {
    return delegate;
  }
}
