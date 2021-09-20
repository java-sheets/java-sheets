package jsheets.server.endpoint;

import io.javalin.http.Context;

public record ErrorResponse<Details>(
  int code,
  String message,
  Details details
) {
  public record IllegalField(String field, String reason) { }

  public void respond(Context context) {
    context.status(code).json(this);
  }
}
