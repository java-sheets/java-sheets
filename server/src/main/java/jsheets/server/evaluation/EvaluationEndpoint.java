package jsheets.server.evaluation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.github.bucket4j.Bandwidth;
import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import jsheets.server.endpoint.Endpoint;
import jsheets.server.endpoint.WebSocketRateLimit;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

public final class EvaluationEndpoint implements Endpoint {
  private final ConnectionTable table;
  @Nullable
  private final Bandwidth bandwidth;

  @Inject
  EvaluationEndpoint(
    ConnectionTable table,
    @Named("evaluationBandwidth") @Nullable Bandwidth bandwidth
  ) {
    this.table = table;
    this.bandwidth = bandwidth;
  }

  private static final String route = "/api/v1/evaluate";

  @Override
  public void configure(Javalin server) {
    if (bandwidth != null) {
      server.wsBefore(route, WebSocketRateLimit.withBandwith(bandwidth));
    }
    server.ws(route, table::listen);
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
