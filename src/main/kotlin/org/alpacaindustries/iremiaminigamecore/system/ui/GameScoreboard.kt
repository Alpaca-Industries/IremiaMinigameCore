package org.alpacaindustries.iremiaminigamecore.system.ui;

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages game-specific scoreboards with automatic player state preservation.
 *
 * Features:
 * - Thread-safe operations
 * - Automatic cleanup on player disconnect
 * - Line-based and team-based content management
 * - Adventure Component support with legacy fallback
 * - Fluent API for easy chaining
 *
 * @param title The title component to display at the top of the scoreboard
 */
class GameScoreboard(title: Component) {

    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
    private val objective: Objective = scoreboard.registerNewObjective("game", Criteria.DUMMY, title)

    // Thread-safe collections for concurrent access
    private val lines = ConcurrentHashMap<Int, String>()
    private val previousScoreboards = ConcurrentHashMap<UUID, Scoreboard>()
    private val playerTeams = ConcurrentHashMap<UUID, Set<String>>()

    init {
        objective.displaySlot = DisplaySlot.SIDEBAR
    }

    companion object {
        private const val MAX_LINES = 15
        private const val MIN_LINES = 1

        /**
         * Creates a GameScoreboard with a legacy text title
         * @param title The title using legacy color codes (&)
         */
        fun withLegacyTitle(title: String): GameScoreboard {
            return GameScoreboard(LegacyComponentSerializer.legacyAmpersand().deserialize(title))
        }

        /**
         * Creates a GameScoreboard with a plain text title
         * @param title The plain text title
         */
        fun withPlainTitle(title: String): GameScoreboard {
            return GameScoreboard(Component.text(title))
        }
    }

    /**
     * Sets a line on the scoreboard using Adventure Component.
     *
     * @param line The line number (1-15, where 1 is at the bottom)
     * @param component The component to display on this line
     * @return This scoreboard for method chaining
     * @throws IllegalArgumentException if line is not between 1 and 15
     */
    fun setLine(line: Int, component: Component): GameScoreboard {
        require(line in MIN_LINES..MAX_LINES) {
            "Line must be between $MIN_LINES and $MAX_LINES, got $line"
        }

        // Remove existing line if it exists
        lines[line]?.let { entryId ->
            scoreboard.resetScores(entryId)
        }

        // Create a unique entry ID for this line
        val entryId = generateEntryId(line)
        lines[line] = entryId

        // Get or create the team for this line
        val teamName = "line_$line"
        val team = getOrCreateTeam(teamName)

        // Clear existing entries and add our new one
        team.entries.toList().forEach { team.removeEntry(it) }
        team.addEntry(entryId)

        // Set the component text as prefix
        team.prefix(component)
        team.suffix(Component.empty())

        // Set the score (higher scores appear at the top)
        objective.getScore(entryId).score = line

        return this
    }

    /**
     * Sets a line using legacy text format.
     *
     * @param line The line number (1-15)
     * @param text The text with legacy color codes (&)
     * @return This scoreboard for method chaining
     */
    fun setLineLegacy(line: Int, text: String): GameScoreboard {
        return setLine(line, LegacyComponentSerializer.legacyAmpersand().deserialize(text))
    }

    /**
     * Sets a line using plain text.
     *
     * @param line The line number (1-15)
     * @param text The plain text
     * @param color Optional color for the text
     * @return This scoreboard for method chaining
     */
    fun setLinePlain(line: Int, text: String, color: NamedTextColor? = null): GameScoreboard {
        val component = if (color != null) {
            Component.text(text, color)
        } else {
            Component.text(text)
        }
        return setLine(line, component)
    }

    /**
     * Removes a line from the scoreboard.
     *
     * @param line The line number to remove
     * @return This scoreboard for method chaining
     */
    fun removeLine(line: Int): GameScoreboard {
        lines[line]?.let { entryId ->
            scoreboard.resetScores(entryId)
            val teamName = "line_$line"
            scoreboard.getTeam(teamName)?.unregister()
            lines.remove(line)
        }
        return this
    }

    /**
     * Clears all lines from the scoreboard.
     *
     * @return This scoreboard for method chaining
     */
    fun clearLines(): GameScoreboard {
        lines.keys.toList().forEach { removeLine(it) }
        return this
    }

    /**
     * Updates a specific team's prefix and suffix.
     * This is useful for updating dynamic content without flickering.
     *
     * @param teamName Team name
     * @param prefix New prefix component
     * @param suffix New suffix component (optional)
     * @return This scoreboard for method chaining
     */
    fun updateTeam(
        teamName: String,
        prefix: Component,
        suffix: Component = Component.empty()
    ): GameScoreboard {
        val team = getOrCreateTeam(teamName)

        // Ensure the team has an entry if it doesn't already
        if (team.entries.isEmpty()) {
            team.addEntry(teamName)
        }

        team.prefix(prefix)
        team.suffix(suffix)

        return this
    }

    /**
     * Updates a team using legacy text format.
     *
     * @param teamName Team name
     * @param prefix New prefix with legacy color codes
     * @param suffix New suffix with legacy color codes (optional)
     * @return This scoreboard for method chaining
     */
    fun updateTeamLegacy(
        teamName: String,
        prefix: String,
        suffix: String = ""
    ): GameScoreboard {
        return updateTeam(
            teamName,
            LegacyComponentSerializer.legacyAmpersand().deserialize(prefix),
            LegacyComponentSerializer.legacyAmpersand().deserialize(suffix)
        )
    }

    /**
     * Creates a team for player-specific content.
     *
     * @param player The player
     * @param teamName Custom team name (optional, defaults to player UUID)
     * @param prefix Team prefix
     * @param suffix Team suffix (optional)
     * @return This scoreboard for method chaining
     */
    fun createPlayerTeam(
        player: Player,
        teamName: String = "player_${player.uniqueId}",
        prefix: Component = Component.empty(),
        suffix: Component = Component.empty()
    ): GameScoreboard {
        val team = getOrCreateTeam(teamName)
        team.addEntry(player.name)
        team.prefix(prefix)
        team.suffix(suffix)

        // Track teams for this player for cleanup
        playerTeams.compute(player.uniqueId) { _, existing ->
            (existing ?: emptySet()) + teamName
        }

        return this
    }

    /**
     * Shows this scoreboard to a player, preserving their previous scoreboard.
     *
     * @param player Player to show the scoreboard to
     */
    fun showTo(player: Player) {
        // Store their previous scoreboard for restoration
        previousScoreboards[player.uniqueId] = player.scoreboard
        player.scoreboard = scoreboard
    }

    /**
     * Shows this scoreboard to multiple players.
     *
     * @param players Collection of players
     */
    fun showTo(players: Collection<Player>) {
        players.forEach { showTo(it) }
    }

    /**
     * Hides this scoreboard from a player, restoring their previous one.
     *
     * @param player Player to hide the scoreboard from
     */
    fun hideFrom(player: Player) {
        val previous = previousScoreboards.remove(player.uniqueId)
        player.scoreboard = previous ?: Bukkit.getScoreboardManager()!!.mainScoreboard

        // Clean up player-specific teams
        cleanupPlayerTeams(player)
    }

    /**
     * Hides this scoreboard from multiple players.
     *
     * @param players Collection of players
     */
    fun hideFrom(players: Collection<Player>) {
        players.forEach { hideFrom(it) }
    }

    /**
     * Checks if a player is currently viewing this scoreboard.
     *
     * @param player The player to check
     * @return true if the player is viewing this scoreboard
     */
    fun isShownTo(player: Player): Boolean {
        return player.scoreboard == scoreboard
    }

    /**
     * Gets all players currently viewing this scoreboard.
     *
     * @return Set of players viewing this scoreboard
     */
    fun getViewers(): Set<Player> {
        return Bukkit.getOnlinePlayers().filter { it.scoreboard == scoreboard }.toSet()
    }

    /**
     * Updates the scoreboard title.
     *
     * @param title New title component
     * @return This scoreboard for method chaining
     */
    fun updateTitle(title: Component): GameScoreboard {
        objective.displayName(title)
        return this
    }

    /**
     * Updates the scoreboard title using legacy text.
     *
     * @param title New title with legacy color codes
     * @return This scoreboard for method chaining
     */
    fun updateTitleLegacy(title: String): GameScoreboard {
        return updateTitle(LegacyComponentSerializer.legacyAmpersand().deserialize(title))
    }

    /**
     * Cleans up all resources and hides the scoreboard from all viewers.
     */
    fun cleanup() {
        // Hide from all current viewers
        getViewers().forEach { hideFrom(it) }

        // Clear all data structures
        lines.clear()
        previousScoreboards.clear()
        playerTeams.clear()

        // Unregister the objective
        objective.unregister()
    }

    /**
     * Gets the underlying Bukkit scoreboard.
     * Use with caution as direct modifications may break the wrapper's functionality.
     *
     * @return The underlying Bukkit scoreboard
     */
    val bukkitScoreboard: Scoreboard
        get() = scoreboard

    // Private helper methods

    private fun generateEntryId(line: Int): String {
        // Use a combination of color codes to create unique, invisible entries
        val hexChar = Integer.toHexString(line).lowercase()
        return "§$hexChar§r"
    }

    private fun getOrCreateTeam(teamName: String): Team {
        return scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
    }

    private fun cleanupPlayerTeams(player: Player) {
        playerTeams.remove(player.uniqueId)?.forEach { teamName ->
            scoreboard.getTeam(teamName)?.let { team ->
                team.removeEntry(player.name)
                // Remove team if it has no more entries
                if (team.entries.isEmpty()) {
                    team.unregister()
                }
            }
        }
    }
}
