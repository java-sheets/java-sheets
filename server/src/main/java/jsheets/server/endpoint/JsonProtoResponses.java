package jsheets.server.endpoint;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.javalin.http.Context;

public final class JsonProtoResponses {
	private static final FluentLogger log = FluentLogger.forEnclosingClass();

	private JsonProtoResponses() {}

	private static final String jsonContentType = "application/json";

	private static final JsonFormat.Printer prettyPrinter =
		JsonFormat.printer()
			.sortingMapKeys()
			.preservingProtoFieldNames();

	public static void respond(Context call, Message message) {
		call.contentType(jsonContentType).result(prettyPrint(message));
	}

	private static String prettyPrint(Message message) {
		try {
			return prettyPrinter.print(message);
		} catch (InvalidProtocolBufferException failedPrint) {
			log.atWarning().withCause(failedPrint).log("failed to print message as json");
			throw new RuntimeException("failed to print message", failedPrint);
		}
	}
}
