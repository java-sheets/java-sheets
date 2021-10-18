package jsheets.event;

import java.util.function.Supplier;

public interface EventSink {
  void post(Object event);

  /**
   * Only creates the event to post if the sink is enabled.
   * <p>
   * This method is preferred if creating an event is associated with some
   * overhead. It only runs the {@code eventFactory}, if there is a chance that
   * it is subscribed.
   *
   * @param eventFactory Lazily creates the event
   */
  default void postIfEnabled(Supplier<Object> eventFactory) {
    post(eventFactory.get());
  }

  static EventSink ignore() {
    return event -> {};
  }
}
