package jsheets.runtime.evaluation.shell.environment;

import jdk.jshell.execution.JdiExecutionControlProvider;
import jdk.jshell.spi.ExecutionControlProvider;

public final class RemoteExecution implements ExecutionEnvironment {
	@Override
	public ExecutionControlProvider control() {
		return new JdiExecutionControlProvider();
	}
}
