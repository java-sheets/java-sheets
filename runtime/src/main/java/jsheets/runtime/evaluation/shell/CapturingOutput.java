package jsheets.runtime.evaluation.shell;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

public class CapturingOutput extends PrintStream {
	private final Consumer<String> receiver;

	public CapturingOutput(Consumer<String> receiver) {
		super(OutputStream.nullOutputStream());
		this.receiver = receiver;
	}

}
