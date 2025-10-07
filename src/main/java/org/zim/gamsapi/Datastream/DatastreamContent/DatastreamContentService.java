package org.zim.gamsapi.Datastream.DatastreamContent;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentService;

@Service
@RequiredArgsConstructor
public class DatastreamContentService implements IDatastreamContentService {

  private final DatastreamContentRepository datastreamContentRepository;


  public InputStreamResource load(DatastreamId datastreamId) {
    return datastreamContentRepository.findById(datastreamId);
  }



}
