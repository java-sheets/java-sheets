package jsheets.sandbox;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jsheets.sandbox.validation.Analysis;
import jsheets.sandbox.validation.Rule;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
    reader.accept(new ClassCheck(rules, analysis), 0);
  }

  static final class ClassCheck extends ClassVisitor {
    private final Collection<Rule> rules;
    private final Analysis analysis;

    private ClassCheck(Collection<Rule> rules, Analysis analysis) {
      super(Opcodes.ASM9);
      this.rules = rules;
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
      return new MethodCheck(name, rules, analysis);
    }
  }

  static final class MethodCheck extends MethodVisitor {
    private final String name;
    private final Collection<Rule> rules;
    private final Analysis analysis;

    private MethodCheck(
      String name,
      Collection<Rule> rules,
      Analysis analysis
    ) {
      super(Opcodes.ASM9);
      this.name = name;
      this.rules = rules;
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
      var call = new Rule.MethodCall(owner, name);
      for (var rule : rules) {
        rule.visitCall(analysis, call);
      }
    }
  }
}