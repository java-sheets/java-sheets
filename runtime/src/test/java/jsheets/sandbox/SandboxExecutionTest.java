package jsheets.sandbox;

import jdk.jshell.JShell;
import jsheets.sandbox.access.AccessGraph;
import jsheets.sandbox.validation.ForbiddenMemberFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public final class SandboxExecutionTest {
  @Test
  public void test() {
    var shell = createSandboxedShell();
    shell.eval("""
      System.out.println("Hello, World!");
      System.err.println("Hello, World!");
    """);
  }

  private JShell createSandboxedShell() {
    var accessGraph = AccessGraph.of(
      "java.lang.Object",
      "java.lang.System.out",
      "java.io.PrintStream#println"
    );
    System.out.println(accessGraph);
    return JShell.builder()
      .out(System.out)
      .err(System.err)
      .executionEngine(
        SandboxedEnvironment.create(List.of(ForbiddenMemberFilter.create(accessGraph))),
        Map.of()
      ).build();
  }
}
