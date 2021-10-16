package jsheets.evaluation.shell;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jsheets.output.CapturingOutput;
import jsheets.evaluation.shell.execution.ExhaustiveExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;

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
      System.out.print("[");
      for (int x = 0; x < 3; x++) {
        if (x != 0) System.out.print(", ");
        System.out.print(x);
      }
      System.out.print("]");
      """
    );
    Assertions.assertEquals(1, created.size());
    Assertions.assertEquals(Snippet.Status.VALID, created.iterator().next().status());
    Assertions.assertEquals("[0, 1, 2]", log.toString());
  }

  private JShell createSilentShell() {
    var output = new PrintStream(OutputStream.nullOutputStream());
    return JShell.builder().out(output).err(output).build();
  }

  @Test
  public void testSuccessfulCompilation() {
    var events = ExhaustiveExecution.create(createSilentShell())
      .execute("""
        int x = 0;
        for (x = 0; x < 100; x++) {
          System.out.println(x);
        }
        int y = x;
        for (int z = 0; z < 100; z++) {
          System.out.println(x * y * z);
        }
        """);
    Assertions.assertEquals(2, events.size());
    for (var event : events) {
      Assertions.assertEquals(Snippet.Status.VALID, event.status());
    }
  }

  @Test
  public void testEmpty() {
    var events = ExhaustiveExecution.create(createSilentShell())
      .execute("");
    Assertions.assertEquals(0, events.size());
  }

  @Test
  public void testInvalid() {
    var events = ExhaustiveExecution.create(createSilentShell())
      .execute("callWithoutClosedParen(someArgument, 1, 2, 3");
    Assertions.assertEquals(1, events.size());
  }
}