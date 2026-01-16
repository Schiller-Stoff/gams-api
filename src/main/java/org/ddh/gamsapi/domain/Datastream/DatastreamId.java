package org.ddh.gamsapi.domain.Datastream;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

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
    return digitalObject + "_" + dsid;
  }


}
