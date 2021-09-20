package jsheets.server.evaluation.connection;

import com.google.common.base.MoreObjects;
import com.google.common.flogger.FluentLogger;
import jsheets.*;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Evaluating implements Stage {
  private final AtomicBoolean terminated = new AtomicBoolean(false);
  private final Evaluation evaluation;

  private Evaluating(Evaluation evaluation) {
    this.evaluation = evaluation;
  }

  @Override
  public CompletableFuture<Stage> run(Session session) {
    return null;
  }

  @Override
  public Stage receive(Session session, EvaluateRequest request) {
    return switch (request.getMessageCase()) {
      case STOP -> preempt(session);
      default -> receiveInvalidMessage(session);
    };
  }

  private Stage preempt(Session session) {
    return Closed.create();
  }

  private Stage receiveInvalidMessage(Session session) {
    return Closed.create();
  }

  @Override
  public void close(Session session) {

  }

  private final class UpstreamListener
    implements jsheets.runtime.evaluation.Evaluation.Listener {

    private static final FluentLogger log = FluentLogger.forEnclosingClass();

    private final Session downstream;

    private UpstreamListener(Session downstream) {
      this.downstream = downstream;
    }

    @Override
    public void close() {}

    @Override
    public void send(EvaluateResponse response) {
      if (terminated.get()) {
        discardDueToTermination(response);
        return;
      }
      sendInRunningSession(response);
    }

    private void sendInRunningSession(EvaluateResponse response) {
      try {
        var payload = ByteBuffer.wrap(response.toByteArray());
        downstream.getRemote().sendBytes(payload);
      } catch (IOException failedTransmission) {
        reportFailedTransmission(response, failedTransmission);
      }
    }

    private void discardDueToTermination(EvaluateResponse message) {
      log.atWarning().log(
        "discarding message because session to %s has been closed",
        downstream.getRemoteAddress()
      );
    }

    private void reportFailedTransmission(
      EvaluateResponse response,
      IOException failedTransmission
    ) {
      log.atWarning()
        .withCause(failedTransmission)
        .log(
          "could not write message to %s",
          downstream.getRemoteAddress()
        );
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
        .add("downstreamAddress", downstream.getRemoteAddress())
        .toString();
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("evaluation", evaluation)
      .toString();
  }
}