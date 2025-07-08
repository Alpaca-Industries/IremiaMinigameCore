package org.alpacaindustries.iremiaminigamecore.system.config;

/**
 * Configuration class for survival-based minigames
 * Centralizes game settings and provides easy customization
 */
public class GameConfig {

  // Countdown settings
  private int countdownSeconds = 10;
  private int[] countdownAnnouncements = { 10, 5, 4, 3, 2, 1 };

  // Game duration settings
  private int maxGameDuration = 300; // 5 minutes default
  private int eventInterval = 10; // seconds between events

  // Player settings
  private int minPlayers = 2;
  private int maxPlayers = 16;
  private int yThreshold = 70; // elimination height
  private boolean allowJoinDuringGame = false;

  // Platform settings
  private int platformSize = 6;
  private int platformSpacing = 8;
  private int shrinkInterval = 15; // for shrinking platforms

  // Debug settings
  private boolean debugMode = false;

  // Getters and setters
  public int getCountdownSeconds() {
    return countdownSeconds;
  }

  public GameConfig setCountdownSeconds(int countdownSeconds) {
    this.countdownSeconds = countdownSeconds;
    return this;
  }

  public int[] getCountdownAnnouncements() {
    return countdownAnnouncements;
  }

  public GameConfig setCountdownAnnouncements(int[] countdownAnnouncements) {
    this.countdownAnnouncements = countdownAnnouncements;
    return this;
  }

  public int getMaxGameDuration() {
    return maxGameDuration;
  }

  public GameConfig setMaxGameDuration(int maxGameDuration) {
    this.maxGameDuration = maxGameDuration;
    return this;
  }

  public int getEventInterval() {
    return eventInterval;
  }

  public GameConfig setEventInterval(int eventInterval) {
    this.eventInterval = eventInterval;
    return this;
  }

  public int getMinPlayers() {
    return minPlayers;
  }

  public GameConfig setMinPlayers(int minPlayers) {
    this.minPlayers = minPlayers;
    return this;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public GameConfig setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
    return this;
  }

  public int getYThreshold() {
    return yThreshold;
  }

  public GameConfig setYThreshold(int yThreshold) {
    this.yThreshold = yThreshold;
    return this;
  }

  public boolean isAllowJoinDuringGame() {
    return allowJoinDuringGame;
  }

  public GameConfig setAllowJoinDuringGame(boolean allowJoinDuringGame) {
    this.allowJoinDuringGame = allowJoinDuringGame;
    return this;
  }

  public int getPlatformSize() {
    return platformSize;
  }

  public GameConfig setPlatformSize(int platformSize) {
    this.platformSize = platformSize;
    return this;
  }

  public int getPlatformSpacing() {
    return platformSpacing;
  }

  public GameConfig setPlatformSpacing(int platformSpacing) {
    this.platformSpacing = platformSpacing;
    return this;
  }

  public int getShrinkInterval() {
    return shrinkInterval;
  }

  public GameConfig setShrinkInterval(int shrinkInterval) {
    this.shrinkInterval = shrinkInterval;
    return this;
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public GameConfig setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
    return this;
  }

  /**
   * Create a debug configuration with relaxed settings
   */
  public static GameConfig debug() {
    return new GameConfig()
        .setDebugMode(true)
        .setMinPlayers(1)
        .setCountdownSeconds(3);
  }

  /**
   * Create a quick game configuration for faster testing
   */
  public static GameConfig quickGame() {
    return new GameConfig()
        .setCountdownSeconds(5)
        .setEventInterval(5)
        .setMaxGameDuration(120);
  }
}
