package jsheets.server;

import java.util.UUID;

public interface IdPool {
	UUID take();
}
