package jsheets.evaluation.sandbox.access;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AccessGraph {
  private static final AccessGraph empty = new AccessGraph(
    AccessGraphNode.Path.create("root", Access.Denied, List.of()));

  public static AccessGraph empty() {
    return empty;
  }

  public static AccessGraph of(String... permissions) {
    var builder = newBuilder();
    for (var permission : permissions) {
      if (permission.startsWith("!")) {
        var remaining = permission.substring(1).trim();
        builder.deny(AccessKey.infer(remaining));
      } else {
        builder.permit(AccessKey.infer(permission.trim()));
      }
    }
    return builder.create();
  }

  public static AccessGraphBuilder newBuilder() {
    return new AccessGraphBuilder();
  }

  private final AccessGraphNode root;

  AccessGraph(AccessGraphNode root) {
    this.root = root;
  }

  private Collection<AccessGraphNode> findClosestMatch(AccessKey key) {
    return findClosestMatch(root, key.split(), 0);
  }

  private static Collection<AccessGraphNode> findClosestMatch(
    AccessGraphNode node,
    String[] key,
    int depth
  ) {
    if (depth >= key.length) {
      return List.of(node);
    }
    if (depth == key.length - 1) {
      return listClosestChildrenOrParent(node, key[key.length - 1]);
    }
    return findClosestChild(node, key, depth);
  }

  private static Collection<AccessGraphNode> findClosestChild(
    AccessGraphNode node,
    String[] key,
    int depth
  ) {
    for (var child : node) {
      if (child.matchesKey(key[depth])) {
        return findClosestMatch(child, key, depth + 1);
      }
    }
    return List.of(node);
  }

  private static Collection<AccessGraphNode> listClosestChildrenOrParent(
    AccessGraphNode node,
    String lastKey
  ) {
    var children = node.children()
      .filter(child -> child.matchesKey(lastKey))
      .toList();
    return children.isEmpty() ? List.of(node) : children;
  }

  /**
   * Checks if the use of the class or method under the given key is permitted.
   * <p>
   * When checking access to methods prefer
   * {@link AccessGraph#isMethodPermitted(MethodSignature)}.
   *
   * @param key Key of the checked package, class or method.
   * @return True if the caller has permission to use classes or methods
   *   under this key.
   */
  public boolean isPermitted(AccessKey key) {
    var matches = findClosestMatch(key);
    if (matches.isEmpty()) {
      return false;
    }
    var first = matches.iterator().next();
    return first.access().equals(Access.Permitted);
  }

  /**
   * Checks if a call to the method is permitted.
   * <p>
   * This call is better for checking access to methods because it supports
   * overloaded methods. The {@link AccessGraph#isPermitted} method does
   * not handle ambiguous calls well.
   * <p>
   * Note, that this method does not support covariant return types.
   *
   * @param signature Signature of the method that is checked.
   * @return True if the caller has permissions to call this method.
   */
  public boolean isMethodPermitted(MethodSignature signature) {
    var key = AccessKey.infer(signature.formatWithoutTypes());
    var matches = findClosestMatch(key);
    var access = switch (matches.size()) {
      case 0 -> Access.Denied;
      case 1 -> {
        var result = matches.iterator().next();
        boolean isMatch = !result.isClassMember() || result.matchesMethod(signature);
        yield isMatch ? result.access() : Access.NotSet;
      }
      default -> findBestMethodMatch(signature, matches).map(AccessGraphNode::access);
    };
    return access.equals(Access.Permitted);
  }

  private Optional<AccessGraphNode> findBestMethodMatch(
    MethodSignature signature,
    Collection<AccessGraphNode> nodes
  ) {
    for (var node : nodes) {
      if (node.matchesMethod(signature)) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "AccessGraph(%s)".formatted(root);
  }

  @Override
  public boolean equals(Object target) {
    if (this == target) {
      return true;
    }
    return target instanceof AccessGraph graph && (
      Objects.equals(graph.root, root)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(root);
  }
}