package jsheets.server.evaluation.connection;

import jsheets.EvaluateRequest;
import org.eclipse.jetty.websocket.api.Session;

import java.util.concurrent.CompletableFuture;

public class Closed implements Stage {
  public static Closed create() {
    return new Closed();
  }

  @Override
  public CompletableFuture<Stage> run(Session session) {
    return null;
  }

  @Override
  public Stage receive(Session session, EvaluateRequest request) {
    return null;
  }

  @Override
  public void close(Session session) {

  }
}
