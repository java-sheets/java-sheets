package jsheets.evaluation.sandbox;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jsheets.evaluation.sandbox.validation.Analysis;
import jsheets.evaluation.sandbox.validation.Rule;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class SandboxBytecodeCheck {
  public static SandboxBytecodeCheck withRules(Rule... rules) {
    Objects.requireNonNull(rules, "rules");
    return withRules(List.of(rules));
  }

  public static SandboxBytecodeCheck withRules(Collection<Rule> rules) {
    Objects.requireNonNull(rules, "rules");
    return new SandboxBytecodeCheck(rules);
  }

  private final Collection<Rule> rules;

  private SandboxBytecodeCheck(Collection<Rule> rules) {
    this.rules = rules;
  }

  public void run(Analysis analysis, byte[] classCode) {
    var reader = new ClassReader(classCode);
    reader.accept(new ClassCheck(reader.getClassName(), rules, analysis), 0);
  }

  static final class ClassCheck extends ClassVisitor {
    private final String className;
    private final Collection<Rule> rules;
    private final Analysis analysis;

    private ClassCheck(
      String className,
      Collection<Rule> rules,
      Analysis analysis
    ) {
      super(Opcodes.ASM9);
      this.rules = rules;
      this.className = className;
      this.analysis = analysis;
    }

    @Override
    public MethodVisitor visitMethod(
      int access,
      String name,
      String descriptor,
      String signature,
      String[] exceptions
    ) {
      return new MethodCheck(name, className, rules, analysis);
    }
  }

  static final class MethodCheck extends MethodVisitor {
    private final String name;
    private final String className;
    private final Collection<Rule> rules;
    private final Analysis analysis;

    private MethodCheck(
      String name,
      String className,
      Collection<Rule> rules,
      Analysis analysis
    ) {
      super(Opcodes.ASM9);
      this.name = name;
      this.rules = rules;
      this.className = className;
      this.analysis = analysis;
    }

    @Override
    public void visitMethodInsn(
      int opcode,
      String owner,
      String name,
      String descriptor,
      boolean isInterface
    ) {
      var type = Type.getMethodType(descriptor);
      var ownerClass = Type.getObjectType(owner).getClassName();
      var call = new Rule.MethodCall(createAccessPoint(), ownerClass, name, type);
      for (var rule : rules) {
        rule.visitCall(analysis, call);
      }
    }

    @Override
    public void visitFieldInsn(
      int opcode,
      String owner,
      String field,
      String descriptor
    ) {
      var ownerClass = Type.getObjectType(owner).getClassName();
      var access = new Rule.FieldAccess(createAccessPoint(), ownerClass, field);
      for (var rule : rules) {
        rule.visitFieldAccess(analysis, access);
      }
    }

    private Rule.AccessPoint createAccessPoint() {
      return new Rule.AccessPoint(className, name);
    }
  }
}