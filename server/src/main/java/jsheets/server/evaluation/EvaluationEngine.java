package jsheets.server.evaluation;

import jsheets.StartEvaluationRequest;

public interface EvaluationEngine {
  Evaluation start(StartEvaluationRequest request, Evaluation.Listener listener);
}
