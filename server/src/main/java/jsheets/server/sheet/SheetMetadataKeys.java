package jsheets.server.sheet;

import com.google.common.flogger.MetadataKey;

public final class SheetMetadataKeys {
  private SheetMetadataKeys() {}

  private static final MetadataKey<String> idMetadataKey =
    MetadataKey.single("sheetId", String.class);

  public static MetadataKey<String> idKey() {
    return idMetadataKey;
  }
}
