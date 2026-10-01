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
   * Checks if the prefix of given handle is managed by the gams-api
   * @param handle given handle
   * @return boolean if handle is managed or not
   */
  public boolean isManagedHandle(Handle handle){
    return handle.prefix().equals(properties.getPrefix());
  }

  /**
   * Checks if given pid is a valid handle
   * @param pid permanent identifier of a resource
   * @return boolean if given string is a valid handle
   */
  public boolean isHandle(String pid){
    return Handle.isValid(pid);
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
