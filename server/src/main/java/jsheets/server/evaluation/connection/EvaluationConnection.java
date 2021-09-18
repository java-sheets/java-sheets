package jsheets.server.evaluation.connection;

import com.google.common.base.MoreObjects;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import io.javalin.websocket.*;
import jsheets.*;
import jsheets.server.evaluation.Evaluation;
import jsheets.server.evaluation.EvaluationEngine;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API">WebSocket</a>
 * implementation of the <strong<Evaluation Protocol</strong>, allowing web-clients
 * to evaluate code without directly connecting to a {@code SnippetRuntime}.
 * <p>
 * It acts as a proxy between the client and a {@code SnippetRuntime} and
 * employs some additional control checks to detect erroneous client behavior.
 * We refer to both parties as <strong>upstream</strong>upstream and
 * <strong>downstream</strong> respectively.
 * <p>
 * The <strong>upstream</strong> is the {@code SnippetRuntime} executing the evaluation.
 * It may be located remotely or inside the same process, and is therefore
 * abstracted by the {@link EvaluationEngine}. Code obtains an instance of
 * {@link Evaluation}, which represents the upstream connection.
 * <p>
 * The <strong>Downstream</strong> is the
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API">WebSocket Client</a>
 * requesting the evaluation.
 * <p>
 * The connection is stateful, stages describe the current state and its behavior.
 * <ul>
 *   <li>
 *     <strong>Initial:</strong>
 *  	 Transitions into <italic>Connecting</italic> after receiving a
 *  	 {@link StartEvaluationRequest}.
 *   </li>
 *   <li>
 *			<strong>Connecting:</strong>
 * 			Is the active stage while waiting for the evaluation to start,
 * 		  after which it transitions to <italic>Evaluating</italic>.
 *			If an asynchronous {@link StopEvaluationRequest} is received, the
 *			evaluation is stopped immediately after is has been created and the
 *			stage changes to <italic>Terminated</italic>.
 *   </li>
 *   <li>
 *     <strong>Evaluating:</strong>
 *     Active stage while the evaluation is taking place. Can be terminated by
 *     either the <italic>upstream</italic> or preemptively by receiving a
 *     {@link StopEvaluationRequest}. Any message send by the <italic>upstream</italic>
 *     is forwarded to the client. Once completed, transitions into <italic>Terminated</italic>.
 *   </li>
 *   <li>
 *     <strong>Terminated:</strong>
 *     Final stage, indicating that the connection is done and does
 *     not accept any further requests. The <italic>downstream</italic>
 *     connection is expected to be closed at this point.
 *   </li>
 * </ul>
 */
public final class EvaluationConnection {
  private enum Stage { Initial, Connecting, Evaluating, Terminated }

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final AtomicReference<Stage> stage =
    new AtomicReference<>(Stage.Initial);

  private final EvaluationEngine engine;

  private volatile Evaluation evaluation;

  @Inject
  EvaluationConnection(EvaluationEngine engine) {
    this.engine = engine;
  }

  public void listen(WsConfig connection) {
    connection.onConnect(this::connect);
    connection.onClose(this::close);
    connection.onMessage(this::receive);
  }

  private static final FluentLogger.Api logPreemption = log.atInfo()
    .atMostEvery(5, TimeUnit.SECONDS);

  private void close(WsCloseContext context) {
    var previousStage = stage.getAndSet(Stage.Terminated);
    if (previousStage.equals(Stage.Evaluating)) {
      evaluation.stop();
      return;
    }
    if (!previousStage.equals(Stage.Terminated)) {
       logPreemption.with(sessionIdMetadata, context.getSessionId())
       .log("evaluation call was closed without receiving stop");
    }
  }

  private static final CloseStatus illegalStage =
    new CloseStatus(HttpStatus.CONFLICT_409, "illegal stage");

  private void connect(WsConnectContext context) {
    if (!stage.compareAndSet(Stage.Initial, Stage.Connecting)) {
      context.session.close(illegalStage);
    }
  }

  private void receive(WsMessageContext context) {
    var request = readRequest(context);
    switch (request.getMessageCase()) {
      case START -> receiveStart(context, request.getStart());
      case STOP -> receiveStop(context, request.getStop());
      default -> receiveInvalidMessage(context);
    }
  }

  private EvaluateRequest readRequest(WsMessageContext context) {
    try {
      return context.messageAsClass(EvaluateRequest.class);
    } catch (Exception failedDeserialization) {
      log.atWarning()
        .withCause(failedDeserialization)
        .log("received invalid EvaluateRequest");
      return EvaluateRequest.getDefaultInstance();
    }
  }

  private void receiveStart(WsMessageContext context, StartEvaluationRequest request) {
    establishConnection(context, request);
  }

  private static final CloseStatus CANCELLED =
    new CloseStatus(HttpStatus.BAD_REQUEST_400, "cancelled");

  private void establishConnection(
		WsMessageContext context,
    StartEvaluationRequest request
  ) {
    var listener = new UpstreamListener(context.session);
    var upstream = engine.start(request, listener);
    if (!completeConnecting(upstream)) {
      ensureSessionIsClosed(context.session, CANCELLED);
    }
  }

  private boolean completeConnecting(jsheets.server.evaluation.Evaluation upstream) {
    if (!stage.compareAndSet(Stage.Connecting, Stage.Evaluating)) {
      log.atWarning().log("the evaluation was terminated in the connecting stage");
      upstream.stop();
      return false;
    }
    evaluation = upstream;
    return true;
  }

  private void complete() {

  }

  final class UpstreamListener implements Evaluation.Listener {
    private static final FluentLogger log = FluentLogger.forEnclosingClass();

    private final Session downstream;

    private UpstreamListener(Session downstream) {
      this.downstream = downstream;
    }

    @Override
    public void onEnd() {
      EvaluationConnection.this.complete();
    }

    @Override
    public void onError(EvaluationError error) {
      send(EvaluateResponse.newBuilder().setError(error));
    }

    @Override
    public void onResult(EvaluationResult result) {
      send(EvaluateResponse.newBuilder().setResult(result));
    }

    @Override
    public void onMissingSources(MissingSources sources) {
      send(EvaluateResponse.newBuilder().setMissingSources(sources));
    }

    private void send(EvaluateResponse.Builder message) {
      var payload = ByteBuffer.wrap(message.build().toByteArray());
      try {
        downstream.getRemote().sendBytes(payload);
      } catch (IOException failedTransmission) {
        log.atWarning().log(
          "could not write {} to {}",
          message.getMessageCase(),
          downstream.getRemoteAddress()
        );
      }
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
        .add("downstreamAddress", downstream.getRemoteAddress())
        .toString();
    }
  }


  private void ensureSessionIsClosed(Session session, CloseStatus status) {
    // close does not throw an exception if the session is already closed
    session.close(status);
  }

  private void receiveStop(WsContext context, StopEvaluationRequest stop) {
    var previousStage = stage.getAndSet(Stage.Terminated);
    switch (previousStage) {
      case Connecting -> context.session.close();
      case Evaluating -> {
        evaluation.stop();
        context.session.close();
      }
      case Terminated -> {
        log.atWarning()
          .with(sessionIdMetadata, context.getSessionId())
          .log("received stop request for terminated evaluation");
        context.session.close(illegalStage);
      }
      case Initial -> {
        log.atWarning()
          .with(sessionIdMetadata, context.getSessionId())
          .log("received stop request for uninitialized evaluation");
        context.session.close(illegalStage);
      }
    }
  }

  private static final CloseStatus invalidMessage = new CloseStatus(
    HttpStatus.BAD_REQUEST_400,
    "invalid message"
  );

  private static final MetadataKey<String> sessionIdMetadata =
    MetadataKey.single("ws-session-id", String.class);

  private static final MetadataKey<Stage> stageMetadata =
    MetadataKey.single("connection-stage", Stage.class);

  private void receiveInvalidMessage(WsContext context) {
    var previousStage = stage.getAndSet(Stage.Terminated);
    log.atWarning()
      .with(sessionIdMetadata, context.getSessionId())
      .with(stageMetadata, previousStage)
      .log("closing session after receiving an invalid message");
    context.session.close(invalidMessage);
  }

  @Override
  public String toString() {
    var currentEvaluation = evaluation;
    return MoreObjects.toStringHelper(this)
      .add("engine", engine)
      .add("evaluation", currentEvaluation)
      .add("running", currentEvaluation != null)
      .toString();
  }
}