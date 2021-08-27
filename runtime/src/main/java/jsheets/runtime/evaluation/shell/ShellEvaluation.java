package jsheets.runtime.evaluation.shell;

import com.google.common.base.MoreObjects;
import jdk.jshell.JShell;
import jsheets.StartEvaluationRequest;
import jsheets.runtime.evaluation.Evaluation;
import jsheets.runtime.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.source.SharedSources;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

final class ShellEvaluation implements Evaluation {
	private enum Stage { Initial, Starting, Evaluating, Terminated }

	private volatile JShell shell;

	private final SharedSources sources;
	private final ExecutionEnvironment environment;
	private final AtomicReference<Stage> stage =
		new AtomicReference<>(Stage.Initial);

	ShellEvaluation(Clock clock, ExecutionEnvironment environment) {
		this.environment = environment;
		sources = SharedSources.createEmpty(clock);
	}

	public void start(StartEvaluationRequest request) {
		shell = JShell.builder()
			.out(createOutputStream())
			.err(createErrorStream())
			.build();
	}

	private PrintStream createOutputStream() {
		return new PrintStream(OutputStream.nullOutputStream());
	}

	private PrintStream createErrorStream() {
		return new PrintStream(OutputStream.nullOutputStream());
	}

	@Override
	public void stop() {}

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
