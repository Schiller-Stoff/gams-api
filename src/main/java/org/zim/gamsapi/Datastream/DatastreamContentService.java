package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentService;

@Service
@RequiredArgsConstructor
public class DatastreamContentService implements IDatastreamContentService {

  private final FileSystemRepository fileSystemRepository;


  public FileSystemResource loadFile(String fileName) {
    return fileSystemRepository.load(fileName);
  }



}
