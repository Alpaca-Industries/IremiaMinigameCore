package org.alpacaindustries.iremiaminigame.util.schem;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.Chunk;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.alpacaindustries.iremiaminigame.util.schem.nbt.NBTParser;
import org.alpacaindustries.iremiaminigame.util.AsyncUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SchematicLoader {
  private static final Logger logger = Logger.getLogger(SchematicLoader.class.getName());
  private final World world;
  private final Plugin plugin;
  private final NBTParser nbtParser = new NBTParser();
  private final VarIntDecoder varIntDecoder = new VarIntDecoder();
  private final Set<String> loggedBlockErrors = ConcurrentHashMap.newKeySet();

  public record PlacementMetrics(
      int blocksPlaced,
      long loadTime,
      long parseTime,
      long decodingTime,
      long preparationTime,
      long batchTime,
      long totalTime,
      int batchCount,
      long memoryUsedMB,
      double blocksPerSecond,
      String performanceGrade) {
  }

  public sealed interface SchematicResult permits SchematicResult.Success, SchematicResult.Failure {
    record Success(PlacementMetrics metrics) implements SchematicResult {
    }

    record Failure(String reason, Exception cause) implements SchematicResult {
    }
  }

  public SchematicLoader(World world, Plugin plugin) {
    this.world = world;
    this.plugin = plugin;
  }

  /**
   * Loads and places a schematic at the given position
   */
  public boolean pasteSchematic(String filePath, Location position) {
    File file = new File(filePath);
    if (!file.exists()) {
      logger.info("Schematic file not found: " + filePath);
      return false;
    }

    long startTime = System.currentTimeMillis();
    try {
      // Load and parse schematic file
      LoadResult nbtResult = loadSchematicFile(file);
      if (nbtResult == null)
        return false;

      ParseResult parseResult = parseSchematicData(nbtResult.nbtData);
      if (parseResult == null)
        return false;

      // Extract dimensions and position
      DimensionResult dimensionResult = extractDimensions(parseResult.schematic, position);
      if (dimensionResult == null)
        return false;

      // Place the blocks
      PlacementMetrics metrics = placeBlocks(parseResult.schematic, dimensionResult.dimensions,
          dimensionResult.finalPosition, startTime,
          nbtResult.loadTime, parseResult.parseTime);

      // Log success and performance metrics
      logPerformanceMetrics(metrics);
      return true;
    } catch (Exception e) {
      logger.severe("Error loading schematic from " + filePath + ": " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private record LoadResult(Map<String, Object> nbtData, long loadTime) {
  }

  /**
   * Loads schematic file and parses NBT data
   */
  private LoadResult loadSchematicFile(File file) {
    var startTime = System.currentTimeMillis();
    try (var fis = new FileInputStream(file);
        var gis = new GZIPInputStream(fis);
        var input = new DataInputStream(gis)) {

      var nbtData = nbtParser.readNBT(input);
      logger.info("Reading schematic from " + file.getPath());
      return new LoadResult(nbtData, System.currentTimeMillis() - startTime);
    } catch (Exception e) {
      logger.severe("Failed to load schematic file: " + file.getPath() + " - " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  private record ParseResult(Map<String, Object> schematic, long parseTime) {
  }

  /**
   * Parses schematic data and validates format
   */
  private ParseResult parseSchematicData(Map<String, Object> nbtData) {
    long startTime = System.currentTimeMillis();
    Map<String, Object> schematic = (Map<String, Object>) nbtData.get("Schematic");
    if (schematic == null) {
      logger.severe("Missing Schematic compound in NBT data");
      return null;
    }

    Integer version = nbtParser.getIntValue(schematic, "Version");
    if (version == null || version != 2) {
      logger.severe("Unsupported schematic version: " + version + " (expected 2)");
      return null;
    }

    Integer dataVersion = nbtParser.getIntValue(schematic, "DataVersion");
    logger.info("Schematic data version: " + dataVersion);

    return new ParseResult(schematic, System.currentTimeMillis() - startTime);
  }

  // Convert to records for better Java 21 support
  private record Dimensions(int width, int height, int length) {
  }

  private record DimensionResult(Dimensions dimensions, Location finalPosition) {
  }

  private record ChunkPos(int x, int z) {
    @Override
    public int hashCode() {
      return x * 31 + z;
    }
  }

  private record BlockChange(int x, int y, int z, BlockData blockData) {
  }

  private DimensionResult extractDimensions(Map<String, Object> schematic, Location position) {
    Integer width = nbtParser.getIntValue(schematic, "Width");
    if (width == null) {
      logger.severe("Missing or invalid Width in schematic");
      return null;
    }

    Integer height = nbtParser.getIntValue(schematic, "Height");
    if (height == null) {
      logger.severe("Missing or invalid Height in schematic");
      return null;
    }

    Integer length = nbtParser.getIntValue(schematic, "Length");
    if (length == null) {
      logger.severe("Missing or invalid Length in schematic");
      return null;
    }

    Location finalPosition = position.clone();
    int[] offsetArray = (int[]) schematic.get("Offset");
    if (offsetArray != null && offsetArray.length >= 3) {
      finalPosition.add(offsetArray[0], offsetArray[1], offsetArray[2]);
    }

    return new DimensionResult(new Dimensions(width, height, length), finalPosition);
  }

  /**
   * Processes palette and maps block IDs to BlockData with preserved states
   */
  private Map<Integer, BlockData> processPaletteWithStates(Map<String, Object> schematic) {
    Map<String, Object> palette = (Map<String, Object>) schematic.get("Palette");
    if (palette == null) {
      logger.severe("Missing Palette in schematic");
      return null;
    }

    Map<Integer, BlockData> result = new ConcurrentHashMap<>();
    for (Map.Entry<String, Object> entry : palette.entrySet()) {
      Integer paletteValue = (Integer) entry.getValue();
      if (paletteValue == null)
        continue;

      String blockName = entry.getKey();
      BlockData blockData = createBlockDataFromString(blockName);
      result.put(paletteValue, blockData);
    }

    return result;
  }

  /**
   * Creates BlockData from block string using Bukkit's native parsing with fallbacks
   */
  private BlockData createBlockDataFromString(String blockId) {
    return createBlockDataWithFallback(blockId);
  }

  /**
   * Places blocks from the schematic into the world
   */
  private PlacementMetrics placeBlocks(Map<String, Object> schematic, Dimensions dimensions,
      Location finalPosition, long startTime, long loadTime, long parseTime) {
    Map<Integer, BlockData> paletteMap = processPaletteWithStates(schematic);
    if (paletteMap == null) {
      return new PlacementMetrics(0, loadTime, parseTime, 0, 0, 0, 0, 0, 0, 0, "Unknown");
    }

    byte[] blockData = (byte[]) schematic.get("BlockData");
    if (blockData == null) {
      logger.severe("Missing BlockData in schematic");
      return new PlacementMetrics(0, loadTime, parseTime, 0, 0, 0, 0, 0, 0, 0, "Unknown");
    }

    int totalBlocks = dimensions.width * dimensions.height * dimensions.length;

    long decodeStartTime = System.currentTimeMillis();
    int[] blockIds = varIntDecoder.decodeVarints(blockData, totalBlocks);
    long decodingTime = System.currentTimeMillis() - decodeStartTime;

    long preparationStartTime = System.currentTimeMillis();
    Map<ChunkPos, List<BlockChange>> batchesByChunk = prepareBatchesParallel(dimensions, finalPosition, blockIds,
        paletteMap);
    long preparationTime = System.currentTimeMillis() - preparationStartTime;

    long batchApplyStart = System.currentTimeMillis();
    applyBatches(batchesByChunk);
    long batchTime = System.currentTimeMillis() - batchApplyStart;

    return new PlacementMetrics(
        totalBlocks,
        loadTime,
        parseTime,
        decodingTime,
        preparationTime,
        batchTime,
        System.currentTimeMillis() - startTime,
        batchesByChunk.size(),
        getMemoryUsedMB(),
        calculateBlocksPerSecond(totalBlocks, preparationTime + batchTime),
        calculatePerformanceGrade(totalBlocks, preparationTime + batchTime));
  }

  // Removed duplicate classes - using records defined above

  /**
   * Prepares block batches using AsyncUtils for proper Bukkit thread management
   */
  private Map<ChunkPos, List<BlockChange>> prepareBatchesParallel(Dimensions dimensions, Location finalPosition,
      int[] blockIds, Map<Integer, BlockData> paletteMap) {
    Map<ChunkPos, List<BlockChange>> batchesByChunk = new ConcurrentHashMap<>();
    int width = dimensions.width;
    int height = dimensions.height;
    int length = dimensions.length;

    // Use Virtual Threads instead of ForkJoinPool
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      for (int y = 0; y < height; y++) {
        for (int z = 0; z < length; z++) {
          final int finalY = y;
          final int finalZ = z;

          CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            for (int x = 0; x < width; x++) {
              int index = x + (finalZ * width) + (finalY * width * length);
              if (index < blockIds.length) {
                int blockId = blockIds[index];
                BlockData blockData = paletteMap.getOrDefault(blockId, Material.AIR.createBlockData());

                int worldX = finalPosition.getBlockX() + x;
                int worldY = finalPosition.getBlockY() + finalY;
                int worldZ = finalPosition.getBlockZ() + finalZ;

                int chunkX = worldX >> 4;
                int chunkZ = worldZ >> 4;

                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                batchesByChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>())
                    .add(new BlockChange(worldX, worldY, worldZ, blockData));
              }
            }
          }, executor);

          futures.add(future);
        }
      }

      // Wait for all futures to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    return batchesByChunk;
  }

  /**
   * Applies batches to the world using bulk block operations
   */
  private void applyBatches(Map<ChunkPos, List<BlockChange>> batchesByChunk) {
    logger.info("Preloading " + batchesByChunk.size() + " chunks...");
    Set<ChunkPos> loadedChunks = preloadChunks(new ArrayList<>(batchesByChunk.keySet()));

    logger.info("Applying " + batchesByChunk.size() + " batches using bulk operations...");
    int totalBatches = batchesByChunk.size();
    int completedBatches = 0;

    for (Map.Entry<ChunkPos, List<BlockChange>> entry : batchesByChunk.entrySet()) {
      ChunkPos chunkPos = entry.getKey();
      List<BlockChange> changes = entry.getValue();

      if (loadedChunks.contains(chunkPos)) {
        // Use bulk block change for better performance
        applyBulkBlockChanges(changes);

        synchronized (this) {
          completedBatches++;
          logBatchProgress(completedBatches, totalBatches);
        }
      } else {
        logger.warning("Skipping batch for unloaded chunk " + chunkPos.x + ", " + chunkPos.z);
      }
    }
  }

  /**
   * Applies block changes using bulk operations with cache optimization
   */
  private void applyBulkBlockChanges(List<BlockChange> changes) {
    applyAdvancedBulkChanges(changes);
  }

  /**
   * Preloads chunks for batch operations
   */
  private Set<ChunkPos> preloadChunks(List<ChunkPos> chunksToLoad) {
    Set<ChunkPos> loadedChunks = new HashSet<>();

    for (ChunkPos chunkPos : chunksToLoad) {
      if (world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
        loadedChunks.add(chunkPos);
        continue;
      }

      try {
        Chunk chunk = world.getChunkAt(chunkPos.x, chunkPos.z);
        if (!chunk.isLoaded()) {
          chunk.load();
        }
        loadedChunks.add(chunkPos);

        if (loadedChunks.size() % 10 == 0 || loadedChunks.size() == chunksToLoad.size()) {
          logger.info("Loaded " + loadedChunks.size() + "/" + chunksToLoad.size() + " chunks");
        }
      } catch (Exception e) {
        logger.severe("Error loading chunk at (" + chunkPos.x + ", " + chunkPos.z + "): " + e.getMessage());
        e.printStackTrace();
      }
    }

    logger.info("Successfully preloaded " + loadedChunks.size() + "/" + chunksToLoad.size() + " chunks");
    return loadedChunks;
  }

  /**
   * Logs batch application progress
   */
  private void logBatchProgress(int completedBatches, int totalBatches) {
    int percent = (int) (completedBatches * 100.0 / totalBatches);
    if (completedBatches % 5 == 0 || completedBatches == totalBatches) {
      logger.info("Applied " + completedBatches + "/" + totalBatches + " batches (" + percent + "%)");
    }
  }

  /**
   * Logs performance metrics after schematic placement
   */
  private void logPerformanceMetrics(PlacementMetrics metrics) {
    long placementTime = metrics.preparationTime + metrics.batchTime;
    int blocksPerSecond = (int) (metrics.blocksPlaced * 1000.0 / Math.max(placementTime, 1));
    int blocksPerSecondTotal = (int) (metrics.blocksPlaced * 1000.0 / Math.max(metrics.totalTime, 1));
    int averageBatchSize = metrics.blocksPlaced / Math.max(metrics.batchCount, 1);

    StringBuilder sb = new StringBuilder();
    sb.append("Schematic placed successfully! Performance metrics:");
    sb.append("\n - Total blocks placed: ").append(metrics.blocksPlaced);
    sb.append("\n - NBT load time: ").append(metrics.loadTime).append("ms");
    sb.append("\n - NBT parse time: ").append(metrics.parseTime).append("ms");
    sb.append("\n - VarInt decoding time: ").append(metrics.decodingTime).append("ms");
    sb.append("\n - Block preparation time: ").append(metrics.preparationTime).append("ms");
    sb.append("\n - Batch application time: ").append(metrics.batchTime).append("ms");
    sb.append("\n - Total block placement time: ").append(placementTime).append("ms");
    sb.append("\n - Total execution time: ").append(metrics.totalTime).append("ms");
    sb.append("\n - Block placement speed: ").append(blocksPerSecond).append(" blocks/sec");
    sb.append("\n - End-to-end speed: ").append(blocksPerSecondTotal).append(" blocks/sec");
    sb.append("\n - Batches: ").append(metrics.batchCount).append(" (avg: ").append(averageBatchSize)
        .append(" blocks/batch)");

    Runtime runtime = Runtime.getRuntime();
    long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    long totalMemory = runtime.totalMemory() / (1024 * 1024);
    sb.append("\n - Memory usage: ").append(usedMemory).append(" MB / ").append(totalMemory).append(" MB");

    logger.info(sb.toString());
  }

  /**
   * Async schematic pasting using AsyncUtils utility
   */
  public CompletableFuture<SchematicResult> pasteSchematicAsync(String filePath, Location position) {
    return AsyncUtils.runAsync(plugin, () -> {
      boolean success = pasteSchematic(filePath, position);
      if (success) {
        // TODO: Capture actual metrics from the operation
        var metrics = new PlacementMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "Unknown");
        return new SchematicResult.Success(metrics);
      } else {
        return new SchematicResult.Failure("Schematic pasting failed", null);
      }
    });
  }

  /**
   * Gets current memory usage in MB
   */
  private long getMemoryUsedMB() {
    Runtime runtime = Runtime.getRuntime();
    return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
  }

  /**
   * Calculates blocks per second performance metric
   */
  private double calculateBlocksPerSecond(int totalBlocks, long timeMs) {
    if (timeMs <= 0)
      return 0.0;
    return (totalBlocks * 1000.0) / timeMs;
  }

  /**
   * Calculates performance grade based on blocks per second
   */
  private String calculatePerformanceGrade(int totalBlocks, long timeMs) {
    double blocksPerSecond = calculateBlocksPerSecond(totalBlocks, timeMs);

    if (blocksPerSecond >= 10000)
      return "S+ (Excellent)";
    if (blocksPerSecond >= 5000)
      return "S (Very Good)";
    if (blocksPerSecond >= 2000)
      return "A (Good)";
    if (blocksPerSecond >= 1000)
      return "B (Average)";
    if (blocksPerSecond >= 500)
      return "C (Below Average)";
    return "D (Poor)";
  }

  /**
   * Enhanced bulk block placement with cache locality optimization
   */
  private void applyAdvancedBulkChanges(List<BlockChange> changes) {
    // Group by Y level AND chunk for optimal cache locality
    var changesByYAndChunk = changes.stream()
        .filter(change -> change != null)
        .collect(Collectors.groupingBy(
            change -> change.y,
            Collectors.groupingBy(change -> new ChunkPos(change.x >> 4, change.z >> 4))));

    // Process in optimal order: Y-level ascending, then by chunk
    changesByYAndChunk.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(yEntry -> {
          yEntry.getValue().entrySet().parallelStream()
              .forEach(chunkEntry -> {
                List<BlockChange> chunkChanges = chunkEntry.getValue();

                // Batch process blocks in the same chunk at the same Y level
                for (BlockChange change : chunkChanges) {
                  Block block = world.getBlockAt(change.x, change.y, change.z);
                  block.setBlockData(change.blockData, false);
                }
              });
        });
  }

  /**
   * Enhanced error handling with recovery strategies
   */
  private BlockData createBlockDataWithFallback(String blockId) {
    try {
      return Bukkit.createBlockData(blockId);
    } catch (IllegalArgumentException e) {
      // Try fallback strategies
      String fallbackId = attemptBlockIdRecovery(blockId);
      if (fallbackId != null) {
        try {
          logger.info("Recovered block ID: " + blockId + " -> " + fallbackId);
          return Bukkit.createBlockData(fallbackId);
        } catch (Exception fallbackError) {
          // Fall through to final fallback
        }
      }

      // Final fallback with logging
      if (!loggedBlockErrors.contains(blockId)) {
        logger.warning("Unknown block ID: " + blockId + ", using AIR. Error: " + e.getMessage());
        loggedBlockErrors.add(blockId);
      }
      return Material.AIR.createBlockData();
    }
  }

  /**
   * Attempts to recover invalid block IDs using common patterns
   */
  private String attemptBlockIdRecovery(String blockId) {
    // Remove version-specific properties that might be incompatible
    if (blockId.contains("[")) {
      String baseName = blockId.split("\\[")[0];
      return baseName;
    }

    // Try adding minecraft namespace if missing
    if (!blockId.contains(":")) {
      return "minecraft:" + blockId;
    }

    // Try common block name mappings for legacy versions
    return switch (blockId) {
      case "minecraft:grass" -> "minecraft:grass_block";
      case "minecraft:dirt_path" -> "minecraft:dirt";
      default -> null;
    };
  }
}
