package jsheets.runtime.evaluation.shell;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;

public final class ExhaustiveEvaluation {
  private record WrapType(Method firstIndex, Method lastIndex) {}

  private static final String internalWrapTypeName = "jdk.jshell.GeneralWrap";

  private static WrapType resolveWrapType() {
    try {
      var wrapType = Class.forName(internalWrapTypeName);
      var firstIndex = wrapType.getDeclaredMethod("firstSnippetIndex");
      firstIndex.setAccessible(true);
      var secondIndex = wrapType.getDeclaredMethod("lastSnippetIndex");
      secondIndex.setAccessible(true);
      return new WrapType(firstIndex, secondIndex);
    } catch (InaccessibleObjectException missingAccess) {
      throw new RuntimeException("could not open " + internalWrapTypeName);
    } catch (ClassNotFoundException | NoSuchMethodException incompatible) {
      throw new RuntimeException("incompatible jshell version", incompatible);
    }
  }

  private final int statementLimit;
  private final WrapType wrapType;
  private final Field wrapField;

  private ExhaustiveEvaluation() {}

  public static void main(String[] options) {
    var shell = jdk.jshell.JShell.builder()
      .err(System.err)
      .out(System.out)
      .build();
    var originalSource = """
      int x = 10;
      System.out.println(x + "a");
      x = 20;
      System.out.println(x + "b");
    """.trim();
    var source = originalSource;
    var snippets = new java.util.ArrayList<jdk.jshell.Snippet>();

    for (int index = 0; index < 100 && !source.isEmpty(); index++) {
      var snippet = shell.eval(source).get(0).snippet();
      var range = rangeInText(snippet);
      snippets.add(snippet);
      if (range.end() >= source.length()) {
        break;
      }
      source = originalSource.substring(range.end());
    }
    System.out.println(snippets);
  }

  record Range(int start, int end) {}

  private static Range rangeInText(jdk.jshell.Snippet snippet) {
    try {
      var accessor = jdk.jshell.Snippet.class.getDeclaredMethod("guts");
      accessor.setAccessible(true);
      var wrap = accessor.invoke(snippet);
      return rangeInWrap(wrap);
    } catch (Exception failedAccess) {
      throw new RuntimeException(failedAccess);
    }
  }

  static Class<?> wrapType;


  static Range rangeInWrap(Object wrap) {
    try {
        (int) firstIndex.invoke(wrap),
        (int) secondIndex.invoke(wrap)
      );
    } catch (Exception failedAccess) {
      throw new RuntimeException(failedAccess);
    }
  }
}

