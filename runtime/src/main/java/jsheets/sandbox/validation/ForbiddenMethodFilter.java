package jsheets.sandbox.validation;

public final class ForbiddenMethodFilter implements Rule {
  private ForbiddenMethodFilter() {}

  @Override
  public void visitCall(Analysis analysis, MethodCall call) {
    System.out.println(call);
  }
}