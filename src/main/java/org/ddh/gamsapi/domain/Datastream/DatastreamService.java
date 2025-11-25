package org.ddh.gamsapi.domain.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.*;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.ddh.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentDeletionFailure;
import org.ddh.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentDeletionFailureRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.*;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNoMainResourceDatastreamDefinedException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatastreamService implements IDatastreamService {

  private final IDatastreamRepository datastreamRepository;

  private final IDigitalObjectRepository digitalObjectRepository;

  private final IDatastreamContentRepository datastreamContentRepository;

  private final DatastreamContentDeletionFailureRepository datastreamContentDeletionFailureRepository;

  @Override
  @Transactional
  public void delete(Datastream datastream) {

    if(datastream.getDigitalObject() == null){
      String msg = String.format("Datastream's digital object is unexpectedly null %s . Cannot delete datastream.", datastream);
      log.error(msg);
      // TODO this exception here is completely wrong - is more about client input validation. Need to define a new exception type?
      throw new DigitalObjectNotFoundException(msg);
    }

    if(!datastreamRepository.existsById(datastream.deriveDatastreamId())){
      throw new DatastreamNotFoundException(
          "Cannot delete datastream. Datastream not found: " + datastream
      );
    }

    datastreamRepository.delete(datastream);

    try {
      datastreamContentRepository.delete(
          datastream.deriveDatastreamId()
      );
    } catch (DatastreamCannotDeleteFileException e){
      log.error("Failed to delete file content for datastream {} object: {}. Recording deletion failure for cleanup job. Reason: {}",
          datastream.getDsid(),
          datastream.getDigitalObject().getId(),
          e.getMessage(), e);

      datastreamContentDeletionFailureRepository.save(
          DatastreamContentDeletionFailure.builder()
              .digitalObjectId(datastream.getDigitalObject().getId())
              .datastreamDsid(datastream.getDsid())
              .build()
      );
    }

    log.debug("Successfully deleted datastream {} from object {}",
        datastream.getDsid(),
        datastream.getDigitalObject().getId());

  }



  @Override
  @Transactional(readOnly = true)
  public Datastream findById(DatastreamId id) {
    return datastreamRepository.findById(id).orElseThrow(() -> new DatastreamNotFoundException(
        "Cannot find datastream. Datastream does not exist: " + id
    ));

  }

  @Override
  @Transactional
  public Datastream save(Datastream datastream, MultipartFile file) {

    // Validate digital object exists
    String digitalObjectId = datastream.getDigitalObject().getId();
    if (!digitalObjectRepository.existsById(digitalObjectId)) {
      throw new DigitalObjectNotFoundException(
          "Digital object with id " + digitalObjectId + " not found"
      );
    }

    DatastreamId dsId = datastream.deriveDatastreamId();

    // Stream file content directly to disk - NO byte[] intermediate!
    try (InputStream inputStream = file.getInputStream()) {
      datastreamContentRepository.save(inputStream, dsId);
    } catch (IOException e) {
      log.error("Failed to save file content for datastream {}: {}",
          dsId, e.getMessage(), e);
      throw new DatastreamCannotWriteFileException(
          "Failed to save file content for datastream " + dsId
      );
    }

    // Save entity metadata
    Datastream savedDatastream = datastreamRepository.save(datastream);

    log.debug("Successfully saved datastream {} with file content", savedDatastream);
    return savedDatastream;

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
    return datastreamRepository.findDatastreamDetailsViewByDigitalObject_IdAndDsid(
        datastreamId.getDigitalObject(), datastreamId.getDsid()).orElseThrow(
            () -> new DatastreamNotFoundException(
        "Cannot find datastream details view. For: " + datastreamId
    ));
  }

  @Override
  public IDatastreamDetailsView findSingularDatastreamDetailsViewByObjectIdAndTags(String digitalObjectId, Set<String> tags) {

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot find datastream via tags. Digital object with id does not exist: " + digitalObjectId
      );
    }

    var foundDatastreams = datastreamRepository.findDatastreamsPaginatedByDigitalObject_IdAndTagsIn(
        digitalObjectId, tags,
        tags.size(),
        PageRequest.of(0, 1));

    if (foundDatastreams.isEmpty()) {
      throw new DatastreamNotFoundException(
          "Cannot find datastream via tags. No datastreams found for object: " + digitalObjectId + " with tags: " + tags
      );
    }

    // method allows only to match a single datastream
    if (foundDatastreams.getTotalElements() > 1) {
      throw new DatastreamAmbiguousMatchException(
          "Multiple datastreams found for digital object " + digitalObjectId + ". For tags: " + tags
      );
    }

    // return the first datastream found
    return foundDatastreams.getContent().get(0);
  }


  @Override
  public IDatastreamDetailsView findMainDatastreamByDigitalObjectId(String digitalObjectId) {

    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId).orElseThrow(() -> {
      return new DigitalObjectNotFoundException(
          "Cannot find main resource datastream - Parent digital object not found: " + digitalObjectId
      );
    });

    // check if mainResource is set
    if (digitalObject.getMainResource() == null || digitalObject.getMainResource().isEmpty()) {
      throw new DigitalObjectNoMainResourceDatastreamDefinedException(
          "Cannot find main resource datastream - No mainResource defined for digital object " + digitalObjectId
      );
    }

    return datastreamRepository.findDatastreamByDigitalObject_IdAndDsid(
        digitalObjectId,
        digitalObject.getMainResource()
    ).orElseThrow(() -> new DatastreamNotFoundException(
        "Cannot find main resource datastream: Expected datastream does not exist. Digital object: " + digitalObjectId + " mainResource: " + digitalObject.getMainResource()
    ));

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

  @Override
  public PagedResponse<String> findAllIds(String digitalObjectId, Pageable pageable) throws DigitalObjectNotFoundException {

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      throw new DigitalObjectNotFoundException(
          "Cannot find datastream ids. Digital object does not exist: " + digitalObjectId
      );
    }

    var foundDatastreamViews = datastreamRepository.findAllDatastreamIdViewsByDigitalObjectId(
        digitalObjectId,
        pageable
    );

    if (foundDatastreamViews.isEmpty()) {
      log.warn("No datastreams found for digital object with id {} (There should be at least one datastream)", digitalObjectId);
    }

    return PagedResponse.from(
        foundDatastreamViews.map(IDatastreamIdView::getDsid)
    );

  }
}
