package org.zim.gamsapi.Datastream.interfaces;

import org.springframework.core.io.FileSystemResource;

/**
 * Service interface for datastream content.
 */
public interface IDatastreamContentService {

  FileSystemResource loadFile(String fileName);

}
