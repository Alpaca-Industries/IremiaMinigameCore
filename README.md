# IremiaMinigameCore

A powerful and flexible minigame framework for Minecraft servers running Bukkit/Spigot/Paper.

## 🎮 Overview

IremiaMinigameCore provides a comprehensive API and management system for creating custom minigames. It handles all the common functionality like player management, game states, commands, and lifecycle management, allowing developers to focus on creating unique game mechanics.

## ✨ Features

- **🏗️ Complete Framework** - Everything you need to build minigames
- **⚡ High Performance** - Optimized player caching and event handling
- **🔧 Easy API** - Simple and intuitive API for game creation
- **📋 Command System** - Full `/minigame` command suite included
- **🎯 Event System** - Comprehensive event handling and notifications
- **🔄 Lifecycle Management** - Automatic game state management
- **👥 Player Management** - Built-in player tracking and utilities
- **⚙️ Configurable** - Extensive configuration options

## 🚀 Quick Start

### For Server Administrators

1. Download the latest `IremiaMinigameCore.jar`
2. Place it in your `plugins/` folder
3. Restart your server
4. Configure the plugin in `plugins/IremiaMinigameCore/config.yml`

### For Developers

Add IremiaMinigameCore as a dependency to your plugin:

```xml
<!-- In your plugin.yml -->
depend: [IremiaMinigameCore]
```

```java
// In your plugin code
public class MyGamePlugin extends JavaPlugin {
    private IremiaMinigameAPI api;
    
    @Override
    public void onEnable() {
        // Get the API
        api = IremiaMinigameCorePlugin.getMinigameAPI();
        
        // Register your game type
        api.registerMinigameType(this, "mygame", new MyGameFactory(), "My Custom Game");
    }
}
```

## 📚 Commands

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

## 🔧 Configuration

```yaml
# config.yml
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

## 🛠️ API Documentation

### Core Classes

#### `IremiaMinigameAPI`
Main API interface for external plugins.

```java
// Register a new minigame type
boolean registerMinigameType(Plugin plugin, String typeId, MinigameFactory factory, String displayName);

// Create a minigame instance
Minigame createMinigame(String typeId, String instanceId);

// Player management
boolean addPlayerToMinigame(Player player, String minigameId);
boolean removePlayerFromMinigame(Player player);
Minigame getPlayerMinigame(Player player);
```

#### `MinigameFactory`
Interface for creating minigame instances.

```java
public class MyGameFactory implements MinigameFactory {
    @Override
    public Minigame createMinigame(String id, MinigameManager manager) {
        return new MyGame(id, "My Game", manager);
    }
}
```

#### `Minigame`
Base class for all minigames.

```java
public class MyGame extends Minigame {
    public MyGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
    }
    
    @Override
    protected void onStart() {
        // Game start logic
    }
    
    @Override
    protected void onEnd() {
        // Game end logic
    }
    
    @Override
    protected void onPlayerJoin(Player player) {
        // Player join logic
    }
    
    @Override
    protected void onPlayerLeave(Player player) {
        // Player leave logic
    }
}
```

### Game States

| State | Description |
|-------|-------------|
| `WAITING` | Waiting for players to join |
| `COUNTDOWN` | Countdown before game starts |
| `RUNNING` | Game is active |
| `ENDED` | Game has finished |

### Event System

```java
// Register global event listeners
api.registerGlobalMinigameListener(plugin, new MinigameEventListener() {
    @Override
    public void onMinigameStart(Minigame minigame) {
        // Handle game start
    }
    
    @Override
    public void onPlayerJoinMinigame(Player player, Minigame minigame) {
        // Handle player join
    }
});
```

## 🏗️ Creating Custom Minigames

### 1. Create Your Game Class

```java
public class KingOfTheHillGame extends Minigame {
    private Location hillCenter;
    private Player currentKing;
    
    public KingOfTheHillGame(String id, String displayName, MinigameManager manager) {
        super(id, displayName, manager);
        setMinPlayers(2);
        setMaxPlayers(10);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        broadcastMessage(Component.text("King of the Hill has started!"));
        // Start your game logic
    }
    
    @Override
    protected void onPlayerJoin(Player player) {
        super.onPlayerJoin(player);
        player.sendMessage("Welcome to King of the Hill!");
    }
}
```

### 2. Create a Factory

```java
public class KingOfTheHillFactory implements MinigameFactory {
    @Override
    public Minigame createMinigame(String id, MinigameManager manager) {
        return new KingOfTheHillGame(id, "King of the Hill", manager);
    }
}
```

### 3. Register Your Game

```java
public class MyMinigamePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        IremiaMinigameAPI api = IremiaMinigameCorePlugin.getMinigameAPI();
        api.registerMinigameType(this, "kingofthehill", new KingOfTheHillFactory(), "King of the Hill");
    }
}
```

## 🔗 Dependencies

- **Bukkit/Spigot/Paper** 1.21+
- **Java** 17+

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/alpacaindustries/iremiaminigamecore/issues)
- **Documentation**: [Wiki](https://github.com/alpacaindustries/iremiaminigamecore/wiki)
- **Discord**: [AlpacaIndustries Discord](https://discord.gg/alpacaindustries)

## 🏆 Examples

Check out these example implementations:
- **IremiaMinigameCorePlugin** - Collection of built-in minigames
- **Example plugins** in the `examples/` directory

---

Made with ❤️ by [AlpacaIndustries](https://alpacaindustries.org)
