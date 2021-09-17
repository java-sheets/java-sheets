package jsheets.server.endpoint;

import io.javalin.Javalin;

public interface Endpoint {
  void configure(Javalin server);
}
