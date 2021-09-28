package jsheets.shell;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jsheets.output.CapturingOutput;
import jsheets.shell.execution.ExhaustiveExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ExhaustiveExecutionTest {
  private ExhaustiveExecutionTest() {}

  @Test
  public void testPrinting() {
    var log = new StringBuilder();
    var output = CapturingOutput.to(log::append);
    var shell = JShell.builder().out(output).err(System.err).build();
    var execution = ExhaustiveExecution.create(shell);
    var created = execution.execute(
      """
      System.out.println("[");
      for (int x = 0; x < 3; x++) {
        if (x != 0) System.out.print(", ");
        System.out.print(x);
      }
      System.out.println("]");
      """
    );
    Assertions.assertEquals(1, created.size());
    Assertions.assertEquals(Snippet.Status.VALID, created.iterator().next().status());
    Assertions.assertEquals("[1, 2, 3]", log.toString());
  }
}