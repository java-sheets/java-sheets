package jsheets.sandbox;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.*;
import java.util.regex.Pattern;

public final class AccessGraph {
  private static final AccessGraph empty =
    new AccessGraph(new Node.Path("root", Access.Denied, List.of()));

  public static AccessGraph empty() {
    return empty;
  }

  public static AccessGraph of(String... permissions) {
    var builder = newBuilder();
    for (var permission : permissions) {
      if (permission.startsWith("!")) {
        var remaining = permission.substring(1).trim();
        builder.deny(Key.infer(remaining));
      } else {
        builder.permit(Key.infer(permission.trim()));
      }
    }
    return builder.create();
  }

  public enum Access {
    Permitted, Denied
  }

  /* sealed */ interface Node extends Iterable<Node> {
    Access access();
    String name();
    Collection<Node> children();
    void insertChild(Node node);

    default Iterator<Node> iterator() {
      return children().iterator();
    }

    record Path(String name, Access access, Collection<Node> children) implements Node {
      public Collection<Method> listMethodsByName(String name) {
        var methods = new ArrayList<Method>();
        for (var child : children) {
          if (child instanceof Method method && method.name().equals(name)) {
            methods.add(method);
          }
        }
        return methods;
      }

      @Override
      public void insertChild(Node node) {
        children.add(node);
      }
    }

    record Method(MethodSignature signature, Access access) implements Node {
      @Override
      public String name() {
        return signature.methodName();
      }

      @Override
      public Collection<Node> children() {
        return List.of();
      }

      @Override
      public void insertChild(Node node) {
        throw new UnsupportedOperationException("can not add child to method");
      }
    }
  }

  public record Key(Pattern separator, String value) {
    public static Key infer(String value) {
      return value.contains("/") ? slashSeparated(value) : dotSeparated(value);
    }

    private static final Pattern dotOrMethodSeparator =
      Pattern.compile("[.#]");

    public static Key dotSeparated(String value) {
      Objects.requireNonNull(value, "value");
      return new Key(dotOrMethodSeparator, value);
    }

    private static final Pattern slashOrMethodSeparator =
      Pattern.compile("[/#]");

    public static Key slashSeparated(String value) {
      Objects.requireNonNull(value, "value");
      return new Key(slashOrMethodSeparator, value);
    }

    public String[] split() {
      return separator.split(value);
    }

    public String lastPart() {
      var parts = split();
      if (parts.length == 0) {
        return value;
      }
      return parts[parts.length - 1];
    }
  }

  private final Node root;

  private AccessGraph(Node root) {
    this.root = root;
  }

  private Collection<Node> findClosestMatch(Key key) {
    return findClosestMatch(root, key.split(), 0);
  }

  private static Collection<Node> findClosestMatch(Node node, String[] key, int depth) {
    if (depth >= key.length) {
      return List.of(node);
    }
    if (depth == key.length - 1) {
      return findClosestMatchOrMethods(node, key[key.length - 1]);
    }
    return findClosestChild(node, key, depth);
  }

  private static Collection<Node> findClosestChild(Node node, String[] key, int depth) {
    for (var child : node.children()) {
      if (child.name().equals(key[depth])) {
        return findClosestMatch(child, key, depth + 1);
      }
    }
    return List.of(node);
  }

  private static Collection<Node> findClosestMatchOrMethods(Node node, String lastKey) {
    var children = node.children().stream()
      .filter(child -> child.name().equals(lastKey))
      .toList();
    return children.isEmpty() ? List.of(node) : children;
  }

  /**
   * Checks if the use of the class or method under the given key is permitted.
   * <p>
   * When checking access to methods prefer {@link AccessGraph#isMethodPermitted(MethodSignature)}.
   *
   * @param key Key of the checked package, class or method.
   * @return True if the caller has permission to use classes or methods
   *   under this key.
   */
  public boolean isPermitted(Key key) {
    var matches = findClosestMatch(key);
    return !matches.isEmpty()
      && matches.iterator().next().access().equals(Access.Permitted);
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
    var key = Key.slashSeparated(signature.formatWithoutTypes());
    var matches = findClosestMatch(key);
    var access = switch (matches.size()) {
      case 0 -> Access.Denied;
      case 1 -> matches.iterator().next().access();
      default -> findBestMatch(signature, matches).map(Node::access);
    };
    return access.equals(Access.Permitted);
  }

  private Optional<Node> findBestMatch(MethodSignature signature, Collection<Node> nodes) {
    for (var node : nodes) {
      if (node instanceof Node.Method method
          && method.signature.matches(signature)) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "AccessGraph(%s)".formatted(root.children().toString());
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

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private final Node root =
      new Node.Path("root", Access.Denied, new ArrayList<>());

    private Builder() {}

    @CanIgnoreReturnValue
    public Builder permit(Key key) {
      var node = createNode(key, Access.Permitted);
      insert(key, node);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder deny(Key key) {
      var node = createNode(key, Access.Denied);
      insert(key, node);
      return this;
    }

    private void insert(Key key, Node node) {
      var parent = resolveParent(key, node);
      parent.insertChild(node);
    }

    private Node resolveParent(Key key, Node inserted) {
      var path = key.split();
      if (path.length == 1) {
        return root;
      }
      var parentPath = Arrays.copyOf(path, path.length - 1);
      return resolveParent(root, parentPath, 0, inserted);
    }

    private Node resolveParent(Node target, String[] key, int depth, Node inserted) {
      if (depth >= key.length) {
        return target;
      }
      for (var child : target.children()) {
        if (child.name().equals(key[depth])) {
          return resolveParent(child, key, depth + 1, inserted);
        }
      }
      return insertRemainingPath(target, key, depth, inserted.access());
    }

    private Node insertRemainingPath(Node target, String[] key, int depth, Access access) {
      for (int index = depth; index < key.length; index++) {
        var intermediate = new Node.Path(key[index], access, new ArrayList<>());
        target.insertChild(intermediate);
        target = intermediate;
      }
      return target;
    }

    private Node createNode(Key key, Access access) {
      if (isMethodSignature(key.value())) {
        var signature = MethodSignature.parse(key.value());
        return new Node.Method(signature, access);
      }
      return new Node.Path(key.lastPart(), access, new ArrayList<>());
    }

    private static boolean isMethodSignature(String path) {
      return path.indexOf(MethodSignature.methodSeparator()) >= 0;
    }

    // TODO: Create defensive copies after building
    public AccessGraph create() {
      return new AccessGraph(root);
    }
  }
}
