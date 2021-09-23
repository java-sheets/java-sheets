package jsheets.sandbox.validation;

public interface Rule {
  record MethodCall(String owner, String method) { }

  default void visitCall(Analysis analysis, MethodCall call) {}
}
