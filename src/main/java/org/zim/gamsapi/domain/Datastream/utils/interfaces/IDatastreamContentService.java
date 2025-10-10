package org.zim.gamsapi.domain.Datastream.utils.interfaces;

import org.springframework.core.io.InputStreamResource;
import org.zim.gamsapi.domain.Datastream.DatastreamId;

/**
 * Service interface for datastream content.
 */
public interface IDatastreamContentService {

  /**
   * Retrieves the datastream content as streamable resource.
   * @param datastreamId the datastream id
   * @return the datastream content as streamable resource
   */
  InputStreamResource load(DatastreamId datastreamId);

}
