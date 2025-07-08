package org.alpacaindustries.iremiaminigame.system.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages game-specific scoreboards with automatic player state preservation
 * Handles line management, team-based text display, and cleanup on player
 * disconnect
 */
public class GameScoreboard {
  private final Scoreboard scoreboard;
  private final Objective objective;
  private final Map<Integer, String> lines = new HashMap<>();
  private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>(); // Restore player's original scoreboard

  public GameScoreboard(Component title) {
    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    objective = scoreboard.registerNewObjective("game", Criteria.DUMMY, title);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
  }

  /**
   * Set a line on the scoreboard using Adventure Component
   *
   * @param line      The line number (1-15, 1 being at the bottom)
   * @param component The component to display on this line
   * @return This scoreboard for chaining
   */
  /**
   * Array of color codes that can be used as invisible entry IDs
   * Using color codes makes them not visible in the scoreboard
   */
  private static final String[] COLOR_CODES = {
      "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9",
      "§a", "§b", "§c", "§d", "§e", "§f", "§r"
  };

  public GameScoreboard setLine(int line, Component component) {
    if (line < 1 || line > 15) {
      throw new IllegalArgumentException("Line must be between 1 and 15");
    }

    // Remove existing line if it exists
    if (lines.containsKey(line)) {
      scoreboard.resetScores(lines.get(line));
    }

    // Create an entry ID using the line number
    String entryId = "§" + Integer.toHexString(line); // Using hex color code as an invisible prefix

    // For lines > 15, we need to add extra characters to make unique ids
    if (line > 15) {
      entryId += "§r"; // Reset code as a spacer
    }

    lines.put(line, entryId);

    // Get or create the team for this line
    String teamName = "line_" + line;
    Team team = scoreboard.getTeam(teamName);
    if (team == null) {
      team = scoreboard.registerNewTeam(teamName);
    } else {
      // Clear any existing entries
      for (String entry : team.getEntries()) {
        team.removeEntry(entry);
      }
    }

    // Add our entry to the team
    team.addEntry(entryId);

    // Set the component text
    team.prefix(component);

    // Optionally add empty suffix to ensure consistent appearance
    team.suffix(Component.empty());

    // Set the score in the scoreboard
    objective.getScore(entryId).setScore(line);

    return this;
  }

  /**
   * Update a specific team's prefix/suffix
   * This is useful for updating values without flickering the scoreboard
   *
   * @param team   Team name
   * @param prefix New prefix for the team as a Component
   * @param suffix New suffix for the team as a Component
   * @return This scoreboard for chaining
   */
  public GameScoreboard updateTeam(String team, Component prefix, Component suffix) {
    Team scoreboardTeam = scoreboard.getTeam(team);
    if (scoreboardTeam == null) {
      scoreboardTeam = scoreboard.registerNewTeam(team);
      scoreboardTeam.addEntry(team);
    }
    scoreboardTeam.prefix(prefix);
    scoreboardTeam.suffix(suffix);

    return this;
  }

  /**
   * Update a specific team's prefix/suffix using legacy text format
   * This is useful for updating values without flickering the scoreboard
   *
   * @param team   Team name
   * @param prefix New prefix for the team (can include color codes with &)
   * @param suffix New suffix for the team (can include color codes with &)
   * @return This scoreboard for chaining
   */
  public GameScoreboard updateTeamLegacy(String team, String prefix, String suffix) {
    return updateTeam(
        team,
        LegacyComponentSerializer.legacyAmpersand().deserialize(prefix),
        LegacyComponentSerializer.legacyAmpersand().deserialize(suffix));
  }

  /**
   * Get the configured score for a team entry
   * Override this in subclasses if you want to customize the order
   *
   * @param team Team name
   * @return Score value for this team
   */
  protected int getTeamScore(String team) {
    // Default implementation uses the hashCode to get a "random" but consistent
    // value
    return Math.abs(team.hashCode()) % 15 + 1;
  }

  /**
   * Show this scoreboard to a player
   *
   * @param player Player to show the scoreboard to
   */
  public void showTo(Player player) {
    // Store their previous scoreboard
    previousScoreboards.put(player.getUniqueId(), player.getScoreboard());

    // Set our scoreboard
    player.setScoreboard(scoreboard);
  }

  /**
   * Hide this scoreboard from a player, restoring their previous one
   *
   * @param player Player to hide the scoreboard from
   */
  public void hideFrom(Player player) {
    // Restore their previous scoreboard if we have it
    Scoreboard previous = previousScoreboards.get(player.getUniqueId());
    if (previous != null) {
      player.setScoreboard(previous);
    } else {
      // Otherwise set them to the main scoreboard
      player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    previousScoreboards.remove(player.getUniqueId());
  }

  /**
   * Get the underlying Bukkit scoreboard
   *
   * @return The Bukkit scoreboard
   */
  public Scoreboard getScoreboard() {
    return scoreboard;
  }
}
