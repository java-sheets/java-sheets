package jsheets.output;

import com.google.errorprone.annotations.Var;

import javax.annotation.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Works with {@link ThreadGroup ThreadGroups} to determine which tenant is
 * printing a message or error.
 * <p>
 * The {@code fallback} consumer can be a method reference to the print method
 * of the actual {@code stdout} of the host process, thus preventing logs from
 * the actual application to be ignored.
 * <p>
 * Each {@link ThreadGroup} has an associated buffer that is used to store
 * a message before it is sent to the listener.
 */
public final class TenantBasedOutput extends ListeningPrintStream {
  public static final class Installation implements AutoCloseable {
    private final TenantBasedOutput output;
    private final PrintStream overwritten;
    private final Consumer<PrintStream> mutator;

    private Installation(
      TenantBasedOutput output,
      PrintStream overwritten,
      Consumer<PrintStream> mutator
    ) {
      this.output = output;
      this.overwritten = overwritten;
      this.mutator = mutator;
    }

    public TenantBasedOutput output() {
      return output;
    }

    public void uninstall() {
      mutator.accept(overwritten);
    }

    @Override
    public void close() {
      uninstall();
    }
  }

  /**
   * Creates an instance and installs it as this processes {@link System#out output} stream.
   * <p>
   * The returned installation can be used to obtain the output and unapply it.
   */
  public static Installation installAsStandard() {
    var previous = System.out;
    var output = new TenantBasedOutput(previous::print);
    System.setOut(output);
    return new Installation(output, previous, System::setOut);
  }

  /**
   * Creates an instance and installs it as this processes {@link System#err error} stream.
   * <p>
   * The returned installation can be used to obtain the output and unapply it.
   */
  public static Installation installAsError() {
    var previous = System.err;
    var output = new TenantBasedOutput(previous::print);
    System.setErr(output);
    return new Installation(output, previous, System::setErr);
  }

  public static Optional<TenantBasedOutput> currentOutput() {
    return cast(System.out);
  }

  public static Optional<TenantBasedOutput> currentError() {
    return cast(System.err);
  }

  private static Optional<TenantBasedOutput> cast(PrintStream stream) {
    return stream instanceof TenantBasedOutput
      ? Optional.of((TenantBasedOutput) stream)
      : Optional.empty();
  }

  public static TenantBasedOutput create() {
    return new TenantBasedOutput(message -> {});
  }

  public static TenantBasedOutput createWithFallback(Consumer<String> fallback) {
    Objects.requireNonNull(fallback, "fallback");
    return new TenantBasedOutput(fallback);
  }

  private final Consumer<String> fallback;
  private final Map<String, Consumer<String>> groupListeners = new ConcurrentHashMap<>();
  private final Map<String, StringBuilder> buffers = new HashMap<>();

  private TenantBasedOutput(Consumer<String> fallback) {
    super(new PrintStream(OutputStream.nullOutputStream()));
    this.fallback = fallback;
  }

  public void removeGroup(String name) {
    groupListeners.remove(name);
    synchronized (this) {
      buffers.remove(name);
    }
  }

  public void registerGroup(String name, Consumer<String> listener) {
    groupListeners.put(name, listener);
  }

  @Override
  public synchronized boolean capture(String text) {
    var group = currentGroup();
    var buffer = removeBuffer(group);
    var message = buffer == null ? text : buffer.append(text).toString();
    write(group, message);
    return true;
  }

  @Nullable
  private StringBuilder removeBuffer(@Var @Nullable ThreadGroup group) {
    while (group != null) {
      var buffer = buffers.remove(group.getName());
      if (buffer != null) {
        return buffer;
      }
      group = group.getParent();
    }
    return null;
  }

  @Override
  public synchronized void flush() {
    flushToGroup(currentGroup());
  }

  private void flushToGroup(@Nullable ThreadGroup group) {
    if (group == null) {
      flushToFallback();
      return;
    }
    var buffer = buffers.remove(group.getName());
    if (buffer == null) {
      flushToGroup(group.getParent());
    } else {
      write(group, buffer.toString());
    }
  }

  private static final String fallbackBufferKey = null;

  private void flushToFallback() {
    var buffer = buffers.remove(fallbackBufferKey);
    if (buffer != null) {
      fallback.accept(buffer.toString());
    }
  }

  private synchronized void write(@Nullable ThreadGroup group, String content) {
    if (group == null) {
      fallback.accept(content);
      return;
    }
    var listener = groupListeners.get(group.getName());
    if (listener == null) {
      write(group.getParent(), content);
    } else {
      listener.accept(content);
    }
  }

  private ThreadGroup currentGroup() {
    return Thread.currentThread().getThreadGroup();
  }
}