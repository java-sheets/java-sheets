package jsheets.server.sheet;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.flogger.FluentLogger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import jsheets.Sheet;
import org.bson.Document;
import org.bson.conversions.Bson;


public final class DocumentSheetRepository implements SheetRepository {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final MongoCollection<Document> documents;

  @Inject
  DocumentSheetRepository(@Named("sheets") MongoCollection<Document> documents) {
    this.documents = documents;
  }

  @Override
  public Optional<Sheet> findById(UUID id) {
    var found = documents.find(whereIdIs(id.toString()))
      .limit(1)
      .map(this::decodeDocument)
      .first();
    return Objects.requireNonNullElseGet(found, Optional::empty);
  }

  private static Bson whereIdIs(String id) {
    return Filters.eq("_id", id);
  }

  private static final JsonFormat.Parser documentParser =
    JsonFormat.parser().ignoringUnknownFields();

  @Nonnull
  private Optional<Sheet> decodeDocument(Document document) {
    var sheet = Sheet.newBuilder();
    try {
      documentParser.merge(document.toJson(), sheet);
    } catch (InvalidProtocolBufferException malformedDocument) {
      reportMalformedDocument(document, malformedDocument);
      return Optional.empty();
    }
    return Optional.of(sheet.build());
  }

  private void reportMalformedDocument(
    Document document,
    Exception details
  ) {
    var id = Objects.requireNonNullElse(document.getString("_id"), "Unknown");
    log.atWarning()
      .withCause(details)
      .with(SheetMetadataKeys.idKey(), id)
      .log("ignored found malformed document");
  }

  @Override
  public void update(Sheet sheet) {
    var document = encodeDocument(sheet);
    try {
      long modified = documents.updateOne(whereIdIs(sheet.getId()), document)
        .getModifiedCount();
      if (modified < 1) {
        throw new NoSuchElementException(
          "update failed: no sheet with id %s was found".formatted(sheet.getId())
        );
      }
    } catch (Exception failedInsertion) {
      throw new RuntimeException("failed to update sheet", failedInsertion);
    }
  }

  @Override
  public void create(Sheet sheet) {
    var document = encodeDocument(sheet);
    try {
      documents.insertOne(document);
    } catch (Exception failedInsertion) {
      throw new RuntimeException("failed to create sheet", failedInsertion);
    }
  }

  private static final JsonFormat.Printer documentPrinter =
    JsonFormat.printer()
      .includingDefaultValueFields()
      .preservingProtoFieldNames()
      .omittingInsignificantWhitespace();

  private Document encodeDocument(Sheet sheet) {
    try {
      var raw = documentPrinter.print(sheet);
      var document = Document.parse(raw);
      document.put("_id", sheet.getId());
      return document;
    } catch (InvalidProtocolBufferException invalidDocument) {
      throw new IllegalArgumentException(
        "can not encode message as json",
        invalidDocument
      );
    }
  }

  @Override
  public void deleteById(UUID id) {
    long deleted = documents.deleteOne(whereIdIs(id.toString()))
      .getDeletedCount();
    if (deleted < 1) {
      throw new NoSuchElementException(
        "delete failed: no sheet with id %s was found".formatted(id)
      );
    }
  }
}
