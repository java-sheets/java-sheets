package jsheets.evaluation.sandbox.validation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import jsheets.EvaluationError;
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

  public record ForbiddenMethod(MethodSignature method) implements Analysis.Violation {
    @Override
    public Stream<EvaluationError> describe(Locale locale) {
      var message = method.isConstructor()
        ? formatForConstructor(locale)
        : formatForMethod(locale);
      return Stream.of(
        EvaluationError.newBuilder()
          .setKind("sandbox")
          .setMessage(message)
          .build()
      );
    }

    private String formatForConstructor(Locale locale) {
      return "The class %s is not allowed".formatted(method.className());
    }

    private String formatForMethod(Locale locale) {
      return "The method %s in %s is not allowed"
        .formatted(method.methodName(), method.className());
    }
  }

  @Override
  public void visitCall(Analysis analysis, MethodCall call) {
    if (isClassExcluded(call.owner())) {
      return;
    }
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

  public record ForbiddenField(String owner, String field) implements Analysis.Violation {
    @Override
    public Stream<EvaluationError> describe(Locale locale) {
      return Stream.of(
        EvaluationError.newBuilder()
          .setKind("sandbox")
          .setMessage("The field %s in %s is not allowed".formatted(field, owner))
          .build()
      );
    }
  }

  @Override
  public void visitFieldAccess(Analysis analysis, FieldAccess access) {
    if (isClassExcluded(access.owner())) {
      return;
    }
    var key = "%s.%s".formatted(access.owner(), access.field());
    if (!accessGraph.isPermitted(AccessKey.dotSeparated(key))) {
      analysis.report(new ForbiddenField(access.owner(), access.field()));
    }
  }

  private static final String generatedSnippetClassPrefix = "REPL.$JShell$";

  private boolean isClassExcluded(String className) {
    return className.startsWith(generatedSnippetClassPrefix);
  }
}