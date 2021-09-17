package jsheets.server.sheet;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.Timestamp;

import jsheets.Sheet;
import jsheets.server.IdPool;

public final class SheetService {
	private static final FluentLogger log = FluentLogger.forEnclosingClass();

	private final IdPool ids;
	private final Clock clock;
	private final SheetRepository repository;

	@Inject
	SheetService(@Named("sheets") IdPool ids, Clock clock, SheetRepository repository) {
		this.ids = ids;
		this.clock = clock;
		this.repository = repository;
	}

	public Optional<Sheet> findById(UUID id) {
		Objects.requireNonNull(id, "id");
		return repository.findById(id);
	}

	public Sheet create(Sheet.Form form) {
		var sheet = createFromForm(form);
		repository.create(sheet);
		log.atFiner()
			.with(SheetMetadataKeys.idKey(), sheet.getId())
			.log("created sheet");
		return sheet;
	}

	private Sheet createFromForm(Sheet.Form form) {
		return Sheet.newBuilder()
			.setId(ids.take().toString())
			.setTitle(form.getTitle())
			.setDescription(form.getDescription())
			.setTemplateId(form.getTemplateId())
			.setAuthor(form.getAuthor())
			.setMetadata(createMetadata())
			.addAllSnippets(form.getSnippetsList())
			.build();
	}

	private Sheet.Metadata createMetadata() {
		var time = currentTime();
		return Sheet.Metadata.newBuilder()
			.setCreateTime(time)
			.setUpdateTime(time)
			.build();
	}

	private Timestamp currentTime() {
		return Timestamp.newBuilder()
			.setSeconds(clock.instant().getEpochSecond())
			.build();
	}

	public Sheet update(Sheet target) {
		var updated = updateMetadata(target);
		repository.update(target);
		log.atFiner()
			.with(SheetMetadataKeys.idKey(), target.getId())
			.log("updated sheet");
		return updated;
	}

	private Sheet updateMetadata(Sheet target) {
		return target.toBuilder()
			.setMetadata(
				target.getMetadata().toBuilder()
					.setUpdateTime(currentTime())
					.build()
			).build();
	}

	@Override
	public String toString() {
		return "SheetService(repository=%s)".formatted(repository);
	}
}