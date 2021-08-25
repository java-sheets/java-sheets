package jsheets.server;

import com.google.common.flogger.FluentLogger;

public final class App {
  private App() {}

  public static void main(String[] arguments) {
    FluentLogger.forEnclosingClass().atInfo().log("Hello, World!");
  }
}
