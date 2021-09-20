package jsheets.server.evaluation.connection;

import jsheets.EvaluateRequest;
import jsheets.StartEvaluationRequest;
import jsheets.runtime.evaluation.EvaluationEngine;
import org.eclipse.jetty.websocket.api.Session;

import java.util.concurrent.CompletableFuture;

public final class Connecting implements Stage {
  private final StartEvaluationRequest startRequest;
  private final EvaluationEngine engine;
  private volatile boolean closedDuringOperation;

  private Connecting(
    EvaluationEngine engine,
    StartEvaluationRequest startRequest
  ) {
    this.engine = engine;
    this.startRequest = startRequest;
  }

  @Override
  public CompletableFuture<Stage> run(Session session) {
    return null;
  }

  @Override
  public Stage receive(Session session, EvaluateRequest request) {
    return switch (request.getMessageCase()) {
      case START -> reportAlreadyStarted(session);
      case STOP -> preempt(session);
      case MESSAGE_NOT_SET -> {
      	session.close();
      	yield Closed.create();
			}
    };
  }

 	private Stage reportAlreadyStarted(Session session) {
  	return Closed.create();
	}

	private Stage preempt(Session session) {
  	return Closed.create();
	}

  @Override
  public void close(Session session) {
    closedDuringOperation = true;
  }
}
