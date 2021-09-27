package jsheets.runtime.evaluation.shell;

import java.time.Clock;
import java.util.UUID;

import jsheets.EvaluateResponse;
import jsheets.EvaluatedSnippet;
import jsheets.Snippet;
import jsheets.SnippetSources;
import jsheets.StartEvaluationRequest;
import jsheets.runtime.evaluation.Evaluation;
import jsheets.runtime.evaluation.shell.environment.StandardExecution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/* This is not a Unit Test */
public class ShellEvaluationEngineTest {
  static SnippetSources.CodeComponent code(String id, String content) {
    return SnippetSources.CodeComponent.newBuilder()
      .setId(id)
      .setCode(content)
      .build();
  }

  @Test
  // @Disabled
  public void testExecution() {
    var environment = StandardExecution.create();
    var installation = environment.install();
    var engine = ShellEvaluationEngine.newBuilder()
      .useClock(Clock.systemUTC())
      .useEnvironment(environment)
      .useWorkerPool(Runnable::run)
      .create();
    var request = StartEvaluationRequest.newBuilder()
      .setSnippet(
        EvaluatedSnippet.newBuilder()
          .setHash("1")
          .setReference(
            Snippet.Reference.newBuilder()
              .setSheetId(UUID.randomUUID().toString())
              .setSnippetId(UUID.randomUUID().toString())
              .build()
          ).build()
      ).addSources(
        SnippetSources.newBuilder()
          .setHash("")
          .setReference(
            Snippet.Reference.newBuilder()
              .setSheetId(UUID.randomUUID().toString())
              .setSnippetId(UUID.randomUUID().toString())
              .build()
          )
          .addCodeComponents(code("0", "1 + 1"))
          .addCodeComponents(code("1", "1 + 2"))
          .addCodeComponents(code("2", "int x = 10;"))
          .addCodeComponents(code("3", "x * x"))
          .addCodeComponents(code("4", "lol"))
          .addCodeComponents(code("5", "class Test {  }"))
          .addCodeComponents(code("6", "new Test().toString()"))
          .addCodeComponents(code("7", """
            System.out.println("Hello, World!");
            System.out.println("Hello, World!");
            """))
          .build()
      ).build();

    engine.start(request, new Evaluation.Listener() {
      @Override
      public void send(EvaluateResponse response) {
        System.out.println(response);
      }

      @Override
      public void close() {
        System.out.println("closed");
        installation.close();
      }
    });
  }
}