package jsheets.sandbox;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@RecordBuilder
record MethodSignature(
  String className,
  String methodName,
  String returnType,
  Collection<String> parameterTypes
) {
  private static final char methodSeparator = '#';

  public static char methodSeparator() {
    return methodSeparator;
  }

  private static final String defaultReturnType = "void";

  public static MethodSignature parse(String input) {
    var trimmed = input.trim();
    int returnTypeEnd = findBeforeMethodName(trimmed, ' ');
    if (returnTypeEnd < 0) {
      return parseWithReturnType(input, defaultReturnType);
    }
    var returnType = trimmed.substring(0, returnTypeEnd);
    var remaining = trimmed.substring(returnTypeEnd + 1);
    return parseWithReturnType(remaining, returnType);
  }

  private static int findBeforeMethodName(String input, char character) {
    int firstSpace = input.indexOf(character);
    int methodNameBegin = input.indexOf(methodSeparator);
    return firstSpace < methodNameBegin ? firstSpace : -1;
  }

  private static MethodSignature parseWithReturnType(String input, String returnType) {
    var methodNameBegin = input.indexOf(methodSeparator);
    if (methodNameBegin < 0) {
      throw new IllegalArgumentException(
        "input does not contain method name (separated by #)"
      );
    }
    var className = input.substring(0, methodNameBegin);
    var parameterListBegin = input.indexOf('(');
    if (parameterListBegin < 0) {
      var methodName = input.substring(methodNameBegin + 1);
      return new MethodSignature(className, methodName, returnType, List.of());
    }
    var parameterListEnd = input.lastIndexOf(')');
    var methodName = input.substring(methodNameBegin + 1, parameterListBegin);
    var parameterPart = input.substring(parameterListBegin + 1, parameterListEnd);
    var parameterTypes = parseParameterTypes(parameterPart);
    return new MethodSignature(className, methodName, returnType, parameterTypes);
  }

  private static final Pattern parameterTypeSeparator = Pattern.compile(",\\s*");

  private static Collection<String> parseParameterTypes(String input) {
    return List.of(parameterTypeSeparator.split(input));
  }

  public boolean matches(MethodSignature signature) {
    return equals(signature);
  }

  public String format() {
    return "%s %s#%s(%s)".formatted(
      returnType,
      className,
      methodName,
      Arrays.toString(parameterTypes.toArray())
    );
  }

  public String formatWithoutTypes() {
    return "%s#%s".formatted(className, methodName);
  }
}
