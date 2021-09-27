package jsheets.shell.execution;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.google.common.flogger.FluentLogger;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

/**
 * Executes all statements and binds all declaration in a source,
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
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public record Config(int statementLimit) {
    public static Config createDefault() {
      return new Config(100);
    }
  }

  public static boolean isSupported() {
    return wrapAccessor != null & wrapType != null;
  }

  public static ExhaustiveExecution create(JShell shell) {
    return create(shell, Config.createDefault());
  }

  public static ExhaustiveExecution create(JShell shell, Config config) {
    Objects.requireNonNull(shell, "shell");
    Objects.requireNonNull(config, "config");
    ensureSupported();
    return new ExhaustiveExecution(shell, config);
  }

  private static void ensureSupported() {
    if (!isSupported()) {
      throw new IllegalStateException("failed to initialize component: " + resolveError);
    }
  }

  private final Config config;
  private final JShell shell;

  private ExhaustiveExecution(JShell shell, Config config) {
    this.config = config;
    this.shell = shell;
  }

  @Override
  public Collection<SnippetEvent> execute(String source) {
    return executeForPreprocessed(preprocess(source));
  }

  private String preprocess(String source) {
    return source.trim();
  }

  private Collection<SnippetEvent> executeForPreprocessed(String source) {
    var reducedSource = source;
    var allEvents = new ArrayList<SnippetEvent>();
    for (int index = 0; index < config.statementLimit() && !reducedSource.isEmpty(); index++) {
      var events = shell.eval(reducedSource);
      if (events.isEmpty()) {
        break;
      }
      for (var event : events) {
        allEvents.add(event);
        var snippet = event.snippet();
        var range = rangeInText(snippet);
        if (range.end() >= reducedSource.length()) {
          break;
        }
        reducedSource = source.substring(range.end());
      }
    }
    return allEvents;
  }

  private record Range(int start, int end) {}

  private Range rangeInText(Snippet snippet) {
    try {
      var wrap = wrapAccessor.invoke(snippet);
      return rangeInWrap(wrap);
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
    return "ExhaustiveExecution(shell=%s, config=%s)".formatted(shell, config);
  }

  private record WrapType(MethodHandle firstIndex, MethodHandle lastIndex) {}

  /* We depend on the smallest possible interface for invocation */
  private static final String generalWrapClass = "jdk.jshell.GeneralWrap";

  private static WrapType resolveWrapType() {
    try {
      var wrapType = Class.forName(generalWrapClass);
      var lookup = MethodHandles.privateLookupIn(wrapType, MethodHandles.lookup());
      var methodType = MethodType.methodType(int.class);
      return new WrapType(
        lookup.findVirtual(wrapType, "firstSnippetIndex", methodType),
        lookup.findVirtual(wrapType, "lastSnippetIndex", methodType)
      );
    } catch (IllegalAccessException missingAccess) {
      throw new RuntimeException("could not open " + generalWrapClass, missingAccess);
    } catch (ClassNotFoundException | NoSuchMethodException incompatible) {
      throw new RuntimeException("incompatible JShell version", incompatible);
    }
  }

  /* This class has tob e used to support the Snippet.guts() signature */
  private static final String wrapClass = "jdk.jshell.Wrap";

  private static MethodHandle resolveWrapAccessor() {
    try {
      var wrapType = Class.forName(wrapClass);
      var lookup = MethodHandles.privateLookupIn(Snippet.class, MethodHandles.lookup());
      return lookup.findVirtual(Snippet.class, "guts", MethodType.methodType(wrapType));
    } catch (IllegalAccessException missingAccess) {
      throw new RuntimeException("could not open " + wrapClass, missingAccess);
    } catch (ClassNotFoundException | NoSuchMethodException incompatible) {
      throw new RuntimeException("incompatible JShell version", incompatible);
    }
  }

  private static WrapType wrapType;
  private static MethodHandle wrapAccessor;

  /* Possible error message from resolveWrapType() and resolveWrapAccessor() */
  private static String resolveError;

  static {
    try {
      wrapType = resolveWrapType();
      wrapAccessor = resolveWrapAccessor();
    } catch (Throwable failure) {
      resolveError = failure.getMessage();
      log.atWarning().withCause(failure).log("failed to initialize");
    }
  }
}