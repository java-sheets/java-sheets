package jsheets.server.evaluation.connection;

import jsheets.EvaluateRequest;
import org.eclipse.jetty.websocket.api.Session;

import java.util.concurrent.CompletableFuture;

public interface Stage {
  CompletableFuture<Stage> run(Session session);

  Stage receive(Session session, EvaluateRequest request);
  void close(Session session);
}
