package jsheets.server.evaluation;

import jsheets.EvaluationError;
import jsheets.EvaluationResult;
import jsheets.MissingSources;

public interface Evaluation {
  interface Listener {
    default void onError(EvaluationError error) {}
    default void onResult(EvaluationResult result) {}
    default void onMissingSources(MissingSources sources) {}
    default void onEnd() {}
  }

  void stop();
}
