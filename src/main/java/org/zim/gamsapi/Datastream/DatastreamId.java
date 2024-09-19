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


}
