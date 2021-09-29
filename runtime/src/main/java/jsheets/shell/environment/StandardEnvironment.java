package jsheets.shell.environment;

import java.net.InetAddress;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

public final class StandardEnvironment implements ExecutionEnvironment {
  public static ExecutionEnvironment create() {
    return new StandardEnvironment();
  }

  @Override
  public ExecutionControlProvider control(String name) {
    return new Provider();
  }

  @Override
  public Installation install() {
    return () -> {};
  }

  static final class Provider implements ExecutionControlProvider {
    private Provider() {}

    @Override
    public String name() {
      return "jsheets-provider";
    }

    @Override
    public ExecutionControl generate(
      ExecutionEnv environment,
      Map<String, String> parameters
    ) throws Throwable {
      var address = InetAddress.getLoopbackAddress().getHostAddress();
      var spec = createControlSpec(address);
      return ExecutionControl.generate(environment, spec);
    }

    private static String createControlSpec(String address) {
      return String.format(
        "failover:0(jdi:hostname(%s)), 1(jdi:launch(true)), 2(jdi)",
        address
      );
    }
  }
}
