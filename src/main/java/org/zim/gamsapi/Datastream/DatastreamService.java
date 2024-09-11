package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotWriteFileException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamException;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatastreamService implements IDatastreamService {

  private final IDatastreamRepository datastreamRepository;

  private final IDigitalObjectRepository digitalObjectRepository;

  private final IDatastreamContentRepository datastreamContentRepository;

  @Override
  @Transactional
  public void delete(Datastream datastream) {

    //TODO update test?

    if(datastream.getDigitalObject() == null){
      String msg = String.format("Datastream's digital object is unexpectedly null %s . Cannot delete datastream.", datastream);
      log.error(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    if(!datastreamRepository.existsById(datastream.deriveDatastreamId())){
      String msg = String.format("Datastream with id %s not found. Cannot delete datastream", datastream);
      log.error(msg);
      throw new DatastreamNotFoundException(msg);
    }

    datastreamRepository.delete(datastream);
    // TODO second delete file (orphaned files are not that serious )
    datastreamContentRepository.delete(
      datastream.deriveDatastreamId()
    );
  }



  @Override
  @Transactional
  public Datastream findById(DatastreamId id) throws DatastreamNotFoundException {
    return datastreamRepository.findById(id).orElseThrow(() -> {
      String msg = String.format("Cannot find datastream with id %s", id);
      log.info(msg);
      return new DatastreamNotFoundException(msg);
    });

  }

  @Override
  @Transactional
  public Datastream save(Datastream datastream, MultipartFile file) {

    // TODO maybe should use input stream instead of byte array?
    byte[] data;
    try {
      data = file.getBytes();
    } catch (IOException e) {
      String msg = String.format("Failed to extract data from given multipart-file for datastream %s from given file %s",datastream, file);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    if(digitalObjectRepository.existsById(datastream.getDigitalObject().getId())){
      String msg = String.format("Found digital object with id %s. Saving datastream %s", datastream.getDigitalObject().getId(), datastream);
      log.info(msg);
      datastreamContentRepository.save(data, datastream.deriveDatastreamId());
      return datastreamRepository.save(datastream);
    } else {
      String msg = String.format("Digital object with id %s does not exist. Cannnot save datastream %s", datastream.getDigitalObject().getId(), datastream);
      log.error(msg);
      throw new DigitalObjectNotFoundException(msg);
    }
  }

  /**
   * Returns a list of datastream projections based on the parent digital object.
   * The projection excludes the actual datastream content. (to improve performance)
   * @param digitalObject parent digital object
   * @return list of datastream projections
   */
  @Override
  @Transactional
  public List<IDatastreamDetailsView> findAll(DigitalObject digitalObject) {
    return datastreamRepository.findAllByDigitalObjectId(digitalObject.getId());
  }

  /**
   * Returns a datastream projection based on the parent digital object, and it's datastream-identifier.
   * The projection excludes the actual datastream content. (to improve performance
   *
   * @param datastreamId of the datastream
   * @return found Datastream projection
   * @throws DatastreamNotFoundException if no datastream is found
   */
  @Override
  @Transactional
  public IDatastreamDetailsView findDatastreamDetailsById(DatastreamId datastreamId) throws DatastreamNotFoundException {
    return datastreamRepository.findDatastreamDetailsViewByDigitalObject_IdAndDsid(datastreamId.getDigitalObject(), datastreamId.getDsid()).orElseThrow(() -> {
      String msg = String.format("Cannot find datastream-details-view via pid %s and dsid %s", datastreamId.getDigitalObject(), datastreamId.getDsid());
      log.info(msg);
      return new DatastreamNotFoundException(msg);
    });
  }
}
