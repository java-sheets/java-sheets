package jsheets.server.evaluation;

import io.javalin.Javalin;
import io.javalin.websocket.WsHandler;
import jsheets.server.Endpoint;
import jsheets.server.evaluation.connection.EvaluationConnection;

import javax.inject.Inject;
import javax.inject.Provider;

public final class EvaluationEndpoint implements Endpoint {
  private final Provider<EvaluationConnection> connectionFactory;

  @Inject
  EvaluationEndpoint(Provider<EvaluationConnection> connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  @Override
  public void configure(Javalin server) {
    server.ws("/api/v1/evaluate", this::evaluate);
  }

  private void evaluate(WsHandler socket) {
    var connection = connectionFactory.get();
    connection.listen(socket);
  }
}
