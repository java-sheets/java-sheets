package jsheets.evaluation.shell.environment.fork;

import java.util.Objects;
import java.util.Set;

import com.google.common.flogger.FluentLogger;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import jdk.jshell.spi.ExecutionControl;

final class RemoteInterrupt {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final VirtualMachine virtualMachine;
  private final String remoteAgentClass;

  RemoteInterrupt(VirtualMachine virtualMachine, String remoteAgentClass) {
    this.virtualMachine = virtualMachine;
    this.remoteAgentClass = remoteAgentClass;
  }

  public void runInSuspendedMode() throws ExecutionControl.InternalException {
    virtualMachine.suspend();
    try {
      run();
    } catch (Exception failure) {
      throw new ExecutionControl.InternalException(
        "failed to stop remote execution: %s".formatted(failure)
      );
    } finally {
      virtualMachine.resume();
    }
  }

  public void run() throws Exception {
    for (var thread : virtualMachine.allThreads()) {
      if (shouldIgnoreThread(thread)) {
        continue;
      }
      for (var frame : thread.frames()) {
        if (isRemoteAgentFrame(frame)) {
          var instance = frame.thisObject();
          Objects.requireNonNull(instance, "frame instance is null");
          closeRemoteAgentThread(thread, instance);
        }
      }
    }
  }

  private static final Set<String> agentMethods = Set.of("invoke", "varValue");

  private boolean isRemoteAgentFrame(StackFrame frame) {
    var location = frame.location();
    if (!remoteAgentClass.equals(location.declaringType().name())) {
      return false;
    }
    var methodName = location.method().name();
    return agentMethods.contains(methodName);
  }

  /* Threads created by user code can and should be ignored */
  private boolean shouldIgnoreThread(ThreadReference thread) {
    log.atInfo().log("not ignoring thread " + thread);
    return false;
  }

  /*
   * Closes the remote agent thread by manipulating the fields that are
   * controlling its execution state and throwing an asynchronous exception.
   * This code is highly coupled with the agent implementation running on the
   * remote end.
   */
  private void closeRemoteAgentThread(
    ThreadReference thread,
    ObjectReference frame
  ) throws Exception {
    var inClientCodeField = frame.referenceType().fieldByName("inClientCode");
    var expectingStopField = frame.referenceType().fieldByName("expectingStop");
    var stopExceptionField = frame.referenceType().fieldByName("stopException");
    var inClientCode = (BooleanValue) frame.getValue(inClientCodeField);
    if (inClientCode.value()) {
      frame.setValue(expectingStopField, virtualMachine.mirrorOf(true));
      var stopInstance = (ObjectReference) frame.getValue(stopExceptionField);
      virtualMachine.resume();
      log.atFine().log("attempting to stop the client code execution");
      thread.stop(stopInstance);
      frame.setValue(expectingStopField, virtualMachine.mirrorOf(false));
    }
  }
}