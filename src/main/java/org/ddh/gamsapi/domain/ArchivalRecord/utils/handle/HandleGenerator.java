package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class HandleGenerator {

  private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstvwxyz".toCharArray();
  private final HandleServerProperties properties;
  private final SecureRandom random = new SecureRandom();  // thread-safe

  public HandleGenerator(HandleServerProperties properties) {
    this.properties = properties;
  }

  public Handle generate() {
    var suffix = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      suffix.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return new Handle(properties.getPrefix(), suffix.toString());
  }
}
