package jsheets.event;

import java.util.Objects;

import com.google.common.eventbus.EventBus;

public final class GuavaEventSink implements EventSink {
  public static EventSink forBus(EventBus bus) {
    Objects.requireNonNull(bus, "bus");
    return new GuavaEventSink(bus);
  }

  private final EventBus bus;

  private GuavaEventSink(EventBus bus) {
    this.bus = bus;
  }

  @Override
  public void post(Object event) {
    bus.post(event);
  }

  @Override
  public String toString() {
    return "GuavaEventSink(bus=%s)".formatted(bus);
  }
}