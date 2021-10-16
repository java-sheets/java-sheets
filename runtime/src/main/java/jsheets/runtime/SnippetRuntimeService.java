package jsheets.runtime;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.flogger.FluentLogger;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import jsheets.EvaluateRequest;
import jsheets.EvaluateResponse;
import jsheets.SnippetRuntimeGrpc.SnippetRuntimeImplBase;
import jsheets.StartEvaluationRequest;
import jsheets.StopEvaluationRequest;
import jsheets.evaluation.Evaluation;
import jsheets.evaluation.EvaluationEngine;

final class SnippetRuntimeService extends SnippetRuntimeImplBase {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final EvaluationEngine engine;

  @Inject
  SnippetRuntimeService(EvaluationEngine engine) {
    this.engine = engine;
  }

  @Override
  public StreamObserver<EvaluateRequest> evaluate(
    StreamObserver<EvaluateResponse> responseStream
  ) {
    return new Call(responseStream);
  }

  final class Call
    implements StreamObserver<EvaluateRequest>, Evaluation.Listener {

    private final StreamObserver<EvaluateResponse> responseStream;
    private final Lock lock = new ReentrantLock();
    private Evaluation evaluation;

    private Call(StreamObserver<EvaluateResponse> responseStream) {
      this.responseStream = responseStream;
    }

    @Override
    public void onNext(EvaluateRequest request) {
      lock.lock();
      try {
        switch (request.getMessageCase()) {
          case STOP -> processStop(request.getStop());
          case START -> processStart(request.getStart());
          default -> processUnknown(request);
        }
      } finally {
        lock.unlock();
      }
    }

    private static final Status invalidState =
      Status.FAILED_PRECONDITION.withDescription("state");

    private void processStop(StopEvaluationRequest request) {
      if (evaluation == null) {
        log.atWarning().log("received stop request without active evaluation");
        responseStream.onError(invalidState.asException());
        return;
      }
      evaluation.stop();
      evaluation = null;
      log.atFine().log("closed evaluation");
    }

    private void processStart(StartEvaluationRequest request) {
      if (evaluation != null) {
        log.atWarning().log("received start request with active evaluation");
        responseStream.onError(invalidState.asException());
        return;
      }
      evaluation = engine.start(request, this);
      log.atFine().log("started evaluation");
    }

    private void processUnknown(EvaluateRequest request) {
      log.atWarning()
        .atMostEvery(5, TimeUnit.SECONDS)
        .log("received unknown request message: %s", request);
    }

    @Override
    public void onError(Throwable failure) {}

    @Override
    public void onCompleted() {
      lock.lock();
      try {
        if (evaluation != null) {
          evaluation.stop();
          evaluation = null;
        }
      } finally {
        lock.unlock();
      }
    }

    // Listens to EvaluationEngine
    @Override
    public void send(EvaluateResponse response) {
      responseStream.onNext(response);
    }

    // Listens to EvaluationEngine
    @Override
    public void close() {
      responseStream.onCompleted();
    }
  }
}