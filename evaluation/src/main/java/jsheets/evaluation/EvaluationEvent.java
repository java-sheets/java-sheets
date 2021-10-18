package jsheets.evaluation;

import jsheets.event.LabeledEvent;

/**
 * The evaluation engine produces events during preprocessing and evaluation
 * of snippets that can be used to create metrics and trigger custom code.
 */
public interface EvaluationEvent extends LabeledEvent {
  String snippetId();
}
