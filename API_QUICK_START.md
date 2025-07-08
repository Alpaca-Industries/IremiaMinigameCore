# API Quick Start Guide

Get up and running with IremiaMinigameCore in minutes!

## 🚀 Basic Setup

### 1. Add Dependency

In your `plugin.yml`:
```yaml
name: MyMinigamePlugin
depend: [IremiaMinigameCore]
```

### 2. Get the API

```java
public class MyPlugin extends JavaPlugin {
    private IremiaMinigameAPI api;
    
    @Override
    public void onEnable() {
        // Check if core is available
        if (!IremiaMinigameCorePlugin.isAPIAvailable()) {
            getLogger().severe("IremiaMinigameCore not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        api = IremiaMinigameCorePlugin.getMinigameAPI();
    }
}
```

## 🎮 Creating Your First Minigame

### Step 1: Create the Game Class

```java
public class MyFirstGame extends Minigame {
    
    public MyFirstGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        
        // Configure your game
        setMinPlayers(2);
        setMaxPlayers(8);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Game started - implement your logic here
        broadcastMessage(Component.text("Game has started!", NamedTextColor.GREEN));
        
        // Teleport players, give items, start timers, etc.
        for (Player player : getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            // Add your game items
        }
    }
    
    @Override
    protected void onEnd() {
        super.onEnd();
        
        // Game ended - cleanup logic here
        broadcastMessage(Component.text("Game has ended!", NamedTextColor.RED));
        
        // Reset players, announce winner, etc.
    }
    
    @Override
    protected void onPlayerJoin(Player player) {
        super.onPlayerJoin(player);
        
        // Player joined - setup for new player
        player.sendMessage(Component.text("Welcome to My First Game!", NamedTextColor.GOLD));
        
        // If waiting for players, show them status
        if (getState() == MinigameState.WAITING) {
            player.sendMessage(Component.text("Waiting for " + 
                (getMinPlayers() - getPlayerCount()) + " more players..."));
        }
    }
    
    @Override
    protected void onPlayerLeave(Player player) {
        super.onPlayerLeave(player);
        
        // Player left - handle cleanup
        broadcastMessage(Component.text(player.getName() + " left the game"));
        
        // Check if game should end due to not enough players
        if (getState() == MinigameState.RUNNING && getPlayerCount() < getMinPlayers()) {
            end();
        }
    }
}
```

### Step 2: Create a Factory

```java
public class MyFirstGameFactory implements MinigameFactory {
    @Override
    public Minigame createMinigame(String id, MinigameManager manager) {
        return new MyFirstGame(id, "My First Game", manager);
    }
}
```

### Step 3: Register Your Game

```java
@Override
public void onEnable() {
    // Get API
    api = IremiaMinigameCorePlugin.getMinigameAPI();
    
    // Register your game type
    boolean success = api.registerMinigameType(
        this,                           // Your plugin
        "myfirstgame",                 // Type ID (lowercase, no spaces)
        new MyFirstGameFactory(),      // Factory instance
        "My First Game"               // Display name
    );
    
    if (success) {
        getLogger().info("Successfully registered My First Game!");
    } else {
        getLogger().warning("Failed to register game type!");
    }
}
```

## 🎯 Testing Your Game

1. **Start your server** with both plugins installed
2. **Create a game**: `/minigame create myfirstgame test1`
3. **Join the game**: `/minigame join test1`
4. **Start the game**: `/minigame start test1`

## 🛠️ Common Patterns

### Adding Game Events

```java
@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    
    // Check if player is in this game
    if (!isPlayerValid(player)) return;
    
    // Your game logic here
    Location loc = player.getLocation();
    // Check boundaries, trigger events, etc.
}
```

### Using Timers

```java
private BukkitTask gameTimer;

@Override
protected void onStart() {
    super.onStart();
    
    // Start a 5-minute game timer
    gameTimer = Bukkit.getScheduler().runTaskLater(getManager().getPlugin(), () -> {
        broadcastMessage(Component.text("Time's up!"));
        end();
    }, 20L * 60 * 5); // 5 minutes in ticks
}

@Override
protected void onEnd() {
    super.onEnd();
    
    // Cancel timer if game ends early
    if (gameTimer != null) {
        gameTimer.cancel();
    }
}
```

### Player State Management

```java
@Override
protected void onPlayerJoin(Player player) {
    super.onPlayerJoin(player);
    
    // Use the built-in PlayerManager for consistent setup
    PlayerManager.preparePlayer(player, getSpawnPoint());
    
    // Or customize as needed
    player.setGameMode(GameMode.ADVENTURE);
    player.setHealth(20);
    player.setFoodLevel(20);
    player.getInventory().clear();
}
```

### Game Configuration

```java
public class ConfigurableGame extends Minigame {
    private final GameConfig config;
    
    public ConfigurableGame(String id, String displayName, MinigameManager manager, GameConfig config) {
        super(id, displayName, manager);
        this.config = config;
        
        // Apply config
        setMinPlayers(config.getMinPlayers());
        setMaxPlayers(config.getMaxPlayers());
    }
}
```

## 🎁 Next Steps

1. **Check out SurvivalMinigame** - Extends Minigame with elimination mechanics
2. **Use the Builder Pattern** - MinigameBuilder for complex configurations  
3. **Add Event Listeners** - Global listeners for cross-game features
4. **Explore Utilities** - MinigameUtils, MinigameMetrics for advanced features

## 💡 Tips

- Always call `super.onX()` in your override methods
- Use `isPlayerValid(player)` before processing player events
- The framework handles player tracking automatically
- Games auto-start when enough players join
- Use `broadcastMessage()` for game-wide announcements

Happy coding! 🎮
