package jsheets.sandbox;

import org.junit.jupiter.api.Assertions;

import static jsheets.sandbox.AccessGraph.Key.slashSeparated;

public class PermissionGraphTest {
  public void testDirectPath() {
    var graph = AccessGraph.of(
      "java.lang",
      "java.util.List",
      "void java.lang.Thread#sleep()"
    );
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang")));
  }
}
