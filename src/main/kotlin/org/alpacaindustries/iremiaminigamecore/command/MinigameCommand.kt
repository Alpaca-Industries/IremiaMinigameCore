package org.alpacaindustries.iremiaminigamecore.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.alpacaindustries.iremiaminigamecore.IremiaMinigameCorePlugin
import org.alpacaindustries.iremiaminigamecore.minigame.MinigameManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Modern Kotlin-based minigame command handler with type-safe command routing.
 *
 * Features:
 * - Sealed class command routing for type safety
 * - Extension functions for cleaner code
 * - Result-based error handling
 * - Improved tab completion
 * - Reduced boilerplate code
 */
class MinigameCommand(private val plugin: IremiaMinigameCorePlugin) : CommandExecutor, TabCompleter {

    private val minigameManager: MinigameManager = plugin.minigameManager!!

    companion object {
        val PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("Minigame", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
    }

    init {
        plugin.getCommand("minigame")?.let { command ->
            command.setExecutor(this)
            command.setTabCompleter(this)
        } ?: plugin.logger.warning("Failed to register minigame command - command not found in plugin.yml")
    }

    /**
     * Sealed class representing all possible minigame subcommands.
     * This provides type safety and exhaustive when expressions.
     */
    sealed class MinigameSubCommand {
        data class Join(val gameId: String) : MinigameSubCommand()
        object Leave : MinigameSubCommand()
        data class Create(val type: String, val id: String) : MinigameSubCommand()
        object List : MinigameSubCommand()
        data class Start(val gameId: String) : MinigameSubCommand()
        data class End(val gameId: String) : MinigameSubCommand()
        data class SetMin(val gameId: String, val minPlayers: Int) : MinigameSubCommand()
        data class SetMax(val gameId: String, val maxPlayers: Int) : MinigameSubCommand()
        data class SetSpawn(val gameId: String) : MinigameSubCommand()
        object Help : MinigameSubCommand()
        data class Unknown(val command: String) : MinigameSubCommand()
    }

    /**
     * Result type for command execution.
     */
    sealed class CommandResult {
        object Success : CommandResult()
        data class Error(val message: Component) : CommandResult()
        data class Usage(val usage: String) : CommandResult()
    }

    /**
     * Permission constants for cleaner code.
     */
    object Permissions {
        const val CREATE = "iremiaminigame.command.create"
        const val START = "iremiaminigame.command.start"
        const val END = "iremiaminigame.command.end"
        const val SET_MIN = "iremiaminigame.command.setmin"
        const val SET_MAX = "iremiaminigame.command.setmax"
        const val SET_SPAWN = "iremiaminigame.command.setspawn"
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        return try {
            val subCommand = parseCommand(args)
            val result = executeCommand(sender, subCommand)
            handleResult(sender, result)
            true
        } catch (e: Exception) {
            sender.sendPrefixed(Component.text("An error occurred: ${e.message}", NamedTextColor.RED))
            plugin.logger.warning("Error in command execution: ${e.message}")
            true
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> getAvailableCommands(sender).filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> getSecondArgCompletions(args[0], args[1])
            3 -> getThirdArgCompletions(args[0], args[1], args[2])
            else -> emptyList()
        }
    }

    /**
     * Parse command arguments into a type-safe command object.
     */
    private fun parseCommand(args: Array<String>): MinigameSubCommand {
        return when (val subCommand = args[0].lowercase()) {
            "join" -> if (args.size >= 2) MinigameSubCommand.Join(args[1]) else MinigameSubCommand.Unknown(subCommand)
            "leave" -> MinigameSubCommand.Leave
            "create" -> if (args.size >= 3) MinigameSubCommand.Create(args[1], args[2]) else MinigameSubCommand.Unknown(subCommand)
            "list" -> MinigameSubCommand.List
            "start" -> if (args.size >= 2) MinigameSubCommand.Start(args[1]) else MinigameSubCommand.Unknown(subCommand)
            "end" -> if (args.size >= 2) MinigameSubCommand.End(args[1]) else MinigameSubCommand.Unknown(subCommand)
            "setmin" -> if (args.size >= 3) {
                args[2].toIntOrNull()?.let { MinigameSubCommand.SetMin(args[1], it) }
                    ?: MinigameSubCommand.Unknown(subCommand)
            } else MinigameSubCommand.Unknown(subCommand)
            "setmax" -> if (args.size >= 3) {
                args[2].toIntOrNull()?.let { MinigameSubCommand.SetMax(args[1], it) }
                    ?: MinigameSubCommand.Unknown(subCommand)
            } else MinigameSubCommand.Unknown(subCommand)
            "setspawn" -> if (args.size >= 2) MinigameSubCommand.SetSpawn(args[1]) else MinigameSubCommand.Unknown(subCommand)
            "help" -> MinigameSubCommand.Help
            else -> MinigameSubCommand.Unknown(subCommand)
        }
    }

    /**
     * Execute a parsed command and return the result.
     */
    private fun executeCommand(sender: CommandSender, command: MinigameSubCommand): CommandResult {
        return when (command) {
            is MinigameSubCommand.Join -> handleJoin(sender, command.gameId)
            is MinigameSubCommand.Leave -> handleLeave(sender)
            is MinigameSubCommand.Create -> handleCreate(sender, command.type, command.id)
            is MinigameSubCommand.List -> handleList(sender)
            is MinigameSubCommand.Start -> handleStart(sender, command.gameId)
            is MinigameSubCommand.End -> handleEnd(sender, command.gameId)
            is MinigameSubCommand.SetMin -> handleSetMin(sender, command.gameId, command.minPlayers)
            is MinigameSubCommand.SetMax -> handleSetMax(sender, command.gameId, command.maxPlayers)
            is MinigameSubCommand.SetSpawn -> handleSetSpawn(sender, command.gameId)
            is MinigameSubCommand.Help -> { showHelp(sender); CommandResult.Success }
            is MinigameSubCommand.Unknown -> handleUnknown(command.command)
        }
    }

    /**
     * Handle command execution results.
     */
    private fun handleResult(sender: CommandSender, result: CommandResult) {
        when (result) {
            is CommandResult.Success -> { /* Already handled in command methods */ }
            is CommandResult.Error -> sender.sendPrefixed(result.message)
            is CommandResult.Usage -> sender.sendPrefixed(Component.text(result.usage, NamedTextColor.RED))
        }
    }

    // Command Handlers

    private fun handleJoin(sender: CommandSender, gameId: String): CommandResult {
        val player = sender.requirePlayer() ?: return CommandResult.Error(
            Component.text("Only players can join games.", NamedTextColor.RED)
        )

        return if (minigameManager.addPlayerToGame(player, gameId)) {
            player.sendPrefixed(
                Component.text("You joined the game: ", NamedTextColor.GREEN)
                    .append(Component.text(gameId, NamedTextColor.GOLD))
            )
            CommandResult.Success
        } else {
            CommandResult.Error(Component.text("Failed to join the game.", NamedTextColor.RED))
        }
    }

    private fun handleLeave(sender: CommandSender): CommandResult {
        val player = sender.requirePlayer() ?: return CommandResult.Error(
            Component.text("Only players can leave games.", NamedTextColor.RED)
        )

        return if (minigameManager.removePlayerFromGame(player)) {
            player.sendPrefixed(Component.text("You left the game.", NamedTextColor.GREEN))
            CommandResult.Success
        } else {
            CommandResult.Error(Component.text("You are not in a game.", NamedTextColor.RED))
        }
    }

    private fun handleCreate(sender: CommandSender, gameType: String, gameId: String): CommandResult {
        if (!sender.requirePermission(Permissions.CREATE)) {
            return CommandResult.Error(Component.text("You don't have permission to create minigames.", NamedTextColor.RED))
        }

        val minigame = minigameManager.createMinigame(gameType, gameId)
        return if (minigame != null) {
            sender.sendPrefixed(
                Component.text("Created game: ", NamedTextColor.GREEN)
                    .append(Component.text("${minigame.displayName} ($gameId)", NamedTextColor.GOLD))
            )
            CommandResult.Success
        } else {
            CommandResult.Error(Component.text("Failed to create game.", NamedTextColor.RED))
        }
    }

    private fun handleList(sender: CommandSender): CommandResult {
        val games = minigameManager.getActiveGames()

        if (games.isEmpty()) {
            sender.sendPrefixed(Component.text("There are no active games.", NamedTextColor.YELLOW))
            return CommandResult.Success
        }

        sender.sendPrefixed(Component.text("Active Games:", NamedTextColor.YELLOW))
        games.forEach { (gameId, game) ->
            sender.sendMessage(
                Component.text("- ", NamedTextColor.GRAY)
                    .append(Component.text(gameId, NamedTextColor.GOLD))
                    .append(Component.text(" (${game.displayName}): ", NamedTextColor.YELLOW))
                    .append(Component.text("${game.playerCount} players, ", NamedTextColor.WHITE))
                    .append(Component.text("Status: ${game.state}", NamedTextColor.AQUA))
            )
        }

        return CommandResult.Success
    }

    private fun handleStart(sender: CommandSender, gameId: String): CommandResult {
        if (!sender.requirePermission(Permissions.START)) {
            return CommandResult.Error(Component.text("You don't have permission to start minigames.", NamedTextColor.RED))
        }

        val game = minigameManager.getActiveGames()[gameId] ?: return CommandResult.Error(
            Component.text("Game not found: $gameId", NamedTextColor.RED)
        )

        game.startCountdown()
        sender.sendPrefixed(
            Component.text("Starting countdown for game: ", NamedTextColor.GREEN)
                .append(Component.text(gameId, NamedTextColor.GOLD))
        )

        return CommandResult.Success
    }

    private fun handleEnd(sender: CommandSender, gameId: String): CommandResult {
        if (!sender.requirePermission(Permissions.END)) {
            return CommandResult.Error(Component.text("You don't have permission to end minigames.", NamedTextColor.RED))
        }

        val game = minigameManager.getActiveGames()[gameId] ?: return CommandResult.Error(
            Component.text("Game not found: $gameId", NamedTextColor.RED)
        )

        game.end()
        sender.sendPrefixed(
            Component.text("Ended game: ", NamedTextColor.GREEN)
                .append(Component.text(gameId, NamedTextColor.GOLD))
        )

        return CommandResult.Success
    }

    private fun handleSetMin(sender: CommandSender, gameId: String, minPlayers: Int): CommandResult {
        if (!sender.requirePermission(Permissions.SET_MIN)) {
            return CommandResult.Error(Component.text("You don't have permission to modify minigames.", NamedTextColor.RED))
        }

        val game = minigameManager.getActiveGames()[gameId] ?: return CommandResult.Error(
            Component.text("Game not found: $gameId", NamedTextColor.RED)
        )

        return try {
            game.minPlayers = minPlayers
            sender.sendPrefixed(
                Component.text("Set minimum players to $minPlayers for game: ", NamedTextColor.GREEN)
                    .append(Component.text(gameId, NamedTextColor.GOLD))
            )
            CommandResult.Success
        } catch (e: IllegalArgumentException) {
            CommandResult.Error(Component.text("Invalid value: ${e.message}", NamedTextColor.RED))
        }
    }

    private fun handleSetMax(sender: CommandSender, gameId: String, maxPlayers: Int): CommandResult {
        if (!sender.requirePermission(Permissions.SET_MAX)) {
            return CommandResult.Error(Component.text("You don't have permission to modify minigames.", NamedTextColor.RED))
        }

        val game = minigameManager.getActiveGames()[gameId] ?: return CommandResult.Error(
            Component.text("Game not found: $gameId", NamedTextColor.RED)
        )

        return try {
            game.maxPlayers = maxPlayers
            sender.sendPrefixed(
                Component.text("Set maximum players to $maxPlayers for game: ", NamedTextColor.GREEN)
                    .append(Component.text(gameId, NamedTextColor.GOLD))
            )
            CommandResult.Success
        } catch (e: IllegalArgumentException) {
            CommandResult.Error(Component.text("Invalid value: ${e.message}", NamedTextColor.RED))
        }
    }

    private fun handleSetSpawn(sender: CommandSender, gameId: String): CommandResult {
        if (!sender.requirePermission(Permissions.SET_SPAWN)) {
            return CommandResult.Error(Component.text("You don't have permission to modify minigames.", NamedTextColor.RED))
        }

        val player = sender.requirePlayer() ?: return CommandResult.Error(
            Component.text("Only players can set spawn points.", NamedTextColor.RED)
        )

        val game = minigameManager.getActiveGames()[gameId] ?: return CommandResult.Error(
            Component.text("Game not found: $gameId", NamedTextColor.RED)
        )

        game.spawnPoint = player.location
        sender.sendPrefixed(
            Component.text("Set spawn point for game: ", NamedTextColor.GREEN)
                .append(Component.text(gameId, NamedTextColor.GOLD))
        )

        return CommandResult.Success
    }

    private fun handleUnknown(command: String): CommandResult {
        return CommandResult.Error(Component.text("Unknown command: $command", NamedTextColor.RED))
    }

    // Tab Completion Helpers

    private fun getAvailableCommands(sender: CommandSender): List<String> {
        val commands = mutableListOf("join", "leave", "list", "help")

        if (sender.hasPermission(Permissions.CREATE)) commands.add("create")
        if (sender.hasPermission(Permissions.START)) commands.add("start")
        if (sender.hasPermission(Permissions.END)) commands.add("end")
        if (sender.hasPermission(Permissions.SET_MIN)) commands.add("setmin")
        if (sender.hasPermission(Permissions.SET_MAX)) commands.add("setmax")
        if (sender.hasPermission(Permissions.SET_SPAWN)) commands.add("setspawn")

        return commands
    }

    private fun getSecondArgCompletions(firstArg: String, prefix: String): List<String> {
        return when (firstArg.lowercase()) {
            "join", "start", "end", "setmin", "setmax", "setspawn" -> {
                minigameManager.getActiveGames().keys.filter { it.startsWith(prefix, ignoreCase = true) }
            }
            "create" -> {
                minigameManager.getMinigameTypes().filter { it.startsWith(prefix, ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    private fun getThirdArgCompletions(firstArg: String, secondArg: String, prefix: String): List<String> {
        return when (firstArg.lowercase()) {
            "create" -> {
                listOf("game1", "game2", "custom", "${secondArg}_1")
                    .filter { it.startsWith(prefix, ignoreCase = true) }
            }
            "setmin", "setmax" -> {
                listOf("1", "2", "4", "8", "16", "32")
                    .filter { it.startsWith(prefix, ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    // Help System

    private fun showHelp(sender: CommandSender) {
        sender.sendPrefixed(Component.text("Available Commands:", NamedTextColor.YELLOW))

        val helpEntries = listOf(
            "/minigame join <gameId>" to "Join a minigame",
            "/minigame leave" to "Leave your current minigame",
            "/minigame list" to "List all active minigames"
        ).toMutableList()

        if (sender.hasPermission(Permissions.CREATE)) {
            helpEntries.add("/minigame create <type> <id>" to "Create a new minigame")
        }
        if (sender.hasPermission(Permissions.START)) {
            helpEntries.add("/minigame start <gameId>" to "Start a minigame countdown")
        }
        if (sender.hasPermission(Permissions.END)) {
            helpEntries.add("/minigame end <gameId>" to "End a minigame")
        }
        if (sender.hasPermission(Permissions.SET_MIN)) {
            helpEntries.add("/minigame setmin <gameId> <count>" to "Set minimum players")
        }
        if (sender.hasPermission(Permissions.SET_MAX)) {
            helpEntries.add("/minigame setmax <gameId> <count>" to "Set maximum players")
        }
        if (sender.hasPermission(Permissions.SET_SPAWN)) {
            helpEntries.add("/minigame setspawn <gameId>" to "Set spawn point to your location")
        }

        helpEntries.forEach { (command, description) ->
            sender.sendMessage(
                Component.text("  $command", NamedTextColor.GOLD)
                    .append(Component.text(" - $description", NamedTextColor.GRAY))
            )
        }
    }
}

// Extension Functions for Cleaner Code

/**
 * Require that the CommandSender is a Player, returning null with error message if not.
 */
private fun CommandSender.requirePlayer(): Player? = this as? Player

/**
 * Send a message with the command prefix.
 */
private fun CommandSender.sendPrefixed(message: Component) {
    sendMessage(MinigameCommand.PREFIX.append(message))
}

/**
 * Check permission and send error message if not granted.
 */
private fun CommandSender.requirePermission(permission: String): Boolean {
    return hasPermission(permission).also {
        if (!it) sendPrefixed(Component.text("You don't have permission.", NamedTextColor.RED))
    }
}
