package jsheets.runtime.evaluation.shell;

import java.io.OutputStream;
import java.io.PrintStream;

import javax.annotation.Nonnull;

public abstract class ListeningPrintStream extends PrintStream {
  protected ListeningPrintStream(OutputStream target) {
    super(target);
  }

  protected abstract boolean capture(String written);

  @Override
  public void print(String string) {
    if (capture(string)) {
      super.print(string);
    }
  }

  @Override
  public void print(int number) {
    if (capture(String.valueOf(number))) {
      super.print(number);
    }
  }

  @Override
  public void print(char character) {
    if (capture(String.valueOf(character))) {
      super.print(character);
    }
  }

  @Override
  public void print(long number) {
    if (capture(String.valueOf(number))) {
      super.print(number);
    }
  }

  @Override
  public void print(float number) {
    if (capture(String.valueOf(number))) {
      super.print(number);
    }
  }

  @Override
  public void print(@Nonnull char[] characters) {
    if (capture(String.valueOf(characters))) {
      super.print(characters);
    }
  }

  @Override
  public void print(double number) {
    if (capture(String.valueOf(number))) {
      super.print(number);
    }
  }

  @Override
  public void print(boolean flag) {
    if (capture(String.valueOf(flag))) {
      super.print(flag);
    }
  }

  @Override
  public void print(Object object) {
    var stringRepresentation = object == null ? "null" : object.toString();
    if (capture(stringRepresentation)) {
      super.print(stringRepresentation);
    }
  }

  @Override
  public PrintStream append(CharSequence csq) {
    return super.append(csq);
  }
}