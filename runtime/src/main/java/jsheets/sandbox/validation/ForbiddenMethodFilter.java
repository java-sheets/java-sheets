package jsheets.sandbox.validation;

public final class ForbiddenMethodFilter implements Rule {
  public static ForbiddenMethodFilter create() {
    return new ForbiddenMethodFilter();
  }

  private ForbiddenMethodFilter() {}

  @Override
  public void visitCall(Analysis analysis, MethodCall call) {
    System.out.println(call);
  }
}