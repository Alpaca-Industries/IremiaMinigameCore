package org.alpacaindustries.iremiaminigamecore.minigame;

import org.alpacaindustries.iremiaminigamecore.system.ui.GameScoreboard;
import org.alpacaindustries.iremiaminigamecore.util.CountdownTimer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Abstract base class for survival-based minigames where players can be
 * eliminated
 * Provides common functionality for player elimination, countdown management,
 * and event handling
 */
public abstract class SurvivalMinigame extends Minigame {

  protected final CountdownTimer countdownTimer;
  protected final GameScoreboard scoreboard;
  public final List<UUID> alivePlayers = new ArrayList<>();

  private static final int DEFAULT_COUNTDOWN_SECONDS = 10;
  private static final int DEFAULT_Y_THRESHOLD = 70;

  protected SurvivalMinigame(String id, String displayName, MinigameManager manager) {
    this(id, displayName, manager, DEFAULT_COUNTDOWN_SECONDS);
  }

  protected SurvivalMinigame(String id, String displayName, MinigameManager manager, int countdownSeconds) {
    super(id, displayName, manager);

    this.countdownTimer = new CountdownTimer(manager.getPlugin(), countdownSeconds)
        .onCount(this::onCountdown)
        .onFinish(this::start);

    this.scoreboard = new GameScoreboard(Component.text(displayName, NamedTextColor.GOLD));
  }

  @Override
  protected void onStart() {
    super.onStart();

    alivePlayers.clear();
    for (Player player : getOnlinePlayers()) {
      alivePlayers.add(player.getUniqueId());
      preparePlayer(player);
    }

    updateScoreboard();
    broadcastGameStart();
  }

  @Override
  protected void onEnd() {
    super.onEnd();

    if (countdownTimer != null) {
      countdownTimer.stop();
    }

    handleGameEnd();
    cleanupPlayers();
  }

  @Override
  protected void onPlayerJoin(Player player) {
    super.onPlayerJoin(player);

    if (getState() == MinigameState.WAITING || getState() == MinigameState.COUNTDOWN) {
      updateWaitingScoreboard();
      scoreboard.showTo(player);
    } else if (getState() == MinigameState.RUNNING) {
      player.setGameMode(GameMode.SPECTATOR);
      scoreboard.showTo(player);
      player.sendMessage(getGamePrefix().append(Component.text("Game in progress! You are now spectating.")));
    }
  }

  @Override
  protected void onPlayerLeave(Player player) {
    super.onPlayerLeave(player);

    alivePlayers.remove(player.getUniqueId());
    scoreboard.hideFrom(player);

    player.setGameMode(GameMode.ADVENTURE);

    onPlayerEliminated(player);
    checkWinCondition();
  }

  @Override
  protected void onCountdownStart() {
    super.onCountdownStart();
    countdownTimer.start();
  }

  /**
   * Handle player elimination from the game
   */
  protected void eliminatePlayer(Player player, String reason) {
    if (!alivePlayers.contains(player.getUniqueId())) {
      return;
    }

    alivePlayers.remove(player.getUniqueId());
    player.setGameMode(GameMode.SPECTATOR);

    broadcastMessage(getGamePrefix().append(
        Component.text(player.getName(), NamedTextColor.YELLOW)
            .append(Component.text(" " + reason + "!", NamedTextColor.RED))));

    onPlayerEliminated(player);
    updateScoreboard();
    checkWinCondition();
  }

  /**
   * Check if the game should end based on remaining players
   */
  protected void checkWinCondition() {
    if (getState() != MinigameState.RUNNING) {
      return;
    }

    if (shouldEndGame()) {
      end();
    }
  }

  // Event Handlers
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (isPlayerInGame(player)) {
      eliminatePlayer(player, "died");
    }
  }

  @EventHandler
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getEntity();
    if (isPlayerInGame(player)) {
      if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
        event.setCancelled(true);
        eliminatePlayer(player, "fell into the void");
      }
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (!isPlayerInGame(player) || getState() != MinigameState.RUNNING) {
      return;
    }

    if (player.getLocation().getY() <= getYThreshold()) {
      eliminatePlayer(player, "fell off");
    }
  }

  // Helper methods
  protected boolean isPlayerInGame(Player player) {
    return getPlayers().contains(player.getUniqueId()) && alivePlayers.contains(player.getUniqueId());
  }

  protected void cleanupPlayers() {
    // Clean up ALL players who were ever in this game, not just online ones
    for (UUID playerId : getPlayers()) {
      Player player = getPlayerById(playerId);
      if (player != null && player.isOnline()) {
        // Reset game mode
        player.setGameMode(GameMode.ADVENTURE);

        // Hide scoreboard FIRST before any other cleanup
        try {
          scoreboard.hideFrom(player);
        } catch (Exception e) {
          // Fallback: Force reset to main scoreboard if hideFrom fails
          player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        // Teleport player to world spawn
        Location worldSpawn = player.getWorld().getSpawnLocation();
        player.teleport(worldSpawn);

        // Play success sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
      }
    }

    // Also clean up any remaining online players just in case
    for (Player player : getOnlinePlayers()) {
      try {
        scoreboard.hideFrom(player);
      } catch (Exception e) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
      }
    }
  }

  protected void updateWaitingScoreboard() {
    scoreboard.setLine(1, Component.text("Players: ", NamedTextColor.YELLOW)
        .append(Component.text(getPlayerCount() + "/" + getMaxPlayers(), NamedTextColor.WHITE)));
    scoreboard.setLine(2, Component.text("Status: ", NamedTextColor.YELLOW)
        .append(Component.text("Waiting for players", NamedTextColor.WHITE)));
    scoreboard.setLine(3, Component.text("Minimum: ", NamedTextColor.YELLOW)
        .append(Component.text(getMinPlayers() + " players", NamedTextColor.WHITE)));
  }

  // Abstract methods that subclasses must implement
  protected abstract Component getGamePrefix();

  protected abstract void preparePlayer(Player player);

  protected abstract void onCountdown(int secondsLeft);

  protected abstract void updateScoreboard();

  protected abstract void broadcastGameStart();

  protected abstract void handleGameEnd();

  protected abstract boolean shouldEndGame();

  protected abstract void onPlayerEliminated(Player player);

  // Optional overrides with default implementations
  protected int getYThreshold() {
    return DEFAULT_Y_THRESHOLD;
  }
}
