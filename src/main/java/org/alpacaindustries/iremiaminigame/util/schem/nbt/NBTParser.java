package org.alpacaindustries.iremiaminigame.util.schem.nbt;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class NBTParser {
  private static final Logger logger = Logger.getLogger(NBTParser.class.getName());

  // Constants for NBT tag types
  private static final int TAG_END = 0;
  private static final int TAG_BYTE = 1;
  private static final int TAG_SHORT = 2;
  private static final int TAG_INT = 3;
  private static final int TAG_LONG = 4;
  private static final int TAG_FLOAT = 5;
  private static final int TAG_DOUBLE = 6;
  private static final int TAG_BYTE_ARRAY = 7;
  private static final int TAG_STRING = 8;
  private static final int TAG_LIST = 9;
  private static final int TAG_COMPOUND = 10;
  private static final int TAG_INT_ARRAY = 11;
  private static final int TAG_LONG_ARRAY = 12;

  /**
   * Reads a full NBT compound from a DataInputStream
   * Returns a Map<String, Object> where Object can be any primitive type, array,
   * List, or nested Map
   */
  public Map<String, Object> readNBT(DataInputStream input) throws IOException {
    Map<String, Object> result = new HashMap<>();

    while (true) {
      int typeId = input.readByte() & 0xFF;
      if (typeId == TAG_END)
        break;

      String name = readString(input);
      Object value = readTagByType(input, typeId);
      result.put(name, value);
    }

    return result;
  }

  /**
   * Reads a tag value by its type ID - returns native Java types
   * Using modern Java 21 switch expressions
   */
  private Object readTagByType(DataInputStream input, int typeId) throws IOException {
    return switch (typeId) {
      case TAG_BYTE -> input.readByte();
      case TAG_SHORT -> input.readShort();
      case TAG_INT -> input.readInt();
      case TAG_LONG -> input.readLong();
      case TAG_FLOAT -> input.readFloat();
      case TAG_DOUBLE -> input.readDouble();
      case TAG_BYTE_ARRAY -> readByteArray(input);
      case TAG_STRING -> readString(input);
      case TAG_LIST -> readList(input);
      case TAG_COMPOUND -> readNBT(input);
      case TAG_INT_ARRAY -> readIntArray(input);
      case TAG_LONG_ARRAY -> readLongArray(input);
      default -> throw new IllegalArgumentException("Unsupported NBT tag type: " + typeId);
    };
  }

  /**
   * Reads a UTF-8 string from the input
   */
  private String readString(DataInputStream input) throws IOException {
    int length = input.readShort() & 0xFFFF;
    if (length > 0) {
      byte[] bytes = new byte[length];
      input.readFully(bytes);
      return new String(bytes, StandardCharsets.UTF_8);
    } else {
      return "";
    }
  }

  /**
   * Reads a byte array from the input
   */
  private byte[] readByteArray(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length > 0) {
      byte[] array = new byte[length];
      input.readFully(array);
      return array;
    } else {
      return new byte[0];
    }
  }

  /**
   * Reads an integer array from the input
   */
  private int[] readIntArray(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length > 0) {
      int[] array = new int[length];
      for (int i = 0; i < length; i++) {
        array[i] = input.readInt();
      }
      return array;
    } else {
      return new int[0];
    }
  }

  /**
   * Reads a long array from the input
   */
  private long[] readLongArray(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length > 0) {
      long[] array = new long[length];
      for (int i = 0; i < length; i++) {
        array[i] = input.readLong();
      }
      return array;
    } else {
      return new long[0];
    }
  }

  /**
   * Reads a list from the input - returns List<Object>
   */
  private List<Object> readList(DataInputStream input) throws IOException {
    int listType = input.readByte() & 0xFF;
    int length = input.readInt();

    List<Object> list = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      Object value = readListElementByType(input, listType);
      list.add(value);
    }
    return list;
  }

  /**
   * Reads a list element by type - returns native Java types
   * Using modern Java 21 switch expressions
   */
  private Object readListElementByType(DataInputStream input, int listType) throws IOException {
    return switch (listType) {
      case TAG_BYTE -> input.readByte();
      case TAG_SHORT -> input.readShort();
      case TAG_INT -> input.readInt();
      case TAG_LONG -> input.readLong();
      case TAG_FLOAT -> input.readFloat();
      case TAG_DOUBLE -> input.readDouble();
      case TAG_STRING -> readString(input);
      case TAG_COMPOUND -> readNBT(input);
      default -> throw new IllegalArgumentException("Unsupported NBT list type: " + listType);
    };
  }

  /**
   * Helper function to get integer values from NBT data
   * Using Java 21 pattern matching with instanceof
   */
  public Integer getIntValue(Map<String, Object> compound, String key) {
    Object value = compound.get(key);
    if (value == null)
      return null;

    return switch (value) {
      case Integer i -> i;
      case String s -> {
        try {
          yield Integer.parseInt(s);
        } catch (NumberFormatException e) {
          yield null;
        }
      }
      case Short s -> s.intValue();
      case Byte b -> b.intValue();
      default -> {
        logger.warning("Unexpected type for " + key + ": " + value.getClass().getSimpleName());
        yield null;
      }
    };
  }
}
