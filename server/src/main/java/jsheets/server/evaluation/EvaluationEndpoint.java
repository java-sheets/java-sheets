package jsheets.server.evaluation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import jsheets.server.endpoint.Endpoint;

import javax.inject.Inject;

public final class EvaluationEndpoint implements Endpoint {
  private final ConnectionTable table;

  @Inject
  EvaluationEndpoint(ConnectionTable table) {
    this.table = table;
  }

  @Override
  public void configure(Javalin server) {
    server.ws("/api/v1/evaluate", table::listen);
  }

  static final class ConnectionTable {
    private final EvaluationConnection.Factory factory;
    private final Map<WsContext, EvaluationConnection> table =
      new ConcurrentHashMap<>();

    @Inject
    private ConnectionTable(EvaluationConnection.Factory connectionFactory) {
      this.factory = connectionFactory;
    }

    public void listen(WsConfig handler) {
      handler.onConnect(forward(EvaluationConnection::connect)::accept);
      handler.onClose(forward(EvaluationConnection::close)::accept);
      handler.onBinaryMessage(forward(EvaluationConnection::receive)::accept);
    }

    private <T extends WsContext> Consumer<T> forward(
      BiConsumer<EvaluationConnection, T> delegate
    ) {
      return context -> {
        var connection = resolve(context);
        delegate.accept(connection, context);
      };
    }

    private EvaluationConnection resolve(WsContext context) {
      return table.computeIfAbsent(context, id ->
        factory.withCloseHook(() -> remove(context))
      );
    }

    private void remove(WsContext context) {
      table.remove(context);
    }
  }
}
