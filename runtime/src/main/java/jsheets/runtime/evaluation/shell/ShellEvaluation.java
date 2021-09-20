package jsheets.runtime.evaluation.shell;

import com.google.common.base.MoreObjects;
import com.google.common.flogger.FluentLogger;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jsheets.CodeSpan;
import jsheets.EvaluateResponse;
import jsheets.EvaluationError;
import jsheets.EvaluationResult;
import jsheets.SnippetSources;
import jsheets.StartEvaluationRequest;
import jsheets.runtime.evaluation.Evaluation;
import jsheets.runtime.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.source.SharedSources;

import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class ShellEvaluation implements Evaluation {
	private enum Stage { Initial, Starting, Evaluating, Terminated }

	private static final FluentLogger log = FluentLogger.forEnclosingClass();

	private volatile JShell shell;
	private final Evaluation.Listener listener;
	private final SharedSources sources;
	private final ExecutionEnvironment environment;
	private final AtomicReference<Stage> stage =
		new AtomicReference<>(Stage.Initial);

	private final MessageSink messageSinks;
	private final Duration flushInterval;
	private final ScheduledExecutorService scheduler;
	private final AtomicReference<String> currentComponentId = new AtomicReference<>(null);
	private final AtomicReference<ScheduledFuture<?>> flushTask = new AtomicReference<>(null);

	ShellEvaluation(
		Evaluation.Listener listener,
		Clock clock,
		ExecutionEnvironment environment,
		ScheduledExecutorService scheduler,
		Duration flushInterval
	) {
		this.listener = listener;
		this.scheduler = scheduler;
		this.environment = environment;
		this.flushInterval = flushInterval;
		sources = SharedSources.createEmpty(clock);
		messageSinks = new MessageSink(listener);
	}

	public void start(StartEvaluationRequest request) {
		shell = createShell();
		scheduleFlush();
		listener.send(evaluateSources(request));
		listener.close();
		cleanUp();
	}

	private EvaluateResponse evaluateSources(StartEvaluationRequest request) {
		var response = EvaluateResponse.newBuilder();
		for (var source : request.getSourcesList()) {
		  source.getCodeComponentsList().stream()
        .sorted(Comparator.comparing(SnippetSources.CodeComponent::getOrder))
        .forEachOrdered(component -> evaluateComponent(component, response));
		}
		return response.build();
	}

	private void evaluateComponent(
		SnippetSources.CodeComponent component,
		EvaluateResponse.Builder response
	) {
		currentComponentId.set(component.getId());
		try {
			for (var snippet : shell.eval(component.getCode()) ) {
				reportEvent(component.getId(), snippet, response);
			}
			flushMessages();
		} catch (Exception failedEvaluation) {
			log.atWarning()
				.atMostEvery(5, TimeUnit.SECONDS)
				.withCause(failedEvaluation).log("shell evaluation failed");
			response.addError(
				EvaluationError.newBuilder()
					.setComponentId(component.getId())
					.setKind("internal")
					.build()
			);
		}
	}

	private void reportEvent(
		String componentId,
		SnippetEvent event,
		EvaluateResponse.Builder response
	) {
		switch (event.status()) {
			case VALID -> {
				if (event.value() == null) {
					return;
				}
				response.addResult(
					EvaluationResult.newBuilder()
						.setComponentId(componentId)
						.setOutput(event.value())
						.build()
				);
			}
			case REJECTED -> reportFailedEvent(componentId, event, response);
		}
	}

	private void reportFailedEvent(
		String componentId,
		SnippetEvent event,
		EvaluateResponse.Builder response
	) {
		shell.diagnostics(event.snippet()).forEach(diagnostic ->
			response.addError(
				EvaluationError.newBuilder()
					.setComponentId(componentId)
					.setKind(diagnostic.getCode())
					.setMessage(diagnostic.getMessage(Locale.ENGLISH))
					.setSpan(
						CodeSpan.newBuilder()
							.setStart(diagnostic.getStartPosition())
							.setEnd(diagnostic.getEndPosition())
							.build()
					).build()
			)
		);
	}

	private void scheduleFlush() {
		flushTask.set(
			scheduler.scheduleWithFixedDelay(
				this::flushMessages,
				flushInterval.toMillis(),
				flushInterval.toMillis(),
				TimeUnit.MILLISECONDS
			)
		);
	}

	private void flushMessages() {
		var id = currentComponentId.get();
		if (id == null) {
			return;
		}
		try {
			messageSinks.flush(id);
		} catch (Exception failure) {
			log.atWarning()
				.withCause(failure)
				.log("failed to flush messages of component %s", id);
		}
	}

	private void cancelFlush() {
		var task = flushTask.getAndSet(null);
		if (task != null) {
			task.cancel(/* mayInterrupt */ false);
		}
	}

	private JShell createShell() {
    var engine = environment.control(UUID.randomUUID().toString());
		return JShell.builder()
			.out(createOutputStream())
			.err(createErrorStream())
			.executionEngine(engine, Map.of())
			.build();
	}

	private PrintStream createOutputStream() {
		return CapturingOutput.to(messageSinks::writeOutput);
	}

	private PrintStream createErrorStream() {
		return CapturingOutput.to(messageSinks::writeError);
	}

	@Override
	public void stop() {
		cleanUp();
	}

	private void cleanUp() {
		cancelFlush();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("shell", shell)
			.add("sources", sources)
			.add("environment", environment)
			.add("stage", stage)
			.toString();
	}
}
