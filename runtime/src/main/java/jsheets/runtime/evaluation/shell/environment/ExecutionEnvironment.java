package jsheets.runtime.evaluation.shell.environment;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;

public interface ExecutionEnvironment {
	ExecutionControlProvider control();
}
