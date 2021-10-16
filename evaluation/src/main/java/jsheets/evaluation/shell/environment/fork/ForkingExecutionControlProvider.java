package jsheets.evaluation.shell.environment.fork;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import com.sun.jdi.VirtualMachine;
import jdk.jshell.execution.JdiInitiator;
import jdk.jshell.execution.RemoteExecutionControl;
import jdk.jshell.execution.Util;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jsheets.evaluation.shell.environment.ClassFileStore;

import static jdk.jshell.execution.Util.remoteInputOutput;

public final class ForkingExecutionControlProvider
  implements ExecutionControlProvider {

  private static final Duration defaultTimeout = Duration.ofMillis(3000);

  public static ForkingExecutionControlProvider create(
    Collection<String> rawVirtualMachineOptions,
    ClassFileStore classFileStore
  ) {
    Objects.requireNonNull(classFileStore, "classFileStore");
    Objects.requireNonNull(rawVirtualMachineOptions, "rawVirtualMachineOptions");
    var host = InetAddress.getLoopbackAddress().getHostName();
    return new ForkingExecutionControlProvider(
      host,
      defaultTimeout,
      List.copyOf(rawVirtualMachineOptions),
      classFileStore
    );
  }

  private final String host;
  private final Duration timeout;
  private final List<String> rawVirtualMachineOptions;
  private final ClassFileStore classFileStore;

  private ForkingExecutionControlProvider(
    String host,
    Duration timeout,
    List<String> rawVirtualMachineOptions,
    ClassFileStore classFileStore
  ) {
    this.host = host;
    this.timeout = timeout;
    this.rawVirtualMachineOptions = rawVirtualMachineOptions;
    this.classFileStore = classFileStore;
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
      /* controlledLaunch */ true,
      /* host */ host,
      /* timeout */ (int) timeout.toMillis(),
      /* connectorOptions*/ Map.of()
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
      listener.setSoTimeout((int) timeout.toMillis());
      var box = initiate(listener.getLocalPort());
      return accept(listener, environment, box);
    }
  }

  private ExecutionControl accept(
    ServerSocket listener,
    ExecutionEnv environment,
    Box box
  ) throws IOException {
    var socket = listener.accept();
    var output = socket.getOutputStream();
    return remoteInputOutput(
      socket.getInputStream(),
      output,
      createOutputs(environment),
      createInputs(environment),
      createControl(box, environment)
    );
  }

  private BiFunction<ObjectInput, ObjectOutput, ExecutionControl> createControl(
    Box box,
    ExecutionEnv environment
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
      registerCloseHooks(box.machine(), environment, control);
      return control;
    };
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

  private void registerCloseHooks(
    VirtualMachine machine,
    ExecutionEnv environment,
    ForkedExecutionControl control
  ) {
    Util.detectJdiExitEvent(machine, event -> {
      environment.closeDown();
      control.disposeMachine();
    });
  }
}