package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class for generating handles.
 * Configured via application.yml
 */
@Component
public class HandleGenerator {

  private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
  private final HandleServerProperties properties;
  private static final int SUFFIX_LENGTH = 10;

  public HandleGenerator(HandleServerProperties properties) {
    this.properties = properties;
  }

  /**
   * Generates a handle with predefined prefix and random suffix.
   * @return Handle object with predefined prefix and random suffix.
   */
  public Handle generate() {
    var random = ThreadLocalRandom.current();
    var suffix = new StringBuilder(SUFFIX_LENGTH);
    for (int i = 0; i < SUFFIX_LENGTH; i++) {
      suffix.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return new Handle(properties.getPrefix(), suffix.toString());
  }
}
