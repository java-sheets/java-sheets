package jsheets.sandbox.access;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AccessGraphNode implements Iterable<AccessGraphNode> {
  private Access access;
  private final Collection<AccessGraphNode> children;

  protected AccessGraphNode(Access access, Collection<AccessGraphNode> nodes) {
    this.access = access;
    this.children = nodes;
  }

  public abstract String key();

  public abstract boolean matchesKey(String key);
  public abstract boolean matchesMethod(MethodSignature signature);

  public void changeAccess(Access access) {
    Objects.requireNonNull(access, "access");
    this.access = access;
  }

  public Access access() {
    return access;
  }

  public void insertChild(AccessGraphNode node) {
    Objects.requireNonNull(node, "node");
    this.children.add(node);
  }

  public Stream<AccessGraphNode> children() {
    return children.stream();
  }

  @Override
  public Iterator<AccessGraphNode> iterator() {
    return children.iterator();
  }

  @Override
  public String toString() {
    return isLeaf() ? formatLeaf() : formatBranch();
  }

  private boolean isLeaf() {
    return children.isEmpty();
  }

  private String formatBranch() {
    return "AccessGraphNode(key=%s, access=%s, nodes=%s)"
      .formatted(key(), access, children);
  }

  private String formatLeaf() {
    return "AccessGraphNode(key=%s, access=%s)"
      .formatted(key(), access);
  }

  public static final class Path extends AccessGraphNode {
    public static AccessGraphNode create(
      String key,
      Access access,
      Collection<AccessGraphNode> nodes
    ) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(access, "access");
      Objects.requireNonNull(nodes, "nodes");
      return new Path(key, access, nodes);
    }

    private final String key;

    private Path(String key, Access access, Collection<AccessGraphNode> nodes) {
      super(access, nodes);
      this.key = key;
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public boolean matchesKey(String key) {
      return this.key.equals(key);
    }

    @Override
    public boolean matchesMethod(MethodSignature signature) {
      return signature.methodName().equals(key);
    }
  }

  public static final class Method extends AccessGraphNode {
    public static AccessGraphNode create(
      MethodSignature method,
      Access access,
      Collection<AccessGraphNode> nodes
    ) {
      Objects.requireNonNull(method, "method");
      Objects.requireNonNull(access, "access");
      Objects.requireNonNull(nodes, "nodes");
      return new Method(method, access, nodes);
    }

    private final MethodSignature method;

    private Method(
      MethodSignature method,
      Access access,
      Collection<AccessGraphNode> nodes
    ) {
      super(access, nodes);
      this.method = method;
    }

    @Override
    public String key() {
      return method.formatNameAndParameters();
    }

    @Override
    public boolean matchesKey(String key) {
      return false;
    }

    @Override
    public boolean matchesMethod(MethodSignature signature) {
      return signature.matches(method);
    }
  }
}