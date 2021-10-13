package jsheets.sandbox;

import jsheets.sandbox.access.AccessGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static jsheets.sandbox.access.AccessKey.slashSeparated;

public class PermissionGraphTest {
  @Test
  public void testDirectPath() {
    var graph = AccessGraph.of(
      "java.lang",
      "java.util.List",
      "void java.lang.Thread#sleep()"
    );
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/io")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/util/List")));
    Assertions.assertTrue(graph.isPermitted(slashSeparated("java/lang/Thread#sleep()")));
  }
}
