package jsheets.evaluation.shell;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import jsheets.EvaluateResponse;
import jsheets.EvaluatedSnippet;
import jsheets.Snippet;
import jsheets.SnippetSources;
import jsheets.StartEvaluationRequest;
import jsheets.config.FileConfigSource;
import jsheets.evaluation.Evaluation;
import jsheets.evaluation.shell.environment.StandardEnvironment;
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
    var environment = StandardEnvironment.create();
    var installation = environment.install();
    var engine = ShellEvaluationEngine.newBuilder()
      .useEnvironment(environment)
      .useWorkerPool(Runnable::run)
      .useBuiltinImports(List.of("java.util.*"))
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
            System.out.println(List.of());
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