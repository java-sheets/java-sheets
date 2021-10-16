package jsheets.evaluation.sandbox;

import jdk.jshell.JShell;
import jsheets.evaluation.sandbox.access.AccessGraph;
import jsheets.evaluation.sandbox.validation.Analysis;
import jsheets.evaluation.sandbox.validation.ForbiddenMemberFilter;
import jsheets.evaluation.shell.environment.sandbox.SandboxedEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public final class SandboxExecutionTest {
  @Test
  public void testPermitted() {
    var shell = createSandboxedShell();
    shell.eval("System.out.println(\"Hello, World!\")");
  }

  @Test
  public void testDenied() {
    var shell = createSandboxedShell();
    try {
      shell.eval("System.err.println(\"Hello, World!\")");
    } catch (Exception failure) {
      var violations = Analysis.captureViolations(failure).toList();
      Assertions.assertEquals(
        List.of(
          new ForbiddenMemberFilter.ForbiddenField("java.lang.System", "err")
        ),
        violations
      );
    }
  }

  private JShell createSandboxedShell() {
    var accessGraph = AccessGraph.of(
      "java.lang.Object",
      "java.lang.System.out",
      "java.io.PrintStream#println"
    );
    return JShell.builder()
      .out(System.out)
      .err(System.err)
      .executionEngine(
        SandboxedEnvironment.create(List.of(ForbiddenMemberFilter.create(accessGraph))),
        Map.of()
      ).build();
  }
}
