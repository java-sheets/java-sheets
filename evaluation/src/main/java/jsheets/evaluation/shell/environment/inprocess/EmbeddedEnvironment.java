package jsheets.evaluation.shell.environment.inprocess;

import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jsheets.output.TenantBasedOutput;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;

/**
 * Executing multiple shells in a shared process can have many negative
 * side effects because the java library is generally not designed in a
 * way to support multi tenancy.
 */
public final class EmbeddedEnvironment implements ExecutionEnvironment {
  public static EmbeddedEnvironment create() {
    return new EmbeddedEnvironment(MultiTenancy.create());
  }

  private final Tenancy tenancy;

  private EmbeddedEnvironment(Tenancy tenancy) {
    this.tenancy = tenancy;
  }

  @Override
  public ExecutionControlProvider control(String name) {
    return new Provider(name, tenancy);
  }

  @Override
  public Installation install() {
    var standardInstallation = TenantBasedOutput.installAsStandard();
    var errorInstallation = TenantBasedOutput.installAsError();
    return () -> {
      standardInstallation.close();
      errorInstallation.close();
    };
  }

  static final class Provider implements ExecutionControlProvider {
    private final String name;
    private final Tenancy tenancy;

    private Provider(String name, Tenancy tenancy) {
      this.name = name;
      this.tenancy = tenancy;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public ExecutionControl generate(
      ExecutionEnv environment,
      Map<String, String> parameters
    ) {
      return new InProcessExecutionControl(
        environment,
        tenancy,
        "schell-executor-" + name
      );
    }
  }
}
