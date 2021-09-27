package jsheets.shell;

import jdk.jshell.JShell;
import jsheets.shell.execution.ExhaustiveExecution;
import org.junit.jupiter.api.Test;

public final class ExhaustiveExecutionTest {
  private ExhaustiveExecutionTest() {}

  @Test
  public void testPrinting() {
    var shell = JShell.builder().out(System.out).err(System.err).build();
    var execution = ExhaustiveExecution.create(shell);
    execution.execute(
     """
     System.out.println("HI");
     System.out.println("QETZ");
     """
    );
  }
}