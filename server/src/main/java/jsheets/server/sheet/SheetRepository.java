package jsheets.server.sheet;

import jsheets.Sheet;

import java.util.Optional;
import java.util.UUID;

public interface SheetRepository {
	Optional<Sheet> findById(UUID id);
	void create(Sheet sheet);
	void update(Sheet sheet);
	void deleteById(UUID id);
}