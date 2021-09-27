package jsheets.runtime.evaluation.shell;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jsheets.runtime.evaluation.shell.environment.inprocess.EmbeddedEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public final class ShellCompilationTest {
  @Test
  public void testMultiline() {
    var shell = JShell.builder()
      .executionEngine(EmbeddedEnvironment.create().control("test"), Map.of())
      .err(System.err)
      .out(System.out)
      .build();
    var source = """
      int x = 10;
      System.out.println("a");
      System.out.println("b");
      """;
    var snippet = shell.eval(source);
    var first = snippet.get(0).snippet();
    Assertions.assertEquals(new Range(0, 10), range(first));
  }

  record Range(int begin, int end) {}

  private static Range range(Snippet snippet) {
    try {
      var wrapAccessor = Snippet.class.getDeclaredMethod("guts");
      wrapAccessor.setAccessible(true);
      var wrap = wrapAccessor.invoke(snippet);
      System.out.println(wrap);
      return new Range(0, 0);
    } catch (Exception failedAccess) {
      throw new RuntimeException(failedAccess);
    }
  }
}
