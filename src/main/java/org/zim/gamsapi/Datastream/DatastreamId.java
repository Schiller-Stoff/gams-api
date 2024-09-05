package org.zim.gamsapi.Datastream;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.codec.Hex;
import org.zim.gamsapi.Datastream.exceptions.DatastreamIdHashingException;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Slf4j
public class DatastreamId implements Serializable {

  /**
   * "Name" of the datastream like "TEI_SOURCE"
   */
  @NotEmpty
  private String dsid;

  /**
   * Digital object identifier, like "o:derla.1234
   */
  @NotEmpty
  private String digitalObject;

  public String toString(){
    return String.format("%s_%s", digitalObject, dsid);
  }

  /**
   * Transforms the toString representation of the datastream-id into a sha256 hash --> and returns that as hex string
   * https://www.baeldung.com/sha-256-hashing-java
   * TODO write tests for this (must create expected value!)
   * @return sha256 hash of the datastream-id as hex value.
   */
  public String calcSha256Hex(){

    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA3-256");
    } catch (NoSuchAlgorithmException e) {
      String msg = String.format("Could not create SHA3-256 digest for datastream-id: %s Original error: %s", this, e);
      log.error(msg);
      throw new DatastreamIdHashingException(msg);
    }

    final byte[] hashbytes = digest.digest(
        this.toString().getBytes(StandardCharsets.UTF_8));

    char[] hex = Hex.encode(hashbytes);
    return String.valueOf(hex);
  }

}
