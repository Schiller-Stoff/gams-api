package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zim.gamsapi.Datastream.exceptions.*;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNoMainResourceDatastreamDefinedException;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.System.dto.PagedResponse;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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

    // THINK ABOUT: maybe should use input stream instead of byte array?
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

  @Override
  public IDatastreamDetailsView findSingularDatastreamDetailsViewByObjectIdAndTags(String digitalObjectId, Set<String> tags) {

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      String msg = String.format("Digital object with id %s does not exist. Cannot find datastream via tags.", digitalObjectId);
      log.error(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    var foundDatastreams = datastreamRepository.findDatastreamsPaginatedByDigitalObject_IdAndTagsIn(
        digitalObjectId, tags,
        tags.size(),
        PageRequest.of(0, 1));

    if (foundDatastreams.isEmpty()) {
      String msg = String.format("No datastream(s) found for digital object %s having the tags: %s", digitalObjectId, tags);
      log.error(msg);
      throw new DatastreamNotFoundException(msg);
    }
    // method allows only to match a single datastream
    if (foundDatastreams.getTotalElements() > 1) {
      // concatenate all dsids (were given tags matched)
      String matchedDsids = foundDatastreams.stream()
          .map(IDatastreamDetailsView::getDsid)
          .reduce("", (a, b) -> a + ", " + b);

      String msg = String.format("Multiple datastreams found for digital object %s having the tags: %s. Matched datastreams are: %s .", digitalObjectId, tags, matchedDsids);
      log.error(msg);
      throw new DatastreamAmbiguousMatchException(msg);
    }

    // return the first datastream found
    return foundDatastreams.getContent().get(0);
  }


  @Override
  public IDatastreamDetailsView findMainDatastreamByDigitalObjectId(String digitalObjectId) {

    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId).orElseThrow(() -> {
      String msg = String.format("Cannot find digital object with id %s", digitalObjectId);
      log.info(msg);
      return new DigitalObjectNotFoundException(msg);
    });

    // check if mainResource is set
    if (digitalObject.getMainResource() == null || digitalObject.getMainResource().isEmpty()) {
      String msg = String.format("Digital object %s has no mainResource datastream defined. mainResource Property is null or empty.", digitalObject);
      log.warn(msg);
      throw new DigitalObjectNoMainResourceDatastreamDefinedException(msg);
    }

    return datastreamRepository.findDatastreamByDigitalObject_IdAndDsid(
        digitalObjectId,
        digitalObject.getMainResource()
    ).orElseThrow(() -> {
      String msg = String.format("Cannot find mainResource datastream for digital object %s for mainResource %s", digitalObjectId, digitalObject.getMainResource());
      log.info(msg);
      return new DatastreamNotFoundException(msg);
    });

  }

  @Override
  public PagedResponse<IDatastreamDetailsView> findAll(String digitalObjectId, Pageable pageable) {
    return PagedResponse.from(datastreamRepository.findAllByDigitalObjectId(digitalObjectId, pageable));
  }

  @Override
  public PagedResponse<IDatastreamDetailsView> findAll(String digitalObjectId, Set<String> tags, Pageable pageable) {
    return PagedResponse.from(
        datastreamRepository.findDatastreamsPaginatedByDigitalObject_IdAndTagsIn(digitalObjectId, tags, tags.size(), pageable)
    );
  }
}
