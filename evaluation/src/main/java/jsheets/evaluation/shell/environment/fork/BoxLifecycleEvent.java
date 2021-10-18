package jsheets.evaluation.shell.environment.fork;

import java.util.Map;

import io.soabase.recordbuilder.core.RecordBuilder;
import jsheets.event.LabeledEvent;

@RecordBuilder
public record BoxLifecycleEvent(
  long processId,
  Stage stage,
  Map<String, Object> labels
) implements LabeledEvent {

  public enum Stage { Starting, Running, Stopping }
}
