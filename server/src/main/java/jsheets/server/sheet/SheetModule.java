package jsheets.server.sheet;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jsheets.server.IdPool;
import org.bson.Document;

import javax.inject.Named;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class SheetModule extends AbstractModule {
	public static SheetModule create() {
		return new SheetModule();
	}

	private SheetModule() { }

	@Override
	protected void configure() {
		bind(Clock.class).toProvider(Clock::systemUTC);
		bind(SheetRepository.class)
			.to(DocumentSheetRepository.class)
			.in(Singleton.class);
	}

	@Provides
	@Singleton
	@Named("sheets")
	IdPool idPool() {
		return UUID::randomUUID;
	}

	@Provides
	@Singleton
	MongoDatabase mongoDatabase() {
		var value = System.getenv("JSHEETS_MONGODB_URI");
		return value == null
			 ? createDefaultDatabase()
			: createDatabaseFromConnectionString(new ConnectionString(value));
	}

	private static final String defaultDatabaseName = "jsheets";

	private MongoDatabase createDefaultDatabase() {
		var client = MongoClients.create();
		return client.getDatabase(defaultDatabaseName);
	}

	private MongoDatabase createDatabaseFromConnectionString(ConnectionString string) {
		var client = MongoClients.create(string);
		var databaseName = Objects.requireNonNullElse(string.getDatabase(), defaultDatabaseName);
		return client.getDatabase(databaseName);
	}

	@Provides
	@Singleton
	@Named("sheets")
	MongoCollection<Document> sheetsCollection(MongoDatabase database) {
		return database.getCollection("sheets");
	}
}
