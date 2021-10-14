package jsheets.evaluation.sandbox.validation;

import java.util.Arrays;
import java.util.Objects;

import jsheets.evaluation.sandbox.access.AccessGraph;
import jsheets.evaluation.sandbox.access.AccessKey;
import jsheets.evaluation.sandbox.access.MethodSignature;
import jsheets.evaluation.sandbox.access.MethodSignatureBuilder;
import org.objectweb.asm.Type;

public final class ForbiddenMemberFilter implements Rule {
  public static ForbiddenMemberFilter create(AccessGraph accessGraph) {
    Objects.requireNonNull(accessGraph, "accessGraph");
    return new ForbiddenMemberFilter(accessGraph);
  }

  private final AccessGraph accessGraph;

  private ForbiddenMemberFilter(AccessGraph accessGraph) {
    this.accessGraph = accessGraph;
  }

  public record ForbiddenMethod(MethodSignature method) implements Analysis.Violation {}

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
      .returnType(call.type().getReturnType().getClassName())
      .parameterTypes(
        Arrays.stream(call.type().getArgumentTypes())
          .map(Type::getClassName)
          .toList()
      ).build();
  }

  public record ForbiddenField(String owner, String field) implements Analysis.Violation {}

  @Override
  public void visitFieldAccess(Analysis analysis, FieldAccess access) {
    var key = "%s.%s".formatted(access.owner(), access.field());
    if (!accessGraph.isPermitted(AccessKey.dotSeparated(key))) {
      analysis.report(new ForbiddenField(access.owner(), access.field()));
    }
  }
}