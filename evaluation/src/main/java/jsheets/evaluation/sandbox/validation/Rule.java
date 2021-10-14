package jsheets.evaluation.sandbox.validation;

import org.objectweb.asm.Type;

public interface Rule {
  record AccessPoint(String className, String methodName) { }

  record MethodCall(
    AccessPoint accessPoint,
    String owner,
    String method,
    Type type
  ) { }

  record FieldAccess(
    AccessPoint accessPoint,
    String owner,
    String field
  ) { }

  default void visitCall(Analysis analysis, MethodCall call) {}
  default void visitFieldAccess(Analysis analysis, FieldAccess access) {}
}
