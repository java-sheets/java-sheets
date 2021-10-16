package jsheets.evaluation.shell.environment.inprocess;

/**
 * Thrown when the execution is preempted using {@link InProcessExecutionControl#stop()}.
 */
public final class Preemption extends RuntimeException {
  Preemption() {}
}