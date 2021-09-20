package jsheets.runtime.evaluation;

import jsheets.EvaluateResponse;

public interface Evaluation {
	interface Listener {
		default void send(EvaluateResponse response) {}
		default void close() {}
	}

	void stop();
}
