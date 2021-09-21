package jsheets.runtime.evaluation.shell;

import java.io.OutputStream;
import java.util.function.Consumer;

public class CapturingOutput extends ListeningPrintStream {
  public static CapturingOutput to(Consumer<String> receiver) {
    return new CapturingOutput(receiver);
  }

  private final Consumer<String> receiver;

  public CapturingOutput(Consumer<String> receiver) {
    super(OutputStream.nullOutputStream());
    this.receiver = receiver;
  }

  @Override
  protected boolean capture(String written) {
    receiver.accept(written);
    return true;
  }
}
