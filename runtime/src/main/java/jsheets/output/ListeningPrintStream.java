package jsheets.output;

import java.io.IOException;
import java.io.PrintStream;

import javax.annotation.Nonnull;

public abstract class ListeningPrintStream extends PrintStream {
  private final PrintStream target;

  protected ListeningPrintStream(PrintStream target) {
    super(target);
    this.target = target;
  }

  protected abstract boolean capture(String written);

  protected boolean captureLine(String line) {
    return capture(line + "\n");
  }

  @Override
  public void print(String string) {
    if (capture(string)) {
      target.print(string);
    }
  }

  @Override
  public void print(int number) {
    if (capture(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void print(char character) {
    if (capture(String.valueOf(character))) {
      target.print(character);
    }
  }

  @Override
  public void print(long number) {
    if (capture(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void print(float number) {
    if (capture(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void print(@Nonnull char[] characters) {
    if (capture(String.valueOf(characters))) {
      target.print(characters);
    }
  }

  @Override
  public void print(double number) {
    if (capture(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void print(boolean flag) {
    if (capture(String.valueOf(flag))) {
      target.print(flag);
    }
  }

  @Override
  public void print(Object object) {
    var stringRepresentation = object == null ? "null" : object.toString();
    if (capture(stringRepresentation)) {
      target.print(stringRepresentation);
    }
  }

  @Override
  public void println(String string) {
    if (captureLine(string)) {
      target.print(string);
    }
  }

  @Override
  public void println(int number) {
    if (captureLine(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void println(char character) {
    if (captureLine(String.valueOf(character))) {
      target.print(character);
    }
  }

  @Override
  public void println(long number) {
    if (captureLine(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void println(float number) {
    if (captureLine(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void println(@Nonnull char[] characters) {
    if (capture(String.valueOf(characters))) {
      target.print(characters);
    }
  }

  @Override
  public void println(double number) {
    if (captureLine(String.valueOf(number))) {
      target.print(number);
    }
  }

  @Override
  public void println(boolean flag) {
    if (captureLine(String.valueOf(flag))) {
      target.print(flag);
    }
  }

  @Override
  public void println(Object object) {
    var stringRepresentation = object == null ? "null" : object.toString();
    if (captureLine(stringRepresentation)) {
      target.print(stringRepresentation);
    }
  }

  @Override
  public void write(int codePoint) {
    if (capture(String.valueOf((char) codePoint))) {
      target.write(codePoint);
    }
  }

  @Override
  public void write(byte[] bytes) throws IOException {
    if (capture(new String(bytes))) {
      target.write(bytes);
    }
  }

  @Override
  public void writeBytes(byte[] bytes) {
    if (capture(new String(bytes))) {
      target.writeBytes(bytes);
    }
  }

  @Override
  public void write(byte[] buffer, int offset, int length) {
    if (capture(new String(buffer, offset, length))) {
      target.write(buffer, offset, length);
    }
  }
}