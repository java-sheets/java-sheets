package jsheets.evaluation;

import java.util.Map;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EvaluationStartEvent(
  String snippetId,
  Map<String, Object> labels
) implements EvaluationEvent {}