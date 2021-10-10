package jsheets.sandbox;

import jdk.jshell.JShell;
import org.junit.jupiter.api.Test;

import java.util.Map;

public final class SandboxExecutionTest {
  @Test
  public void test() {
    var shell = createSandboxedShell();
    shell.eval("System.out.println(\"Hello, World!\");");
  }

  private JShell createSandboxedShell() {
    return JShell.builder()
      .out(System.out)
      .err(System.err)
      .executionEngine(SandboxedEnvironment.create(), Map.of())
      .build();
  }
}
