package org.alpacaindustries.iremiaminigamecore.api.impl;

import org.alpacaindustries.iremiaminigamecore.minigame.Minigame;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager;
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

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
    if (delegate.getSpawnPoint() != null) {
      setSpawnPoint(delegate.getSpawnPoint());
    }
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
  public boolean addPlayer(Player player) {
    boolean result = delegate.addPlayer(player);
    if (result) {
      api.notifyListeners("playerJoin", player, this);
    }
    return result;
  }

  @Override
  public void removePlayer(Player player) {
    delegate.removePlayer(player);
    api.notifyListeners("playerLeave", player, this);
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
  protected void onPlayerJoin(Player player) {
    // This will be called by the delegate
  }

  @Override
  protected void onPlayerLeave(Player player) {
    // This will be called by the delegate
  }

  @Override
  protected void onCountdownStart() {
    // This will be called by the delegate
  }

  // Delegate getters and simple methods
  @Override
  public Set<UUID> getPlayers() {
    return delegate.getPlayers();
  }

  @Override
  public int getPlayerCount() {
    return delegate.getPlayerCount();
  }

  @Override
  public MinigameState getState() {
    return delegate.getState();
  }

  @Override
  public void setState(MinigameState state) {
    delegate.setState(state);
  }

  @Override
  public Location getSpawnPoint() {
    return delegate.getSpawnPoint();
  }

  @Override
  public void setSpawnPoint(Location spawnPoint) {
    delegate.setSpawnPoint(spawnPoint);
    super.setSpawnPoint(spawnPoint);
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
