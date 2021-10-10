package jsheets.sandbox;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static jsheets.sandbox.AccessGraph.Key.*;

public class AccessGraphTest {

  @Test
  public void testReadingFromFile() {
    var specification = readFileContent("access-graph.txt");
    var graph = AccessGraph.of(specification.split("\n"));
    Assertions.assertTrue(graph.isPermitted(dotSeparated("a.b.c")));
    Assertions.assertTrue(graph.isPermitted(dotSeparated("a.b.c.Foo")));
    Assertions.assertTrue(graph.isPermitted(dotSeparated("a.b.c.Foo#run")));
    Assertions.assertTrue(graph.isPermitted(dotSeparated("a.b.c.Unknown")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Foo#exit")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Bar")));
    Assertions.assertFalse(graph.isPermitted(dotSeparated("a.b.c.Bar#run")));
  }

  @Test
  public void testOverloadedMethod() {
    var graph = AccessGraph.of(
      "void a.b.c.Foo#run(String[], int)"
    );
    Assertions.assertTrue(graph.isMethodPermitted(MethodSignature.parse("void a.b.c.Foo#run(String[], int)")));
    Assertions.assertFalse(graph.isMethodPermitted(MethodSignature.parse("void a.b.c.Foo#run")));
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
}
