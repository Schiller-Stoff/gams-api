package org.zim.gamsapi.Datastream;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
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

}
