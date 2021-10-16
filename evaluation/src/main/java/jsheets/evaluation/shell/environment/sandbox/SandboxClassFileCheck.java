package jsheets.evaluation.shell.environment.sandbox;

import java.util.Collection;
import java.util.Objects;

import jdk.jshell.spi.ExecutionControl;
import jsheets.evaluation.sandbox.SandboxBytecodeCheck;
import jsheets.evaluation.sandbox.validation.Analysis;
import jsheets.evaluation.sandbox.validation.Rule;
import jsheets.evaluation.shell.environment.ClassFileStore;

public final class SandboxClassFileCheck implements ClassFileStore {
  public static SandboxClassFileCheck of(Collection<Rule> rules) {
    Objects.requireNonNull(rules, "rules");
    return new SandboxClassFileCheck(rules);
  }

  private final Collection<Rule> rules;

  private SandboxClassFileCheck(Collection<Rule> rules) {
    this.rules = rules;
  }

  @Override
  public void redefine(ExecutionControl.ClassBytecodes[] bytecodes) {
    var analysis = Analysis.create();
    var check = SandboxBytecodeCheck.withRules(rules);
    for (var binary : bytecodes) {
      check.run(analysis, binary.bytecodes());
    }
    analysis.reportViolations();
  }

  @Override
  public void load(ExecutionControl.ClassBytecodes[] bytecodes) {

  }
}