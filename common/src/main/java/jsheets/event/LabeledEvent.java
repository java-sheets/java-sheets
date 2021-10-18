package jsheets.event;

import java.util.Map;

public interface LabeledEvent {
  Map<String, Object> labels();
}
