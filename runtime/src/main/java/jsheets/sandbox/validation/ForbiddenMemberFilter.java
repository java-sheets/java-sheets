package jsheets.sandbox.validation;

import java.util.Objects;

import jsheets.sandbox.access.AccessGraph;
import jsheets.sandbox.access.AccessKey;
import jsheets.sandbox.access.MethodSignature;
import jsheets.sandbox.access.MethodSignatureBuilder;

public final class ForbiddenMemberFilter implements Rule {
  public static ForbiddenMemberFilter create(AccessGraph accessGraph) {
    Objects.requireNonNull(accessGraph, "accessGraph");
    return new ForbiddenMemberFilter(accessGraph);
  }

  private final AccessGraph accessGraph;

  private ForbiddenMemberFilter(AccessGraph accessGraph) {
    this.accessGraph = accessGraph;
  }

  record ForbiddenMethod(MethodSignature method) implements Analysis.Violation {}

  @Override
  public void visitCall(Analysis analysis, MethodCall call) {
    var signature = createSignatureOfCall(call);
    if (!accessGraph.isMethodPermitted(signature)) {
      analysis.report(new ForbiddenMethod(signature));
    }
  }

  private MethodSignature createSignatureOfCall(MethodCall call) {
    return MethodSignatureBuilder.builder()
      .className(call.owner())
      .methodName(call.method())
      .build();
  }

  record ForbiddenField(String owner, String field) implements Analysis.Violation {}

  @Override
  public void visitFieldAccess(Analysis analysis, FieldAccess access) {
    var key = "%s/%s".formatted(access.owner(), access.field());
    if (!accessGraph.isPermitted(AccessKey.slashSeparated(key))) {
      analysis.report(new ForbiddenField(access.owner(), access.field()));
    }
  }
}