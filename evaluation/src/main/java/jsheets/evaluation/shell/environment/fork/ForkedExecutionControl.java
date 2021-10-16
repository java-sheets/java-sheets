package jsheets.evaluation.shell.environment.fork;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.flogger.FluentLogger;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import jdk.jshell.execution.JdiExecutionControl;
import jsheets.evaluation.shell.environment.ClassFileStore;

/*
 * This class is based on the JdiDefaultExecutionControl and is highly
 * coupled to the remote agent implementation.
 */
public final class ForkedExecutionControl extends JdiExecutionControl {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private VirtualMachine machine;
  private Process process;

  private final Lock stopLock = new ReentrantLock();
  private boolean userCodeRunning = false;
  private volatile boolean closed;

  private final String remoteAgentClass;
  private final ClassFileStore classFileStore;

  ForkedExecutionControl(
    ObjectOutput output,
    ObjectInput input,
    VirtualMachine machine,
    Process process,
    String remoteAgentClass,
    ClassFileStore classFileStore
  ) {
    super(output, input);
    this.machine = machine;
    this.process = process;
    this.remoteAgentClass = remoteAgentClass;
    this.classFileStore = classFileStore;
  }

  @Override
  protected synchronized VirtualMachine vm() throws EngineTerminationException {
    if (machine == null) {
      throw new EngineTerminationException("virtual machine is closed");
    }
    return machine;
  }

  @Override
  public void load(ClassBytecodes[] bytecodes)
    throws ClassInstallException, NotImplementedException, EngineTerminationException
  {
    classFileStore.load(bytecodes);
    super.load(bytecodes);
  }

  @Override
  public void redefine(ClassBytecodes[] bytecodes)
    throws ClassInstallException, EngineTerminationException
  {
    classFileStore.redefine(bytecodes);
    super.redefine(bytecodes);
  }

  @Override
  public String invoke(String classname, String methodName)
    throws RunException, EngineTerminationException, InternalException
  {
    String result;
    updateUserCodeRunning(true);
    try {
      result = super.invoke(classname, methodName);
    } finally {
      updateUserCodeRunning(false);
    }
    return result;
  }

  private void updateUserCodeRunning(boolean target) {
    stopLock.lock();
    try {
      userCodeRunning = target;
    } finally {
      stopLock.unlock();
    }
  }

  @Override
  public void stop() throws EngineTerminationException, InternalException {
    stopLock.lock();
    try {
      if (userCodeRunning) {
        new RemoteInterrupt(vm(), remoteAgentClass).runInSuspendedMode();
        closed = true;
      }
    } finally {
      stopLock.unlock();
    }
  }


  @Override
  public void close() {
    super.close();
    disposeMachine();
    stopLock.lock();
    closed = true;
  }

  public boolean isClosed() {
    return closed;
  }

  synchronized void disposeMachine() {
    try {
      if (machine != null) {
        machine.dispose();
        machine = null;
      }
    } catch (VMDisconnectedException alreadyClosed) {
      log.atFiner().withCause(alreadyClosed).log("remote is already closed");
    } catch (Throwable failure) {
      log.atFine().withCause(failure).log("failed to dispose remote");
    } finally {
      if (process != null) {
        process.destroy();
        process = null;
      }
    }
  }
}