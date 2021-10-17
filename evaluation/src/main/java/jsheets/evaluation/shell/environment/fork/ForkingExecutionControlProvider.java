package jsheets.evaluation.shell.environment.fork;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.sun.jdi.VirtualMachine;
import jdk.jshell.execution.JdiInitiator;
import jdk.jshell.execution.RemoteExecutionControl;
import jdk.jshell.execution.Util;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jsheets.evaluation.shell.environment.ClassFileStore;
import jsheets.evaluation.shell.environment.EmptyClassFileStore;

import static jdk.jshell.execution.Util.remoteInputOutput;

public final class ForkingExecutionControlProvider
  implements ExecutionControlProvider {

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static ForkingExecutionControlProvider create() {
    return create(
      List.of(),
      EmptyClassFileStore.create(),
      Executors.newScheduledThreadPool(
        1,
        new ThreadFactoryBuilder().setDaemon(true).build()
      )
    );
  }

  private static final Duration defaultTimeout = Duration.ofMillis(3000);
  private static final Duration defaultExecutionTimeout =
    Duration.ofSeconds(30);

  public static ForkingExecutionControlProvider create(
    Collection<String> rawVirtualMachineOptions,
    ClassFileStore classFileStore,
    ScheduledExecutorService scheduler
  ) {
    Objects.requireNonNull(classFileStore, "classFileStore");
    Objects.requireNonNull(rawVirtualMachineOptions, "rawVirtualMachineOptions");
    return new ForkingExecutionControlProvider(
      defaultExecutionTimeout,
      defaultTimeout,
      List.copyOf(rawVirtualMachineOptions),
      classFileStore,
      scheduler
    );
  }

  private final Duration connectTimeout;
  private final Duration executionTimeout;
  private final List<String> rawVirtualMachineOptions;
  private final ScheduledExecutorService scheduler;
  private final ClassFileStore classFileStore;

  private ForkingExecutionControlProvider(
    Duration executionTimeout,
    Duration connectTimeout,
    List<String> rawVirtualMachineOptions,
    ClassFileStore classFileStore,
    ScheduledExecutorService scheduler
  ) {
    this.executionTimeout = executionTimeout;
    this.connectTimeout = connectTimeout;
    this.rawVirtualMachineOptions = rawVirtualMachineOptions;
    this.classFileStore = classFileStore;
    this.scheduler = scheduler;
  }

  @Override
  public String name() {
    return "forking-execution-control-provider";
  }

  @Override
  public Map<String, String> defaultParameters() {
    return Map.of();
  }

  @Override
  public ExecutionControl generate(
    ExecutionEnv environment,
    Map<String, String> specialParameters
  ) throws IOException {
    return create(environment);
  }

  record Box(VirtualMachine machine, Process process) {}

  private static final String remoteAgentClassName =
    RemoteExecutionControl.class.getName();

  private Box initiate(int port) {
    var initiator = new JdiInitiator(
      /* port */ port,
      /* options */ rawVirtualMachineOptions,
      /* remoteAgentClassName */ remoteAgentClassName,
      /* controlledLaunch */ false,
      /* host */ "",
      /* timeout */ (int) connectTimeout.toMillis(),
      /* connectorOptions*/ Collections.emptyMap()
    );
    return new Box(
      initiator.vm(),
      initiator.process()
    );
  }

  private static final int backlog = 1;

  ExecutionControl create(ExecutionEnv environment) throws IOException {
    var address = InetAddress.getLoopbackAddress();
    try (var listener = new ServerSocket(0, backlog, address)) {
      listener.setSoTimeout((int) connectTimeout.toMillis());
      var box = initiate(listener.getLocalPort());
      return accept(listener, environment, box);
    }
  }

  private ExecutionControl accept(
    ServerSocket listener,
    ExecutionEnv environment,
    Box box
  ) throws IOException {
    var hooks = registerCloseHooks(box.machine());
    var socket = listener.accept();
    var output = socket.getOutputStream();
    return remoteInputOutput(
      socket.getInputStream(),
      output,
      createOutputs(environment),
      createInputs(environment),
      createControl(box, environment, hooks)
    );
  }

  private Collection<Consumer<String>> registerCloseHooks(VirtualMachine machine) {
    var hooks = new CopyOnWriteArrayList<Consumer<String>>();
    Util.detectJdiExitEvent(machine, event -> {
      for (var hook : hooks) {
        hook.accept(event);
      }
    });
    return hooks;
  }

  private BiFunction<ObjectInput, ObjectOutput, ExecutionControl> createControl(
    Box box,
    ExecutionEnv environment,
    Collection<Consumer<String>> hooks
  ) {
    return (input, output) -> {
      var control = new ForkedExecutionControl(
        output,
        input,
        box.machine(),
        box.process(),
        remoteAgentClassName,
        classFileStore
      );
      hooks.add(event -> environment.closeDown());
      hooks.add(event -> control.disposeMachine());
      scheduleExecutionTimeout(control);
      return control;
    };
  }

  private void scheduleExecutionTimeout(ForkedExecutionControl control) {
    scheduler.schedule(() -> {
      if (!control.isClosed()) {
        log.atInfo().log("stopping long running remote");
        try {
          control.stop();
        } catch (Exception failedStop) {
          log.atWarning()
            .withCause(failedStop)
            .atMostEvery(5, TimeUnit.SECONDS)
            .log("failed to stop long running remote");
        }
        control.close();
      }
    }, executionTimeout.toMillis(), TimeUnit.MILLISECONDS);
  }

  private Map<String, OutputStream> createOutputs(ExecutionEnv environment) {
    return Map.of(
      "out", environment.userOut(),
      "err", environment.userErr()
    );
  }

  private Map<String, InputStream> createInputs(ExecutionEnv environment) {
    return Map.of("in", environment.userIn());
  }
}
