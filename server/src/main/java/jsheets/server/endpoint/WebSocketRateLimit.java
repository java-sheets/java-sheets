package jsheets.server.endpoint;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.javalin.http.HttpCode;
import io.javalin.websocket.WsConfig;
import org.eclipse.jetty.websocket.api.CloseStatus;

import java.util.Objects;
import java.util.function.Consumer;

public final class WebSocketRateLimit implements Consumer<WsConfig> {
  public static WebSocketRateLimit withBandwith(Bandwidth bandwidth) {
    Objects.requireNonNull(bandwidth, "bandwidth");
    return new WebSocketRateLimit(
      Bucket4j.builder()
        .addLimit(bandwidth)
        .build()
    );
  }

  private final Bucket bucket;

  private WebSocketRateLimit(Bucket bucket) {
    this.bucket = bucket;
  }

  private static final CloseStatus tooManyRequests = new CloseStatus(
    HttpCode.TOO_MANY_REQUESTS.getStatus(),
    "the evaluation engine load is too high right now"
  );

  @Override
  public void accept(WsConfig config) {
    config.onConnect(connection -> {
      if (!bucket.tryConsume(1)) {
        connection.session.close(tooManyRequests);
      }
    });
  }
}
