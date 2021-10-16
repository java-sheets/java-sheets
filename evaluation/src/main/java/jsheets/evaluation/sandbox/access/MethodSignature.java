package jsheets.evaluation.sandbox.access;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@RecordBuilder
public record MethodSignature(
  String className,
  String methodName,
  String returnType,
  Collection<String> parameterTypes
) {
  private static final char methodSeparator = '#';

  public static char methodSeparator() {
    return methodSeparator;
  }

  public static MethodSignature parse(String input) {
    var trimmed = input.trim().replace("/", ".");
    var methodNameBegin = trimmed.indexOf(methodSeparator);
    if (methodNameBegin < 0) {
      throw new IllegalArgumentException(
        "input does not contain method name (separated by #)"
      );
    }
    return parsePreprocessed(trimmed, methodNameBegin);
  }

  private static final String wildcardParameter = "*";

  private static final Collection<String> wildcardParameters =
    List.of(wildcardParameter);

  private static MethodSignature parsePreprocessed(String input, int methodNameBegin) {
    var className = input.substring(0, methodNameBegin);
    var returnType = parseReturnType(input);
    var parameterListBegin = input.indexOf('(');
    if (parameterListBegin < 0) {
      var methodName = input.substring(methodNameBegin + 1);
      return new MethodSignature(className, methodName, returnType, wildcardParameters);
    }
    var parameterListEnd = input.lastIndexOf(')');
    var methodName = input.substring(methodNameBegin + 1, parameterListBegin);
    var parameterPart = input.substring(parameterListBegin + 1, parameterListEnd);
    var parameterTypes = parseParameterTypes(parameterPart);
    return new MethodSignature(className, methodName, returnType, parameterTypes);
  }

  private static final String wildcardReturnType = "*";

  private static String parseReturnType(String input) {
    int returnTypeBegin = input.indexOf(':');
    return returnTypeBegin < 0
      ? wildcardReturnType
      : input.substring(returnTypeBegin + 1);
  }

  private static final Pattern parameterTypeSeparator = Pattern.compile(",\\s*");

  private static Collection<String> parseParameterTypes(String input) {
    return List.of(parameterTypeSeparator.split(input));
  }

  public MethodSignature {
    Objects.requireNonNull(className, "className");
    Objects.requireNonNull(methodName, "methodName");
    Objects.requireNonNull(returnType, "returnType");
    Objects.requireNonNull(parameterTypes, "parameterTypes");
  }

  public boolean matches(MethodSignature signature) {
    return signature.className.equals(className)
      && signature.methodName.equals(methodName)
      && (hasWildcardParameters() || signature.parameterTypes.equals(parameterTypes))
      && (hasWildcardReturnType() || signature.returnType.equals(returnType));
  }

  private boolean hasWildcardReturnType() {
    return wildcardReturnType.equals(returnType);
  }

  public boolean hasWildcardParameters() {
    return parameterTypes.size() == 1
      && parameterTypes.iterator().next().equals(wildcardParameter);
  }

  public String format() {
    return "%s#%s:%s".formatted(
      className,
      formatNameAndParameters(),
      returnType
    );
  }

  private static final String constructorName = "<init>";

  public boolean isConstructor() {
    return methodName.equals(constructorName);
  }

  public String formatNameAndParameters() {
    return "%s(%s)".formatted(methodName, String.join(", ", parameterTypes));
  }

  public String formatWithoutTypes() {
    return "%s#%s".formatted(className, methodName);
  }
}
