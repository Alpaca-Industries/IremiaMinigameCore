package org.alpacaindustries.iremiaminigame.util.schem;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * High-performance decoder for VarInt data used in Minecraft schematics.
 * Optimized for speed and memory efficiency.
 */
public class VarIntDecoder {
  private static final Logger logger = Logger.getLogger(VarIntDecoder.class.getName());

  /**
   * Decodes varint-encoded data into an array of integers with optimized path for
   * common cases.
   *
   * @param data         ByteArray containing varint encoded integers
   * @param expectedSize Expected number of integers to decode
   * @return IntArray containing the decoded integers
   */
  public int[] decodeVarints(byte[] data, int expectedSize) {
    if (expectedSize <= 0) {
      return new int[0];
    }

    int[] result = new int[expectedSize];
    int dataSize = data.length;
    int index = 0;
    int position = 0;

    while (position < dataSize && index < expectedSize) {
      int firstByte = data[position++] & 0xFF;

      // Fast path for single-byte VarInts
      if ((firstByte & 0x80) == 0) {
        result[index++] = firstByte;
        continue;
      }

      // Decode multi-byte VarInts
      int value = firstByte & 0x7F;
      int shift = 7;
      while (position < dataSize && shift <= 28) {
        int currentByte = data[position++] & 0xFF;
        value = value | ((currentByte & 0x7F) << shift);
        if ((currentByte & 0x80) == 0) {
          break;
        }
        shift += 7;
      }

      // Handle invalid or truncated VarInts
      if (shift > 28) {
        logger.warning("Invalid VarInt at position " + (position - 1));
        result[index++] = 0;
      } else {
        result[index++] = value;
      }
    }

    // Handle case where we didn't decode enough values
    if (index < expectedSize) {
      int missing = expectedSize - index;
      if (missing > 100) { // Only log if significant
        logger.warning("Expected " + expectedSize + " values but only decoded " + index +
            " (missing " + missing + "). Filling with zeros.");
      }
      Arrays.fill(result, index, expectedSize, 0);
    }

    return result;
  }

  /**
   * Process VarInts in parallel for large data sets. Only use this method for
   * arrays with more than
   * 100,000 expected values.
   */
  public int[] decodeVarintsParallel(byte[] data, int expectedSize) {
    if (expectedSize < 100_000) {
      return decodeVarints(data, expectedSize);
    }

    int[] result = new int[expectedSize];
    int chunkSize = 16_384; // Optimal chunk size found through benchmarking
    int[] positions = precomputePositions(data, expectedSize);

    IntStream.range(0, (expectedSize + chunkSize - 1) / chunkSize)
        .parallel()
        .forEach(chunkIndex -> {
          int startIndex = chunkIndex * chunkSize;
          int endIndex = Math.min(startIndex + chunkSize, expectedSize);

          for (int i = startIndex; i < endIndex; i++) {
            if (i >= positions.length - 1) {
              break;
            }

            int start = positions[i];
            int end = positions[i + 1];
            if (start >= 0 && end >= 0 && start < data.length) {
              result[i] = decodeVarIntAt(data, start, end - start);
            }
          }
        });

    return result;
  }

  /**
   * Decode a single VarInt starting at a specific position with known max length.
   * This is optimized for the parallel decoder.
   */
  private int decodeVarIntAt(byte[] data, int position, int maxLength) {
    int value = 0;
    int bitPosition = 0;
    int pos = position;
    int limit = Math.min(position + maxLength, data.length);

    while (pos < limit && bitPosition <= 28) {
      int currentByte = data[pos++] & 0xFF;
      value = value | ((currentByte & 0x7F) << bitPosition);
      if ((currentByte & 0x80) == 0) {
        break;
      }
      bitPosition += 7;
    }

    return value;
  }

  /**
   * Precompute VarInt positions in the byte array for parallel processing.
   * Returns array where each
   * element is the starting position of a VarInt.
   */
  private int[] precomputePositions(byte[] data, int expectedSize) {
    java.util.List<Integer> positions = new java.util.ArrayList<>();
    int dataIndex = 0;

    while (dataIndex < data.length && positions.size() < expectedSize) {
      positions.add(dataIndex);

      // Skip this VarInt
      while (dataIndex < data.length) {
        if ((data[dataIndex++] & 0x80) == 0)
          break;
      }
    }

    // Add a final position marker
    positions.add(dataIndex);

    // Convert to IntArray
    return positions.stream().mapToInt(Integer::intValue).toArray();
  }
}
