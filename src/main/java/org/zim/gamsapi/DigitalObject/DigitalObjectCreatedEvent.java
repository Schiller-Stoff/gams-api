package org.zim.gamsapi.DigitalObject;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring event that is published when a new DigitalObject is created.
 */
@Getter
public class DigitalObjectCreatedEvent extends ApplicationEvent {

  private final DigitalObject digitalObject;

  public DigitalObjectCreatedEvent(Object source, DigitalObject digitalObject) {
    super(source);
    this.digitalObject = digitalObject;
  }

}
