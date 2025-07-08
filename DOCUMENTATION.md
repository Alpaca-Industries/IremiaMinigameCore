# IremiaMinigameAPI Documentation

A comprehensive guide to using the IremiaMinigame framework for creating custom minigames in Minecraft servers.

## Table of Contents

1. [Overview](#overview)
2. [Installation](#installation)
3. [Getting Started](#getting-started)
4. [Core Concepts](#core-concepts)
5. [API Reference](#api-reference)
6. [Creating Custom Minigames](#creating-custom-minigames)
7. [Event System](#event-system)
8. [Commands](#commands)
9. [Configuration](#configuration)
10. [Advanced Features](#advanced-features)
11. [Examples](#examples)
12. [Troubleshooting](#troubleshooting)

## Overview

IremiaMinigameCore is a powerful and flexible minigame framework for Minecraft servers running Bukkit/Spigot/Paper. It provides a complete infrastructure for creating, managing, and running custom minigames with features like:

- **Complete Framework** - Everything needed to build minigames
- **High Performance** - Optimized player caching and event handling
- **Easy API** - Simple and intuitive API for game creation
- **Command System** - Full `/minigame` command suite included
- **Event System** - Comprehensive event handling and notifications
- **Lifecycle Management** - Automatic game state management
- **Player Management** - Built-in player tracking and utilities
- **Configurable** - Extensive configuration options

## Installation

### For Server Administrators

1. **Download** the latest `IremiaMinigameCore.jar` from releases
2. **Place** it in your server's `plugins/` folder
3. **Restart** your server
4. **Configure** the plugin in `plugins/IremiaMinigameCore/config.yml`

### For Developers

Add IremiaMinigameCore as a dependency in your plugin:

#### plugin.yml
```yaml
name: MyMinigamePlugin
version: 1.0.0
main: com.example.MyMinigamePlugin
depend: [IremiaMinigameCore]
api-version: 1.21
```

#### Basic Plugin Setup
```java
public class MyMinigamePlugin extends JavaPlugin {
    private IremiaMinigameAPI api;
    
    @Override
    public void onEnable() {
        // Check if core is available
        if (!IremiaMinigameCorePlugin.isAPIAvailable()) {
            getLogger().severe("IremiaMinigameCore not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get the API
        api = IremiaMinigameCorePlugin.getMinigameAPI();
        
        // Register your minigame types
        registerMinigames();
    }
    
    private void registerMinigames() {
        // Register custom minigame types here
        api.registerMinigameType(this, "example", new ExampleGameFactory(), "Example Game");
    }
}
```

## Getting Started

### Quick Start Example

Here's a complete example of creating your first minigame:

#### 1. Create Your Game Class

```java
public class ExampleGame extends Minigame {
    
    public ExampleGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        
        // Configure game settings
        setMinPlayers(2);
        setMaxPlayers(8);
        setAllowJoinDuringGame(false);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        broadcastMessage(Component.text("Example Game has started!", NamedTextColor.GREEN));
        
        // Initialize game logic
        for (Player player : getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            // Add game items, teleport players, etc.
        }
        
        // Start game-specific timers or mechanics
    }
    
    @Override
    protected void onEnd() {
        super.onEnd();
        
        broadcastMessage(Component.text("Example Game has ended!", NamedTextColor.RED));
        
        // Cleanup logic, reset players, announce winner
        for (Player player : getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            // Reset player state
        }
    }
    
    @Override
    protected void onPlayerJoin(Player player) {
        super.onPlayerJoin(player);
        player.sendMessage(Component.text("Welcome to Example Game!", NamedTextColor.YELLOW));
        
        // Player-specific setup
        if (getState() == MinigameState.RUNNING) {
            // Handle mid-game join if allowed
        }
    }
    
    @Override
    protected void onPlayerLeave(Player player) {
        super.onPlayerLeave(player);
        broadcastMessage(Component.text(player.getName() + " left the game", NamedTextColor.GRAY));
        
        // Handle player leaving
    }
    
    @Override
    protected void onCountdownStart() {
        super.onCountdownStart();
        broadcastMessage(Component.text("Game starting in 10 seconds!", NamedTextColor.GOLD));
    }
}
```

#### 2. Create a Factory

```java
public class ExampleGameFactory implements MinigameFactory {
    @Override
    public Minigame createMinigame(String id, MinigameManager manager) {
        return new ExampleGame(id, "Example Game", manager);
    }
}
```

#### 3. Register Your Game

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        IremiaMinigameAPI api = IremiaMinigameCorePlugin.getMinigameAPI();
        api.registerMinigameType(this, "example", new ExampleGameFactory(), "Example Game");
    }
}
```

#### 4. Test Your Game

1. Start your server with both plugins installed
2. Create a game: `/minigame create example test1`
3. Join the game: `/minigame join test1`
4. Start the game: `/minigame start test1`

## Core Concepts

### Game States

Every minigame goes through these states:

| State | Description |
|-------|-------------|
| `WAITING` | Waiting for players to join |
| `COUNTDOWN` | Countdown before game starts (auto-triggered when min players reached) |
| `RUNNING` | Game is actively running |
| `ENDED` | Game has finished and is cleaning up |

### Player Management

The framework automatically handles:
- Player tracking and caching
- Join/leave validation
- State synchronization
- Memory cleanup

### Type Registration

Minigame types are registered with a unique ID format:
- `plugin:gametype` (recommended)
- `gametype` (auto-prefixed with plugin name)

## API Reference

### IremiaMinigameAPI Interface

The main API interface provides these key methods:

#### Registration Methods

```java
// Register a new minigame type
boolean registerMinigameType(Plugin plugin, String typeId, MinigameFactory factory, String displayName);

// Unregister a minigame type (cleanup on disable)
boolean unregisterMinigameType(Plugin plugin, String typeId);

// Check if a type is registered
boolean isMinigameTypeRegistered(String typeId);

// Get information about a registered type
MinigameTypeInfo getMinigameTypeInfo(String typeId);
```

#### Game Management Methods

```java
// Create a new minigame instance
Minigame createMinigame(String typeId, String instanceId);

// Get all active minigames
Map<String, Minigame> getActiveMinigames();

// Get all registered types
Set<String> getRegisteredTypes();
```

#### Player Management Methods

```java
// Get the minigame a player is currently in
Minigame getPlayerMinigame(Player player);

// Add a player to a specific minigame
boolean addPlayerToMinigame(Player player, String minigameId);

// Remove a player from their current minigame
boolean removePlayerFromMinigame(Player player);
```

#### Event System Methods

```java
// Register a global event listener
void registerGlobalMinigameListener(Plugin plugin, MinigameEventListener listener);

// Unregister a global event listener
void unregisterGlobalMinigameListener(Plugin plugin, MinigameEventListener listener);
```

### Minigame Base Class

The `Minigame` abstract class provides these key methods for your implementations:

#### Lifecycle Methods (Override These)

```java
protected void onStart()                    // Called when game starts
protected void onEnd()                      // Called when game ends
protected void onPlayerJoin(Player player)  // Called when player joins
protected void onPlayerLeave(Player player) // Called when player leaves
protected void onCountdownStart()           // Called when countdown begins
```

#### Utility Methods (Use These)

```java
// Player management
List<Player> getOnlinePlayers()
Player getPlayerById(UUID uuid)
boolean isPlayerValid(Player player)

// Messaging
void broadcastMessage(Component message)
void broadcastMessageBatch(Component message)

// Game control
void start()
void end()
boolean addPlayer(Player player)
void removePlayer(Player player)

// Configuration
void setMinPlayers(int min)
void setMaxPlayers(int max)
void setAllowJoinDuringGame(boolean allow)
void setSpawnPoint(Location location)

// State access
MinigameState getState()
String getId()
String getDisplayName()
Set<UUID> getPlayerUUIDs()
int getPlayerCount()
```

### MinigameFactory Interface

Simple factory interface for creating game instances:

```java
public interface MinigameFactory {
    Minigame createMinigame(String id, MinigameManager manager);
}
```

### MinigameEventListener Interface

Event listener with default implementations for all methods:

```java
public interface MinigameEventListener {
    default void onMinigameCreated(Minigame minigame) {}
    default void onMinigameStart(Minigame minigame) {}
    default void onMinigameEnd(Minigame minigame) {}
    default void onPlayerJoinMinigame(Player player, Minigame minigame) {}
    default void onPlayerLeaveMinigame(Player player, Minigame minigame) {}
    default void onPlayerEliminated(Player player, Minigame minigame, String reason) {}
    default void onCountdownStart(Minigame minigame) {}
}
```

## Creating Custom Minigames

### Basic Minigame Template

```java
public class MyCustomGame extends Minigame {
    
    // Game-specific fields
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final List<Location> spawnPoints = new ArrayList<>();
    private CountdownTimer gameTimer;
    
    public MyCustomGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        
        // Configure basic settings
        setMinPlayers(2);
        setMaxPlayers(10);
        setAllowJoinDuringGame(false);
        
        // Initialize game-specific data
        setupSpawnPoints();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Initialize player scores
        for (UUID playerId : getPlayerUUIDs()) {
            playerScores.put(playerId, 0);
        }
        
        // Setup players
        setupPlayers();
        
        // Start game timer (5 minutes)
        startGameTimer(300);
        
        broadcastMessage(Component.text("Game started! Survive for 5 minutes!", NamedTextColor.GREEN));
    }
    
    @Override
    protected void onEnd() {
        super.onEnd();
        
        // Stop timers
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        // Announce winner
        announceWinner();
        
        // Clean up
        playerScores.clear();
    }
    
    @Override
    protected void onPlayerJoin(Player player) {
        super.onPlayerJoin(player);
        
        // Assign spawn point
        if (!spawnPoints.isEmpty()) {
            Location spawn = spawnPoints.get(getPlayerCount() % spawnPoints.size());
            player.teleport(spawn);
        }
        
        // Initialize player
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        giveStartingItems(player);
        
        player.sendMessage(Component.text("Welcome to " + getDisplayName() + "!", NamedTextColor.YELLOW));
    }
    
    @Override
    protected void onPlayerLeave(Player player) {
        super.onPlayerLeave(player);
        
        // Remove from scores
        playerScores.remove(player.getUniqueId());
        
        // Check win condition
        if (getPlayerCount() == 1 && getState() == MinigameState.RUNNING) {
            // Last player wins
            end();
        }
    }
    
    // Game-specific methods
    private void setupSpawnPoints() {
        // Add your spawn points
    }
    
    private void setupPlayers() {
        for (Player player : getOnlinePlayers()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setGameMode(GameMode.ADVENTURE);
        }
    }
    
    private void giveStartingItems(Player player) {
        // Give starting equipment
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BREAD, 5));
    }
    
    private void startGameTimer(int seconds) {
        gameTimer = new CountdownTimer(getManager().getPlugin(), seconds, 
            (remaining) -> {
                if (remaining % 60 == 0 && remaining > 0) {
                    broadcastMessage(Component.text(remaining/60 + " minutes remaining!", NamedTextColor.GOLD));
                }
            },
            () -> {
                broadcastMessage(Component.text("Time's up!", NamedTextColor.RED));
                end();
            }
        );
        gameTimer.start();
    }
    
    private void announceWinner() {
        if (playerScores.isEmpty()) return;
        
        UUID winnerId = playerScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
            
        if (winnerId != null) {
            Player winner = getPlayerById(winnerId);
            if (winner != null) {
                broadcastMessage(Component.text(winner.getName() + " wins with " + 
                    playerScores.get(winnerId) + " points!", NamedTextColor.GOLD));
            }
        }
    }
    
    // Event handlers for game mechanics
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        
        if (!isPlayerInGame(victim) || !isPlayerInGame(attacker)) return;
        
        // Custom damage handling
        if (getState() != MinigameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        
        // Award points for hits
        playerScores.merge(attacker.getUniqueId(), 1, Integer::sum);
    }
    
    private boolean isPlayerInGame(Player player) {
        return getPlayerUUIDs().contains(player.getUniqueId());
    }
}
```

### Advanced Features

#### Using SurvivalMinigame

For elimination-based games, extend `SurvivalMinigame` instead:

```java
public class LastManStandingGame extends SurvivalMinigame {
    
    public LastManStandingGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        setMinPlayers(3);
        setMaxPlayers(16);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Your start logic
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isPlayerInGame(player)) return;
        
        // Eliminate the player
        eliminatePlayer(player, "Died in combat");
        
        // Check win condition is handled automatically by SurvivalMinigame
    }
}
```

#### Using MinigameBuilder

For complex configurations, use the builder pattern:

```java
public class ComplexGameFactory implements MinigameFactory {
    @Override
    public Minigame createMinigame(String id, MinigameManager manager) {
        return MinigameBuilder.create(id, "Complex Game", manager)
            .minPlayers(4)
            .maxPlayers(12)
            .allowJoinDuringGame(false)
            .spawnPoint(new Location(world, 0, 100, 0))
            .countdownSeconds(15)
            .gameDurationSeconds(600)
            .build(ComplexGame::new);
    }
}
```

## Event System

### Global Event Listeners

Register listeners to handle events across all minigames:

```java
public class MinigameStatsPlugin extends JavaPlugin implements MinigameEventListener {
    
    @Override
    public void onEnable() {
        IremiaMinigameAPI api = IremiaMinigameCorePlugin.getMinigameAPI();
        api.registerGlobalMinigameListener(this, this);
    }
    
    @Override
    public void onMinigameStart(Minigame minigame) {
        // Track game start in database
        getLogger().info("Minigame started: " + minigame.getDisplayName());
    }
    
    @Override
    public void onPlayerJoinMinigame(Player player, Minigame minigame) {
        // Track player participation
        updatePlayerStats(player, "games_joined");
    }
    
    @Override
    public void onMinigameEnd(Minigame minigame) {
        // Track game completion
        saveGameResults(minigame);
    }
    
    private void updatePlayerStats(Player player, String stat) {
        // Your stats tracking logic
    }
    
    private void saveGameResults(Minigame minigame) {
        // Your results saving logic
    }
}
```

### Custom Event Handling

Within your minigame, use standard Bukkit event handling:

```java
public class MyGame extends Minigame {
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isPlayerInGame(event.getPlayer())) return;
        
        if (getState() != MinigameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        
        // Game-specific block break logic
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isPlayerInGame(event.getPlayer())) return;
        
        // Handle player interactions
    }
}
```

## Commands

The framework provides a comprehensive command system:

| Command | Permission | Description |
|---------|------------|-------------|
| `/minigame join <gameId>` | `iremiaminigame.command.join` | Join a minigame |
| `/minigame leave` | `iremiaminigame.command.leave` | Leave current minigame |
| `/minigame list` | `iremiaminigame.command.list` | List active minigames |
| `/minigame create <type> <id>` | `iremiaminigame.command.create` | Create a new minigame |
| `/minigame start <gameId>` | `iremiaminigame.command.start` | Start a minigame |
| `/minigame end <gameId>` | `iremiaminigame.command.end` | End a minigame |
| `/minigame setmin <gameId> <count>` | `iremiaminigame.command.setmin` | Set minimum players |
| `/minigame setmax <gameId> <count>` | `iremiaminigame.command.setmax` | Set maximum players |
| `/minigame setspawn <gameId>` | `iremiaminigame.command.setspawn` | Set spawn point |

### Permission Nodes

Default permissions:
- `iremiaminigame.command.*` - All command access
- `iremiaminigame.command.join` - Join games
- `iremiaminigame.command.leave` - Leave games
- `iremiaminigame.command.list` - List games
- Admin commands require higher permissions

## Configuration

### config.yml

```yaml
minigames:
  # Default player limits
  default-min-players: 2
  default-max-players: 16
  
  # Timing settings
  default-countdown-seconds: 10
  default-game-duration-seconds: 300
  
  # Performance settings
  player-cache-cleanup-interval: 30
  max-cached-players: 100
  
  # Messages
  messages:
    game-full: "This game is full!"
    game-in-progress: "This game is already in progress!"
    not-enough-players: "Not enough players to start!"
    players-remaining: "Not enough players remaining! Ending game."
    countdown-cancelled: "Countdown cancelled - not enough players!"
```

### Accessing Configuration

```java
// In your minigame class
int defaultMin = MinigameConfig.getDefaultMinPlayers();
int defaultMax = MinigameConfig.getDefaultMaxPlayers();
String fullMessage = MinigameConfig.getMsgGameFull();
```

## Advanced Features

### Utility Classes

#### CountdownTimer

```java
CountdownTimer timer = new CountdownTimer(plugin, 30,
    (remaining) -> broadcastMessage(Component.text("Starting in " + remaining + " seconds")),
    () -> start()
);
timer.start();
```

#### MinigameUtils

```java
// Teleport all players to spawn
MinigameUtils.teleportPlayersToSpawn(getOnlinePlayers(), spawnLocation);

// Clear all player inventories
MinigameUtils.clearPlayerInventories(getOnlinePlayers());

// Reset player states
MinigameUtils.resetPlayerStates(getOnlinePlayers());
```

#### AsyncUtils

```java
// Run async task
AsyncUtils.runAsync(plugin, () -> {
    // Heavy computation
    return result;
}).thenAccept(result -> {
    // Handle result on main thread
});
```

### Performance Optimization

#### Player Caching

The framework automatically caches online players for performance:

```java
// Efficient - uses cache
List<Player> players = getOnlinePlayers();

// Less efficient - avoid if possible
for (UUID uuid : getPlayerUUIDs()) {
    Player player = Bukkit.getPlayer(uuid);
    // ...
}
```

#### Batch Operations

```java
// Use batch messaging for better performance
broadcastMessageBatch(message);

// Use batch player operations
List<Player> players = getOnlinePlayers();
players.forEach(player -> {
    // Batch operations
});
```

### Memory Management

The framework handles cleanup automatically, but for custom data:

```java
public class MyGame extends Minigame {
    private final Map<UUID, CustomData> customData = new HashMap<>();
    
    @Override
    protected void onEnd() {
        super.onEnd();
        
        // Clean up custom data
        customData.clear();
    }
    
    @Override
    protected void onPlayerLeave(Player player) {
        super.onPlayerLeave(player);
        
        // Clean up player-specific data
        customData.remove(player.getUniqueId());
    }
}
```

## Examples

### Example 1: Simple Deathmatch

```java
public class DeathmatchGame extends SurvivalMinigame {
    
    public DeathmatchGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        setMinPlayers(2);
        setMaxPlayers(8);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Give weapons to all players
        for (Player player : getOnlinePlayers()) {
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            player.getInventory().addItem(new ItemStack(Material.BOW));
            player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
            player.setGameMode(GameMode.ADVENTURE);
        }
        
        broadcastMessage(Component.text("Last player standing wins!", NamedTextColor.RED));
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isPlayerInGame(player)) return;
        
        eliminatePlayer(player, "Eliminated");
        event.setCancelled(true); // Prevent normal death
        
        // Respawn as spectator
        player.spigot().respawn();
        player.setGameMode(GameMode.SPECTATOR);
    }
}
```

### Example 2: Timed Collection Game

```java
public class CollectionGame extends Minigame {
    private final Map<UUID, Integer> itemsCollected = new HashMap<>();
    private CountdownTimer gameTimer;
    
    public CollectionGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        setMinPlayers(2);
        setMaxPlayers(16);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Initialize scores
        for (UUID playerId : getPlayerUUIDs()) {
            itemsCollected.put(playerId, 0);
        }
        
        // Start 3-minute timer
        gameTimer = new CountdownTimer(getManager().getPlugin(), 180,
            this::onTimerTick,
            this::onTimeUp
        );
        gameTimer.start();
        
        // Spawn collection items
        spawnCollectionItems();
        
        broadcastMessage(Component.text("Collect as many diamonds as possible in 3 minutes!", NamedTextColor.BLUE));
    }
    
    @Override
    protected void onEnd() {
        super.onEnd();
        
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        announceWinner();
        itemsCollected.clear();
    }
    
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlayerInGame(player)) return;
        
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.DIAMOND) {
            itemsCollected.merge(player.getUniqueId(), item.getAmount(), Integer::sum);
            player.sendMessage(Component.text("Diamonds collected: " + itemsCollected.get(player.getUniqueId()), 
                NamedTextColor.GREEN));
        }
    }
    
    private void onTimerTick(int remaining) {
        if (remaining == 60 || remaining == 30 || remaining <= 10) {
            broadcastMessage(Component.text(remaining + " seconds remaining!", NamedTextColor.YELLOW));
        }
    }
    
    private void onTimeUp() {
        broadcastMessage(Component.text("Time's up!", NamedTextColor.RED));
        end();
    }
    
    private void spawnCollectionItems() {
        // Spawn diamond items around the arena
        World world = getSpawnPoint().getWorld();
        for (int i = 0; i < 50; i++) {
            Location loc = getRandomLocation();
            world.dropItem(loc, new ItemStack(Material.DIAMOND));
        }
    }
    
    private void announceWinner() {
        if (itemsCollected.isEmpty()) return;
        
        UUID winnerId = itemsCollected.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
            
        if (winnerId != null) {
            Player winner = getPlayerById(winnerId);
            if (winner != null) {
                broadcastMessage(Component.text(winner.getName() + " wins with " + 
                    itemsCollected.get(winnerId) + " diamonds!", NamedTextColor.GOLD));
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

#### "API not available" Error
```
[ERROR] IremiaMinigameCore not found!
```
**Solution:** Ensure IremiaMinigameCore is installed and loaded before your plugin.

#### Players Can't Join Games
**Possible causes:**
- Game is full (`maxPlayers` reached)
- Game already started (and `allowJoinDuringGame` is false)
- Player is already in another game

#### Games Don't Auto-Start
**Check:**
- Minimum players is set correctly
- Players actually joined successfully
- No errors in console

#### Memory Leaks
**Prevention:**
- Always call `super.onEnd()` in your `onEnd()` method
- Clear custom collections in `onEnd()` and `onPlayerLeave()`
- Unregister custom listeners properly

### Debugging Tips

#### Enable Debug Logging
```java
// Add to your minigame constructor
getManager().getPlugin().getLogger().setLevel(Level.FINE);
```

#### Check Game State
```java
getLogger().info("Game " + getId() + " state: " + getState());
getLogger().info("Players: " + getPlayerCount() + "/" + getMaxPlayers());
```

#### Validate Player State
```java
@Override
protected void onPlayerJoin(Player player) {
    super.onPlayerJoin(player);
    getLogger().info("Player " + player.getName() + " joined. Total: " + getPlayerCount());
}
```

### Performance Tips

1. **Use batch operations** when possible
2. **Cache frequently accessed data** in your minigame class
3. **Limit event handler scope** with proper validation
4. **Clean up resources** in `onEnd()` and `onPlayerLeave()`
5. **Use async operations** for heavy computations

### Best Practices

1. **Always call super methods** in your overrides
2. **Validate player state** before processing events
3. **Handle edge cases** (empty games, disconnections)
4. **Provide clear feedback** to players
5. **Test with multiple players** and edge cases
6. **Document your minigame's** unique mechanics

## Support

- **Issues**: [GitHub Issues](https://github.com/alpaca-industries/iremiaminigamecore/issues)
- **Documentation**: [Wiki](https://github.com/alpaca-industries/iremiaminigamecore/wiki)
- **Discord**: [alpaca-industries Discord](https://discord.gg/alpaca-industries)

---

*Made with ❤️ by [alpaca-industries](https://greysilly7.xyz)*
