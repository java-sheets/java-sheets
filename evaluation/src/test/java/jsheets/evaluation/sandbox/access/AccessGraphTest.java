package jsheets.evaluation.sandbox.access;

import com.google.common.base.Charsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static jsheets.evaluation.sandbox.access.AccessKey.*;

public class AccessGraphTest {
  @Test
  public void testReadingFromFile() {
    var specification = readFileContent("accessGraph.txt");
    var graph = AccessGraph.of(specification.split("\n"));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c")));
    Assertions.assertTrue(graph.isPermitted(dotSeparated("a.b.c.Foo")));
    Assertions.assertTrue(graph.isPermitted(dotSeparated("a.b.c.Foo#run")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Unknown")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Foo#exit")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Bar")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Bar#run")));
  }

  @Test
  public void testOverloadedMethod() {
    var graph = AccessGraph.of(
      "a.b.c.Foo#run(String[], int):void"
    );
    Assertions.assertTrue(graph.isMethodPermitted(MethodSignature.parse("a.b.c.Foo#run(String[], int):void")));
    Assertions.assertFalse(graph.isMethodPermitted(MethodSignature.parse("a.b.c.Foo#run():void")));
  }

  @Test
  public void testKeySplit() {
    Assertions.assertEquals(
      List.of("a", "b", "c"),
      List.of(dotSeparated("a.b.c").split())
    );
    Assertions.assertEquals(
      List.of("a", "b", "c"),
      List.of(slashSeparated("a/b/c").split())
    );
    Assertions.assertEquals(
      List.of("a", "b", "c", "Foo"),
      List.of(slashSeparated("a/b/c#Foo").split())
    );
  }

  private String readFileContent(String path) {
    var resource = Thread.currentThread()
      .getContextClassLoader()
      .getResourceAsStream(path);
    if (resource == null) {
      throw new RuntimeException(new FileNotFoundException());
    }
    try (resource) {
      return new String(resource.readAllBytes(), Charsets.UTF_8);
    } catch (IOException failure) {
      throw new RuntimeException(failure);
    }
  }

  @Test
  public void testOf() {
    var graph = AccessGraph.of(
      "java.lang",
      "java.util.List",
      "java.lang.Thread#sleep",
      "java.lang.System",
      "!java.lang.System#exit"
    );
    Assertions.assertFalse(graph.isPermitted(slashSeparated("java/io")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/util/List")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang/System/out")));
    Assertions.assertFalse(
      graph.isMethodPermitted(MethodSignature.parse("java/lang/System#exit"))
    );
    Assertions.assertTrue(
      graph.isMethodPermitted(MethodSignature.parse("java/lang/Thread#sleep()"))
    );
  }
}
