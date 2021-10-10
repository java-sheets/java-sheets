package jsheets.sandbox;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MethodSignatureTest {
  @Test
  public void testParsing() {
    Assertions.assertEquals(
      MethodSignatureBuilder.builder()
        .className("java/lang/System")
        .methodName("exit")
        .returnType("void")
        .parameterTypes(List.of("int"))
        .build(),
      MethodSignature.parse("void java/lang/System#exit(int)")
    );
    Assertions.assertEquals(
      MethodSignatureBuilder.builder()
        .className("java/lang/String")
        .methodName("join")
        .returnType("void")
        .parameterTypes(List.of("java/lang/CharSequence", "java/lang/Iterable"))
        .build(),
      MethodSignature.parse("java/lang/String#join(java/lang/CharSequence, java/lang/Iterable)")
    );
  }
}
