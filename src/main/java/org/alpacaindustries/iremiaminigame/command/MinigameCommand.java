package org.alpacaindustries.iremiaminigame.command;

import org.alpacaindustries.iremiaminigame.IremiaMinigameCorePlugin;
import org.alpacaindustries.iremiaminigame.minigame.Minigame;
import org.alpacaindustries.iremiaminigame.minigame.MinigameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command handler for minigame commands using the traditional Bukkit API
 * This approach avoids type issues with Paper's Brigadier implementation
 */
public class MinigameCommand implements CommandExecutor, TabCompleter {
  private final IremiaMinigameCorePlugin plugin;
  private final MinigameManager minigameManager;
  private static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
      .append(Component.text("Minigame", NamedTextColor.GOLD, TextDecoration.BOLD))
      .append(Component.text("] ", NamedTextColor.DARK_GRAY));

  public MinigameCommand(IremiaMinigameCorePlugin plugin) {
    this.plugin = plugin;
    this.minigameManager = plugin.getMinigameManager();

    // Register command the traditional way
    plugin.getCommand("minigame").setExecutor(this);
    plugin.getCommand("minigame").setTabCompleter(this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      showHelp(sender);
      return true;
    }

    String subCommand = args[0].toLowerCase();

    switch (subCommand) {
      case "join":
        return handleJoinCommand(sender, args);
      case "leave":
        return handleLeaveCommand(sender, args);
      case "create":
        return handleCreateCommand(sender, args);
      case "list":
        return handleListCommand(sender, args);
      case "start":
        return handleStartCommand(sender, args);
      case "end":
        return handleEndCommand(sender, args);
      case "setmin":
        return handleSetMinCommand(sender, args);
      case "setmax":
        return handleSetMaxCommand(sender, args);
      case "setspawn":
        return handleSetSpawnCommand(sender, args);
      case "help":
      default:
        showHelp(sender);
        return true;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      // Main subcommands
      List<String> subCommands = new ArrayList<>();
      subCommands.add("join");
      subCommands.add("leave");
      subCommands.add("list");
      subCommands.add("help");

      // Add admin commands if player has permission
      if (sender.hasPermission("iremiaminigame.command.create")) {
        subCommands.add("create");
      }
      if (sender.hasPermission("iremiaminigame.command.start")) {
        subCommands.add("start");
      }
      if (sender.hasPermission("iremiaminigame.command.end")) {
        subCommands.add("end");
      }
      if (sender.hasPermission("iremiaminigame.command.setmin")) {
        subCommands.add("setmin");
      }
      if (sender.hasPermission("iremiaminigame.command.setmax")) {
        subCommands.add("setmax");
      }
      if (sender.hasPermission("iremiaminigame.command.setspawn")) {
        subCommands.add("setspawn");
      }

      return filterCompletions(subCommands, args[0]);
    }

    if (args.length == 2) {
      switch (args[0].toLowerCase()) {
        case "join":
        case "start":
        case "end":
        case "setmin":
        case "setmax":
        case "setspawn":
          // Show active games
          return filterCompletions(new ArrayList<>(minigameManager.getActiveGames().keySet()), args[1]);
        case "create":
          // Show available game types
          return filterCompletions(new ArrayList<>(minigameManager.getMinigameTypes()), args[1]);
      }
    }

    if (args.length == 3) {
      if (args[0].toLowerCase().equals("create")) {
        // For create command, suggest some default IDs
        completions.add("game1");
        completions.add("game2");
        completions.add("custom");
        completions.add(args[1] + "_1");
        return filterCompletions(completions, args[2]);
      }
    }

    return completions;
  }

  /**
   * Filter completions by prefix
   */
  private List<String> filterCompletions(List<String> possibilities, String prefix) {
    if (prefix.isEmpty()) {
      return possibilities;
    }

    prefix = prefix.toLowerCase();
    List<String> filtered = new ArrayList<>();
    for (String option : possibilities) {
      if (option.toLowerCase().startsWith(prefix)) {
        filtered.add(option);
      }
    }
    return filtered;
  }

  // Command handlers

  private boolean handleJoinCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(PREFIX.append(Component.text("Only players can join games.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 2) {
      player.sendMessage(PREFIX.append(Component.text("Usage: /minigame join <gameId>", NamedTextColor.RED)));
      return true;
    }

    String gameId = args[1];
    if (minigameManager.addPlayerToGame(player, gameId)) {
      player.sendMessage(PREFIX.append(Component.text("You joined the game: ", NamedTextColor.GREEN))
          .append(Component.text(gameId, NamedTextColor.GOLD)));
    } else {
      player.sendMessage(PREFIX.append(Component.text("Failed to join the game.", NamedTextColor.RED)));
    }

    return true;
  }

  private boolean handleLeaveCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(PREFIX.append(Component.text("Only players can leave games.", NamedTextColor.RED)));
      return true;
    }

    if (minigameManager.removePlayerFromGame(player)) {
      player.sendMessage(PREFIX.append(Component.text("You left the game.", NamedTextColor.GREEN)));
    } else {
      player.sendMessage(PREFIX.append(Component.text("You are not in a game.", NamedTextColor.RED)));
    }

    return true;
  }

  private boolean handleCreateCommand(CommandSender sender, String[] args) {
    if (!sender.hasPermission("iremiaminigame.command.create")) {
      sender.sendMessage(
          PREFIX.append(
              Component.text("You don't have permission to create minigames.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 3) {
      sender.sendMessage(
          PREFIX.append(Component.text("Usage: /minigame create <type> <id>", NamedTextColor.RED)));
      return true;
    }

    String gameType = args[1];
    String gameId = args[2];

    Minigame minigame = minigameManager.createMinigame(gameType, gameId);
    if (minigame != null) {
      sender.sendMessage(PREFIX.append(Component.text("Created game: ", NamedTextColor.GREEN))
          .append(Component.text(minigame.getDisplayName() + " (" + gameId + ")", NamedTextColor.GOLD)));
    } else {
      sender.sendMessage(PREFIX.append(Component.text("Failed to create game.", NamedTextColor.RED)));
    }

    return true;
  }

  private boolean handleListCommand(CommandSender sender, String[] args) {
    Map<String, Minigame> games = minigameManager.getActiveGames();

    if (games.isEmpty()) {
      sender.sendMessage(PREFIX.append(Component.text("There are no active games.", NamedTextColor.YELLOW)));
      return true;
    }

    sender.sendMessage(PREFIX.append(Component.text("Active Games:", NamedTextColor.YELLOW)));
    for (Map.Entry<String, Minigame> entry : games.entrySet()) {
      Minigame game = entry.getValue();
      sender.sendMessage(Component.text("- ", NamedTextColor.GRAY)
          .append(Component.text(entry.getKey(), NamedTextColor.GOLD))
          .append(Component.text(" (" + game.getDisplayName() + "): ", NamedTextColor.YELLOW))
          .append(Component.text(game.getPlayerCount() + " players, ", NamedTextColor.WHITE))
          .append(Component.text("Status: " + game.getState(), NamedTextColor.AQUA)));
    }

    return true;
  }

  private boolean handleStartCommand(CommandSender sender, String[] args) {
    if (!sender.hasPermission("iremiaminigame.command.start")) {
      sender.sendMessage(
          PREFIX.append(Component.text("You don't have permission to start minigames.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(PREFIX.append(Component.text("Usage: /minigame start <gameId>", NamedTextColor.RED)));
      return true;
    }

    String gameId = args[1];
    Minigame game = minigameManager.getActiveGames().get(gameId);

    if (game == null) {
      sender.sendMessage(PREFIX.append(Component.text("Game not found: " + gameId, NamedTextColor.RED)));
      return true;
    }

    game.startCountdown();
    sender.sendMessage(PREFIX.append(Component.text("Starting countdown for game: ", NamedTextColor.GREEN))
        .append(Component.text(gameId, NamedTextColor.GOLD)));

    return true;
  }

  private boolean handleEndCommand(CommandSender sender, String[] args) {
    if (!sender.hasPermission("iremiaminigame.command.end")) {
      sender.sendMessage(
          PREFIX.append(Component.text("You don't have permission to end minigames.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(PREFIX.append(Component.text("Usage: /minigame end <gameId>", NamedTextColor.RED)));
      return true;
    }

    String gameId = args[1];
    Minigame game = minigameManager.getActiveGames().get(gameId);

    if (game == null) {
      sender.sendMessage(PREFIX.append(Component.text("Game not found: " + gameId, NamedTextColor.RED)));
      return true;
    }

    game.end();
    sender.sendMessage(PREFIX.append(Component.text("Ended game: ", NamedTextColor.GREEN))
        .append(Component.text(gameId, NamedTextColor.GOLD)));

    return true;
  }

  private boolean handleSetMinCommand(CommandSender sender, String[] args) {
    if (!sender.hasPermission("iremiaminigame.command.setmin")) {
      sender.sendMessage(
          PREFIX.append(
              Component.text("You don't have permission to set minimum players.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 3) {
      sender.sendMessage(
          PREFIX.append(Component.text("Usage: /minigame setmin <gameId> <minPlayers>", NamedTextColor.RED)));
      return true;
    }

    String gameId = args[1];
    Minigame game = minigameManager.getActiveGames().get(gameId);

    if (game == null) {
      sender.sendMessage(PREFIX.append(Component.text("Game not found: " + gameId, NamedTextColor.RED)));
      return true;
    }

    try {
      int minPlayers = Integer.parseInt(args[2]);
      if (minPlayers < 1) {
        sender.sendMessage(
            PREFIX.append(Component.text("Minimum players must be at least 1.", NamedTextColor.RED)));
        return true;
      }

      game.setMinPlayers(minPlayers);
      sender.sendMessage(
          PREFIX.append(Component.text("Set minimum players to " + minPlayers + " for game: ",
              NamedTextColor.GREEN))
              .append(Component.text(gameId, NamedTextColor.GOLD)));
    } catch (NumberFormatException e) {
      sender.sendMessage(PREFIX.append(Component.text("Invalid number: " + args[2], NamedTextColor.RED)));
    }

    return true;
  }

  private boolean handleSetMaxCommand(CommandSender sender, String[] args) {
    if (!sender.hasPermission("iremiaminigame.command.setmax")) {
      sender.sendMessage(
          PREFIX.append(
              Component.text("You don't have permission to set maximum players.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 3) {
      sender.sendMessage(
          PREFIX.append(Component.text("Usage: /minigame setmax <gameId> <maxPlayers>", NamedTextColor.RED)));
      return true;
    }

    String gameId = args[1];
    Minigame game = minigameManager.getActiveGames().get(gameId);

    if (game == null) {
      sender.sendMessage(PREFIX.append(Component.text("Game not found: " + gameId, NamedTextColor.RED)));
      return true;
    }

    try {
      int maxPlayers = Integer.parseInt(args[2]);
      if (maxPlayers < 1) {
        sender.sendMessage(
            PREFIX.append(Component.text("Maximum players must be at least 1.", NamedTextColor.RED)));
        return true;
      }

      game.setMaxPlayers(maxPlayers);
      sender.sendMessage(
          PREFIX.append(Component.text("Set maximum players to " + maxPlayers + " for game: ",
              NamedTextColor.GREEN))
              .append(Component.text(gameId, NamedTextColor.GOLD)));
    } catch (NumberFormatException e) {
      sender.sendMessage(PREFIX.append(Component.text("Invalid number: " + args[2], NamedTextColor.RED)));
    }

    return true;
  }

  private boolean handleSetSpawnCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          PREFIX.append(Component.text("Only players can set spawn locations.", NamedTextColor.RED)));
      return true;
    }

    if (!sender.hasPermission("iremiaminigame.command.setspawn")) {
      sender.sendMessage(
          PREFIX.append(
              Component.text("You don't have permission to set spawn points.", NamedTextColor.RED)));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(PREFIX.append(Component.text("Usage: /minigame setspawn <gameId>", NamedTextColor.RED)));
      return true;
    }

    String gameId = args[1];
    Minigame game = minigameManager.getActiveGames().get(gameId);

    if (game == null) {
      sender.sendMessage(PREFIX.append(Component.text("Game not found: " + gameId, NamedTextColor.RED)));
      return true;
    }

    Location location = player.getLocation();
    game.setSpawnPoint(location);
    player.sendMessage(PREFIX.append(Component.text("Set spawn point for game: ", NamedTextColor.GREEN))
        .append(Component.text(gameId, NamedTextColor.GOLD)));

    return true;
  }

  private void showHelp(CommandSender sender) {
    sender.sendMessage(Component.text("=== Minigame Command Help ===", NamedTextColor.GOLD));
    sender.sendMessage(Component.text("/minigame join <gameId>", NamedTextColor.YELLOW)
        .append(Component.text(" - Join a minigame", NamedTextColor.WHITE)));
    sender.sendMessage(Component.text("/minigame leave", NamedTextColor.YELLOW)
        .append(Component.text(" - Leave your current minigame", NamedTextColor.WHITE)));
    sender.sendMessage(Component.text("/minigame list", NamedTextColor.YELLOW)
        .append(Component.text(" - List all active minigames", NamedTextColor.WHITE)));

    if (sender.hasPermission("iremiaminigame.command.create")) {
      sender.sendMessage(Component.text("/minigame create <type> <id>", NamedTextColor.YELLOW)
          .append(Component.text(" - Create a new minigame", NamedTextColor.WHITE)));
    }

    if (sender.hasPermission("iremiaminigame.command.start")) {
      sender.sendMessage(Component.text("/minigame start <gameId>", NamedTextColor.YELLOW)
          .append(Component.text(" - Start a minigame", NamedTextColor.WHITE)));
    }

    if (sender.hasPermission("iremiaminigame.command.end")) {
      sender.sendMessage(Component.text("/minigame end <gameId>", NamedTextColor.YELLOW)
          .append(Component.text(" - End a minigame", NamedTextColor.WHITE)));
    }

    if (sender.hasPermission("iremiaminigame.command.setmin")) {
      sender.sendMessage(Component.text("/minigame setmin <gameId> <minPlayers>", NamedTextColor.YELLOW)
          .append(Component.text(" - Set minimum players", NamedTextColor.WHITE)));
    }

    if (sender.hasPermission("iremiaminigame.command.setmax")) {
      sender.sendMessage(Component.text("/minigame setmax <gameId> <maxPlayers>", NamedTextColor.YELLOW)
          .append(Component.text(" - Set maximum players", NamedTextColor.WHITE)));
    }

    if (sender.hasPermission("iremiaminigame.command.setspawn")) {
      sender.sendMessage(Component.text("/minigame setspawn <gameId>", NamedTextColor.YELLOW)
          .append(Component.text(" - Set spawn point to current location", NamedTextColor.WHITE)));
    }
  }
}
