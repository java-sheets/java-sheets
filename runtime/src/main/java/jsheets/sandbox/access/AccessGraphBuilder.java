package jsheets.sandbox.access;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class AccessGraphBuilder {
  private final AccessGraphNode root = AccessGraphNode.Path.create(
    "root",
    Access.NotSet,
    new ArrayList<>()
  );

  AccessGraphBuilder() {}

  @CanIgnoreReturnValue
  public AccessGraphBuilder permit(AccessKey key) {
    var node = createNode(key, Access.Permitted);
    insert(key, node);
    return this;
  }

  @CanIgnoreReturnValue
  public AccessGraphBuilder deny(AccessKey key) {
    var node = createNode(key, Access.Denied);
    insert(key, node);
    return this;
  }

  private void insert(AccessKey key, AccessGraphNode node) {
    var parent = resolveParent(key, node);
    parent.insertChild(node);
  }

  private AccessGraphNode resolveParent(
    AccessKey key,
    AccessGraphNode inserted
  ) {
    var path = key.split();
    if (path.length == 1) {
      return root;
    }
    var parentPath = Arrays.copyOf(path, path.length - 1);
    return resolveParent(root, parentPath, 0, inserted);
  }

  private AccessGraphNode resolveParent(
    AccessGraphNode target,
    String[] key,
    int depth,
    AccessGraphNode inserted
  ) {
    if (depth >= key.length) {
      return target;
    }
    for (var child : target) {
      if (child.matchesKey(key[depth])) {
        return resolveParent(child, key, depth + 1, inserted);
      }
    }
    return insertRemainingPath(target, key, depth);
  }

  private AccessGraphNode insertRemainingPath(AccessGraphNode target, String[] key, int depth) {
    for (int index = depth; index < key.length; index++) {
      var intermediate = AccessGraphNode.Path.create(key[index], Access.NotSet, new ArrayList<>());
      target.insertChild(intermediate);
      target = intermediate;
    }
    return target;
  }

  private AccessGraphNode createNode(AccessKey key, Access access) {
    if (isMethodSignature(key.value())) {
      var signature = MethodSignature.parse(key.value());
      return AccessGraphNode.Method.create(signature, access, new ArrayList<>());
    }
    return AccessGraphNode.Path.create(key.lastPart(), access, new ArrayList<>());
  }

  private static boolean isMethodSignature(String path) {
    return path.indexOf(MethodSignature.methodSeparator()) >= 0;
  }

  // TODO: Create defensive copies after building
  public AccessGraph create() {
    return new AccessGraph(root);
  }
}