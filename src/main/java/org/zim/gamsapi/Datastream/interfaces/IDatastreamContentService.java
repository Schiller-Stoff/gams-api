package org.zim.gamsapi.Datastream.interfaces;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.zim.gamsapi.Datastream.DatastreamId;

/**
 * Service interface for datastream content.
 */
public interface IDatastreamContentService {

  InputStreamResource loadFile(DatastreamId datastreamId);

}
