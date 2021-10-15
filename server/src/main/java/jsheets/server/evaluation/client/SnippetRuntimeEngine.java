package jsheets.server.evaluation.client;

import java.util.Objects;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import jsheets.EvaluateRequest;
import jsheets.EvaluateResponse;
import jsheets.SnippetRuntimeGrpc;
import jsheets.SnippetRuntimeGrpc.SnippetRuntimeStub;
import jsheets.StartEvaluationRequest;
import jsheets.StopEvaluationRequest;
import jsheets.evaluation.Evaluation;
import jsheets.evaluation.EvaluationEngine;

/**
 * Client side {@link EvaluationEngine} that connects to a
 * {@code SnippetRuntime} to evaluate snippets.
 */
final class SnippetRuntimeEngine implements EvaluationEngine {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static SnippetRuntimeEngine forChannel(Channel channel) {
    Objects.requireNonNull(channel, "channel");
    return new SnippetRuntimeEngine(SnippetRuntimeGrpc.newStub(channel));
  }

  private final SnippetRuntimeStub client;

  private SnippetRuntimeEngine(SnippetRuntimeStub client) {
    this.client = client;
  }

  @Override
  public Evaluation start(
    StartEvaluationRequest request,
    Evaluation.Listener listener
  ) {
    var snippetId = request.getSnippet().getReference().getSnippetId();
    var observer = new ListenerBoundObserver(snippetId, listener);
    var call = client.evaluate(observer);
    call.onNext(wrapStartRequest(request));
    return () -> call.onNext(createStopRequest());
  }

  private EvaluateRequest wrapStartRequest(StartEvaluationRequest request) {
    return EvaluateRequest.newBuilder()
      .setStart(request)
      .build();
  }

  private EvaluateRequest createStopRequest() {
    return EvaluateRequest.newBuilder()
      .setStop(StopEvaluationRequest.getDefaultInstance())
      .build();
  }

  static final class ListenerBoundObserver implements StreamObserver<EvaluateResponse> {
    private final String snippetId;
    private final Evaluation.Listener listener;

    private ListenerBoundObserver(
      String snippetId,
      Evaluation.Listener listener
    ) {
      this.snippetId = snippetId;
      this.listener = listener;
    }

    @Override
    public void onNext(EvaluateResponse response) {
      listener.send(response);
    }

    @Override
    public void onCompleted() {
      listener.close();
    }

    private static final MetadataKey<String> snippetIdKey =
      MetadataKey.single("snippetId", String.class);

    @Override
    public void onError(Throwable failure) {
      log.atWarning()
        .withCause(failure)
        .with(snippetIdKey, snippetId)
        .log("received error response in evaluate call");
    }
  }

  @Override
  public String toString() {
    return "SnippetRuntimeEngine(client=%s)".formatted(client);
  }
}