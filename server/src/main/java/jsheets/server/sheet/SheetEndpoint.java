package jsheets.server.sheet;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.Message;

import io.javalin.Javalin;
import io.javalin.http.Context;

import org.eclipse.jetty.http.HttpStatus;

import jsheets.Sheet;
import jsheets.server.endpoint.Endpoint;
import jsheets.server.endpoint.ErrorResponse;
import jsheets.server.endpoint.JsonProtoRequests;
import jsheets.server.endpoint.JsonProtoResponses;

public final class SheetEndpoint implements Endpoint {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final SheetService service;

  @Inject
  SheetEndpoint(SheetService service) {
    this.service = service;
  }

  @Override
  public void configure(Javalin server) {
    server.get("/api/v1/sheets/{id}", this::findById);
    server.put("/api/v1/sheets/", this::update);
    server.post("/api/v1/sheets/", this::post);
  }

  private void findById(Context call) {
    var rawId = call.pathParam("id");
    var id = parseUniqueIdOrNull(rawId);
    if (id == null) {
      illegalIdResponse(rawId).respond(call);
      return;
    }
    findByValidId(call, id);
  }

  private void findByValidId(Context call, UUID id) {
    try {
      service.findById(id).ifPresentOrElse(
        /* present */ sheet -> JsonProtoResponses.respond(call, sheet),
        /* not found */  () -> notFoundResponse().respond(call)
      );
    } catch (Exception failedFind) {
      log.atWarning()
        .with(SheetMetadataKeys.idKey(), id.toString())
        .withCause(failedFind)
        .log("exception occurred while looking up sheet");
    }
  }

  private ErrorResponse<ErrorResponse.IllegalField[]> illegalIdResponse(String id) {
    return new ErrorResponse<>(
      HttpStatus.BAD_REQUEST_400,
      "the request contains invalid parameters",
      new ErrorResponse.IllegalField[] {
        new ErrorResponse.IllegalField("param.id", "%s is not a valid UUID".formatted(id))
      }
    );
  }

  private ErrorResponse<?> notFoundResponse() {
    return new ErrorResponse<>(
      HttpStatus.NOT_FOUND_404,
      "could not find the requested sheet",
      new Object[0]
    );
  }

  @Nullable
  private UUID parseUniqueIdOrNull(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private void post(Context call) {
    requireBody(Sheet.Form.class, call).ifPresent(form -> {
      var created = service.create(form);
      JsonProtoResponses.respond(call, created);
    });
  }

  private void update(Context call) {
    requireBody(Sheet.class, call).ifPresent(target -> {
      var updated = service.update(target);
      JsonProtoResponses.respond(call, updated);
    });
  }

  private <T extends Message> Optional<T> requireBody(Class<T> type, Context call) {
    try {
      return Optional.of(JsonProtoRequests.parse(type, call.body()));
    } catch (IllegalArgumentException illegalInput) {
      illegalJsonInput(illegalInput.getMessage()).respond(call);
      return Optional.empty();
    }
  }

  private ErrorResponse<ErrorResponse.IllegalField[]> illegalJsonInput(String message) {
    return new ErrorResponse<>(
      HttpStatus.BAD_REQUEST_400,
      "the request contains an invalid body",
      new ErrorResponse.IllegalField[] {
        new ErrorResponse.IllegalField("body", message)
      }
    );
  }

  @Override
  public String toString() {
    return "SheetEndpoint(sheets=%s)".formatted(service);
  }
}
