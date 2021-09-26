package jsheets.server.endpoint;

import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonProtoRequests {
  private static final Map<Class<? extends Message>, Message> defaultMessages
    = new ConcurrentHashMap<>();

  private static final JsonFormat.Parser parser = JsonFormat.parser()
    .ignoringUnknownFields();

  public static <T extends Message> T parse(Class<T> type, String text) {
    var builder = resolveBuilder(type);
    try {
      parser.merge(text, builder);
    } catch (InvalidProtocolBufferException malformedInput) {
      throw new IllegalArgumentException("invalid json input", malformedInput);
    }
    @SuppressWarnings("unchecked")
    var result = (T) builder.build();
    return result;
  }

  private static Message.Builder resolveBuilder(Class<? extends Message> type) {
    try {
      return resolveDefaultMessage(type).toBuilder();
    } catch (RuntimeException failedLookup) {
      throw new IllegalArgumentException("type is not a valid message", failedLookup);
    }
  }

  private static Message resolveDefaultMessage(Class<? extends Message> type) {
    return defaultMessages.computeIfAbsent(type, Internal::getDefaultInstance);
  }
}
