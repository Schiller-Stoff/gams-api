package org.ddh.gamsapi;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Test component that captures all application events for verification in tests.
 */
@Component
public class EventCaptureListener {

  private final List<ApplicationEvent> capturedEvents = new ArrayList<>();

  /**
   * Captures all application events
   */
  @EventListener
  public void handleApplicationEvent(ApplicationEvent event) {
    capturedEvents.add(event);
  }

  /**
   * Checks if an event of the specified type has been captured
   */
  public boolean hasEventOfType(Class<? extends ApplicationEvent> eventType) {
    return capturedEvents.stream()
        .anyMatch(eventType::isInstance);
  }

  /**
   * Gets the first captured event of the specified type
   */
  @SuppressWarnings("unchecked")
  public <T extends ApplicationEvent> T getEventOfType(Class<T> eventType) {
    return (T) capturedEvents.stream()
        .filter(eventType::isInstance)
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets count of events of a specific type
   */
  public long countEventsOfType(Class<? extends ApplicationEvent> eventType) {
    return capturedEvents.stream()
        .filter(eventType::isInstance)
        .count();
  }

  /**
   * Clears all captured events
   */
  public void clearEvents() {
    capturedEvents.clear();
  }
}
