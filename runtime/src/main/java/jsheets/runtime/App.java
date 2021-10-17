package jsheets.runtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.google.common.flogger.FluentLogger;

import com.google.inject.Guice;
import com.google.inject.Injector;

import jdk.jshell.JShell;
import jsheets.evaluation.sandbox.access.AccessGraph;
import jsheets.evaluation.sandbox.validation.ForbiddenMemberFilter;
import jsheets.evaluation.shell.environment.fork.ForkingExecutionControlProvider;
import jsheets.evaluation.shell.environment.sandbox.SandboxClassFileCheck;
import jsheets.runtime.evaluation.EvaluationModule;

public final class App {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private App() {}

  public static void main(String[] options) {
    configureLogging();
    var injector = configureInjector();
    launch(injector);
  }

  private static void launch(Injector injector) {
    var setup = injector.getInstance(ServerSetup.class);
    try {
      setup.start();
    } catch (IOException failure) {
      log.atSevere()
        .withCause(failure)
        .log("encountered error while serving");
    } catch (InterruptedException interruption) {
      log.atSevere()
        .withCause(interruption)
        .log("interrupted while serving");
    }
  }

  private static Injector configureInjector() {
    return Guice.createInjector(
      ServerSetupModule.create(),
      ConfigModule.create(),
      ZookeeperModule.create(),
      EvaluationModule.create()
    );
  }

  private static void configureLogging() {
    System.setProperty(
      "flogger.backend_factory",
      "com.google.common.flogger.backend.slf4j.Slf4jBackendFactory#getInstance"
    );
  }
}