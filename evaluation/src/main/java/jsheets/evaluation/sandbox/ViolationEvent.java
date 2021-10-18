package jsheets.evaluation.sandbox;

import java.util.Collection;
import java.util.Map;

import io.soabase.recordbuilder.core.RecordBuilder;
import jsheets.evaluation.EvaluationEvent;
import jsheets.evaluation.sandbox.validation.Analysis;

@RecordBuilder
public record ViolationEvent(
  String snippetId,
  String componentId,
  Collection<Analysis.Violation> violations,
  Map<String, Object> labels
) implements EvaluationEvent {}
