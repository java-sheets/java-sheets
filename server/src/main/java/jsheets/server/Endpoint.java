package jsheets.server;

import io.javalin.Javalin;

public interface Endpoint {
  void configure(Javalin server);
}
