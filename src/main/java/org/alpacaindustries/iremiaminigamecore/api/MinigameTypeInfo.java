package org.alpacaindustries.iremiaminigamecore.api;

import org.bukkit.plugin.Plugin;

/**
 * Information about a registered minigame type
 *
 * @since 1.0.0
 */
public class MinigameTypeInfo {

  private final String typeId;
  private final String displayName;
  private final Plugin plugin;
  private final String description;
  private final String version;

  public MinigameTypeInfo(String typeId, String displayName, Plugin plugin, String description) {
    this.typeId = typeId;
    this.displayName = displayName;
    this.plugin = plugin;
    this.description = description;
    this.version = plugin.getPluginMeta().getVersion();
  }

  /**
   * Get the unique type ID
   */
  public String getTypeId() {
    return typeId;
  }

  /**
   * Get the user-friendly display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Get the plugin that registered this type
   */
  public Plugin getPlugin() {
    return plugin;
  }

  /**
   * Get the description of this minigame type
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the version of the plugin that registered this type
   */
  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return String.format("%s (%s) by %s v%s", displayName, typeId, plugin.getName(), version);
  }
}
