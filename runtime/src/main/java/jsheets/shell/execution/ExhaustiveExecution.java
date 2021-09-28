package jsheets.shell.execution;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;

import javax.annotation.Nullable;
import jdk.jshell.DeclarationSnippet;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

/**
 * Executes all statements and binds all declarations in a source,
 * to support real scripting experience in JShell.
 * <p>
 * The {@link JShell#eval(String)} does not accept sources that contain
 * multiple declarations or statements (apart from blocks). This heavily limits
 * the way we can use it directly, as input is allowed to contain more than just
 * "one distinct thing". This class solves the problem by ensuring that every
 * part of a given source code is evaluated by the shell.
 *
 * @implNote {@link Snippet Snippets} capture the range of code that is actually
 *  included in their declaration/evaluation (based on their
 *  {@link Snippet#kind()} kind). In order to ensure that a given source code
 *  is executed exhaustively, the source must be evaluated and reduced the
 *  part that is not captured by the returned snippet. This process is repeated
 *  until the (not yet executed part of the) source is empty.
 */
public final class ExhaustiveExecution implements ExecutionMethod {
  // TODO: This mechanism has problems with declarations
  //  it is still useful for simple statement blocks (that contain variables).

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public record Config(int statementLimit) {
    public static Config createDefault() {
      return new Config(100);
    }
  }

  public static boolean isSupported() {
    return gutsAccessor != null & wrapType != null;
  }

  public static ExhaustiveExecution create(JShell shell) {
    return create(shell, Config.createDefault());
  }

  public static ExhaustiveExecution create(JShell shell, Config config) {
    Objects.requireNonNull(shell, "shell");
    Objects.requireNonNull(config, "config");
    ensureSupported();
    return new ExhaustiveExecution(shell::eval, config);
  }

  private static void ensureSupported() {
    if (!isSupported()) {
      throw new IllegalStateException("failed to initialize component: " + resolveError);
    }
  }

  private final Config config;
  private final ExecutionMethod delegate;

  private ExhaustiveExecution(ExecutionMethod delegate, Config config) {
    this.config = config;
    this.delegate = delegate;
  }

  @Override
  public Collection<SnippetEvent> execute(String source) {
    return new Evaluation(preprocess(source)).run();
  }

  private String preprocess(String source) {
    return source.trim();
  }

  final class Evaluation {
    private final String originalSource;
    private String reducedSource;
    private final Collection<SnippetEvent> reportedEvents = new ArrayList<>();
    private int iteration;
    private boolean stopped;

    private Evaluation(String originalSource) {
      this.originalSource = originalSource;
      this.reducedSource = originalSource;
    }

    private Collection<SnippetEvent> run() {
      while (shouldContinue()) {
        iteration++;
        process();
      }
      return reportedEvents;
    }

    private boolean shouldContinue() {
      return !stopped
        && !Strings.isNullOrEmpty(reducedSource)
        && iteration < config.statementLimit();
    }

    private void stop() {
      stopped = true;
    }

    private void process() {
      var events = delegate.execute(reducedSource);
      if (events.isEmpty()) {
        stop();
      } else {
        processEvents(events);
      }
    }

    private void processEvents(Collection<SnippetEvent> events) {
      for (var event : events) {
        if (event.status().equals(Snippet.Status.REJECTED)) {
          processRejectedSnippet(event);
        } else {
          processValidSnippet(event);
        }
      }
    }

    private void processRejectedSnippet(SnippetEvent event) {
      var range = rangeInText(event.snippet());
      if (range != null) {
        removeRangeFromSource(range);
        if (event.snippet() instanceof DeclarationSnippet) {
          reportedEvents.addAll(retryRange(originalSource, range));
          return;
        }
      }
      reportedEvents.add(event);
    }

    /*
     * Retries evaluating a rejected snippet but only for the range that
     * has been resolved in the previous compilation. Certain declarations need
     * to be evaluated in isolation.
     */
    private Collection<SnippetEvent> retryRange(String source, Range region) {
      var failedSource = source.substring(region.start(), region.end());
      return delegate.execute(failedSource);
    }

    private void processValidSnippet(SnippetEvent event) {
      reportedEvents.add(event);
      var snippet = event.snippet();
      var range = rangeInText(snippet);
      if (range == null || range.end() >= originalSource.length()) {
        stop();
        return;
      }
      removeRangeFromSource(range);
    }

    private void removeRangeFromSource(Range range) {
      reducedSource = originalSource.substring(range.end());
    }
  }

  private record Range(int start, int end) {}

  @Nullable
  private Range rangeInText(Snippet snippet) {
    try {
      var wrap = gutsAccessor.invoke(snippet);
      return wrap == null ? null : rangeInWrap(wrap);
    } catch (Throwable failedAccess) {
      throw new RuntimeException(
        "failed to access wrap in: " + snippet,
        failedAccess
      );
    }
  }

  private Range rangeInWrap(Object wrap) {
    try {
      return new Range(
        (int) wrapType.firstIndex.invoke(wrap),
        (int) wrapType.lastIndex.invoke(wrap)
      );
    } catch (Throwable failure) {
      throw new RuntimeException(
        "failed to read text range of wrap: " + wrap,
        failure
      );
    }
  }

  @Override
  public String toString() {
    return "ExhaustiveExecution(delegate=%s, config=%s)".formatted(delegate, config);
  }

  private record WrapType(MethodHandle firstIndex, MethodHandle lastIndex) {}

  /* We depend on the smallest possible interface for invocation */
  private static final String generalWrapClass = "jdk.jshell.GeneralWrap";

  private static WrapType resolveWrapType() {
    return ResolveTask.run(() -> {
      var wrapType = Class.forName(generalWrapClass);
      var lookup = MethodHandles.privateLookupIn(wrapType, MethodHandles.lookup());
      var methodType = MethodType.methodType(int.class);
      return new WrapType(
        lookup.findVirtual(wrapType, "firstSnippetIndex", methodType),
        lookup.findVirtual(wrapType, "lastSnippetIndex", methodType)
      );
    });
  }

  /* This class has tob e used to support the Snippet.guts() signature */
  private static final String wrapClass = "jdk.jshell.Wrap";

  private static MethodHandle resolveGutsAccessor() {
    return ResolveTask.run(() -> {
      var wrapType = Class.forName(wrapClass);
      var lookup = MethodHandles.privateLookupIn(Snippet.class, MethodHandles.lookup());
      return lookup.findVirtual(Snippet.class, "guts", MethodType.methodType(wrapType));
    });
  }

  private static WrapType wrapType;
  private static MethodHandle gutsAccessor;

  /* Possible error message from resolveWrapType() and resolveWrapAccessor() */
  private static String resolveError;

  @FunctionalInterface
  interface ResolveTask<T> {
    T resolve() throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException;

    static <T> T run(ResolveTask<T> task) {
      try {
        return task.resolve();
      } catch (IllegalAccessException missingAccess) {
        throw new RuntimeException("could not open " + wrapClass, missingAccess);
      } catch (ClassNotFoundException | NoSuchMethodException incompatible) {
        throw new RuntimeException("incompatible JShell version", incompatible);
      }
    }
  }

  static {
    try {
      wrapType = resolveWrapType();
      gutsAccessor = resolveGutsAccessor();
    } catch (Throwable failure) {
      resolveError = failure.getMessage();
      log.atWarning().withCause(failure).log("failed to initialize");
    }
  }
}