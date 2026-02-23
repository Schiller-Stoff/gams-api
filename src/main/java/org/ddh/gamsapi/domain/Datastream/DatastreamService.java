package org.ddh.gamsapi.domain.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.DatastreamContent.WriteResult;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamCreateDto;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamUpdateDto;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.*;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.*;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectId;
import org.ddh.gamsapi.domain.DigitalObject.utils.events.DigitalObjectModifiedEvent;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserAuthenticationRequiredException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.ddh.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentDeletionFailure;
import org.ddh.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentDeletionFailureRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNoMainResourceDatastreamDefinedException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatastreamService implements IDatastreamService {

  private final IDatastreamRepository datastreamRepository;

  private final IDigitalObjectRepository digitalObjectRepository;

  private final IDatastreamContentRepository datastreamContentRepository;

  private final DatastreamContentDeletionFailureRepository datastreamContentDeletionFailureRepository;

  private final ApplicationEventPublisher applicationEventPublisher;

  private final IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @Override
  @Transactional
  public void delete(Datastream datastream) {

    if(datastream.getDigitalObject() == null){
      // TODO this exception here is wrong? - is more about client input validation. Need to define a new exception type?
      throw new DigitalObjectNotFoundException(
          "Datastream's digital object is unexpectedly null " + datastream + " . Cannot delete datastream."
      );
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

    String currentUser = userPrincipalAuditorMapping.getCurrentAuditor().orElseThrow(
        () -> new UserAuthenticationRequiredException("Failed to save datastream " + datastream + " Current user is not logged in - cannot retrieve username")
    );

    // publish event
    applicationEventPublisher.publishEvent(
        new DigitalObjectModifiedEvent(
            this,
            new DigitalObjectId(datastream.getDigitalObject().getId()),
            Instant.now(),
            currentUser
        )
    );

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

    // TODO check direct modification property + test

    String digitalObjectId = datastream.getDigitalObject().getId();

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      throw new DigitalObjectNotFoundException("Digital object with id " + digitalObjectId + " not found");
    }


    DatastreamId dsId = datastream.deriveDatastreamId();

    // Stream file content to disk WITH checksum computation
    try (InputStream inputStream = file.getInputStream()) {
      WriteResult result = datastreamContentRepository.saveWithChecksums(inputStream, dsId);

      // Set server-computed checksums
      datastream.setMd5Checksum(result.md5Checksum());
      datastream.setSha512Checksum(result.sha512Checksum());

    } catch (IOException e) {
      throw new DatastreamCannotWriteFileException(
          "Failed to save file content for datastream " + dsId +
              " At digital object: " + digitalObjectId +
              " Original error: " + e.getMessage(), e);
    }

    Datastream savedDatastream = datastreamRepository.save(datastream);

    String currentUser = userPrincipalAuditorMapping.getCurrentAuditor().orElseThrow(
        () -> new UserAuthenticationRequiredException("Failed to save datastream " + datastream + " Current user is not logged in")
    );

    // publish event
    applicationEventPublisher.publishEvent(
        new DigitalObjectModifiedEvent(
            this,
            new DigitalObjectId(dsId.getDigitalObject()),
            Instant.now(),
            currentUser
        )
    );


    log.info("Successfully saved datastream {} with file content", savedDatastream);
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

  @Override
  @Transactional
  public Datastream createFromUpload(String digitalObjectId, String dsid,
                                     DatastreamCreateDto dto, MultipartFile file) {

    // 1. Validate digital object exists
    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
        .orElseThrow(() -> new DigitalObjectNotFoundException(
            "Cannot create datastream. Digital object not found: " + digitalObjectId
        ));

    // 2. Validate dsid
    if (dsid == null || dsid.isBlank()) {
      throw new DatastreamValidationException(
          "Cannot create datastream for object: " + digitalObjectId + ". Datastream identifier (dsid) must not be empty"
      );
    }

    // 3. Validate file is present
    if (file == null || file.isEmpty()) {
      throw new DatastreamValidationException(
          "Cannot create datastream: " + dsid + " for object: " + digitalObjectId + ".  Sent file must not be empty"
      );
    }

    // 4. Check for duplicate — explicit check for clear 409 error
    DatastreamId datastreamId = new DatastreamId(dsid, digitalObjectId);
    if (datastreamRepository.existsById(datastreamId)) {
      throw new DatastreamAlreadyExistsException(
          "Datastream with dsid '" + dsid + "' already exists on digital object "
              + digitalObjectId
      );
    }

    // 5. Stream file to disk WITH checksum computation
    WriteResult writeResult;
    try (InputStream inputStream = file.getInputStream()) {
      writeResult = datastreamContentRepository.saveWithChecksums(inputStream, datastreamId);
    } catch (IOException e) {
      throw new DatastreamCannotWriteFileException(
          "Failed to save file content for datastream " + datastreamId
              + " at digital object: " + digitalObjectId
              + " Original error: " + e.getMessage(), e);
    }

    // 6. Build and persist datastream entity with server-computed values
    MetadataBaseEntity metadata = new MetadataBaseEntity();
    metadata.setTitle(dto.getTitle());
    metadata.setDescription(dto.getDescription());
    metadata.setCreator(dto.getCreator());
    metadata.setRights(dto.getRights());

    Datastream datastream = new DatastreamBuilder()
        .dsid(dsid)
        .digitalObject(digitalObject)
        .baseMetadata(metadata)
        .mimeType(resolveMimeType(file))
        .size(file.getSize())
        .bagPath(dsid)
        .md5Checksum(writeResult.md5Checksum())
        .sha512Checksum(writeResult.sha512Checksum())
        .tags(new HashSet<>())
        .lang(new HashSet<>())
        .build();

    Datastream savedDatastream = datastreamRepository.save(datastream);

    // 7. Publish modification event (updates parent object + project timestamps)
    String currentUser = userPrincipalAuditorMapping.getCurrentAuditor().orElseThrow(
        () -> new UserAuthenticationRequiredException(
            "Failed to create datastream " + dsid + ". Current user is not logged in"
        )
    );

    applicationEventPublisher.publishEvent(
        new DigitalObjectModifiedEvent(
            this,
            new DigitalObjectId(digitalObjectId),
            Instant.now(),
            currentUser
        )
    );

    log.info("Successfully created datastream {} via direct upload on object {}",
        dsid, digitalObjectId);
    return savedDatastream;
  }

  /**
   * Resolves MIME type for uploaded file.
   * Falls back to application/octet-stream if detection fails.
   */
  private String resolveMimeType(MultipartFile file) {
    // First: try the content type from the upload
    String contentType = file.getContentType();
    if (contentType != null && !contentType.isBlank()
        && !contentType.equals("application/octet-stream")) {
      return contentType;
    }

    // Second: try Java's built-in detection based on filename extension
    String filename = file.getOriginalFilename();
    if (filename != null) {
      try {
        String detected = java.nio.file.Files.probeContentType(
            java.nio.file.Path.of(filename)
        );
        if (detected != null) {
          return detected;
        }
      } catch (IOException e) {
        log.warn("MIME type probe failed for {}: {}", filename, e.getMessage());
      }
    }

    // Final fallback
    return "application/octet-stream";
  }


  /**
   * Updates an existing datastream's metadata. Only non-null fields from the patch DTO
   * are applied. The dsid and digitalObject (composite PK) cannot be changed.
   *
   * @param digitalObjectId the parent digital object ID
   * @param dsid the datastream identifier
   * @param patch DTO containing the fields to update
   * @return the updated datastream details projection
   * @throws DatastreamNotFoundException if the datastream does not exist
   * @throws DigitalObjectNotFoundException if the digital object does not exist
   * @throws DatastreamValidationException if the patch would violate invariants
   */
  @Override
  @Transactional
  public IDatastreamDetailsView updateDatastream(String digitalObjectId, String dsid,
                                                 DatastreamUpdateDto patch) {

    // 1. Validate digital object exists
    if (!digitalObjectRepository.existsById(digitalObjectId)) {
      throw new DigitalObjectNotFoundException(
          "Cannot update datastream. Digital object not found: " + digitalObjectId
      );
    }

    // 2. Find existing datastream
    DatastreamId datastreamId = new DatastreamId(dsid, digitalObjectId);
    Datastream existing = datastreamRepository.findById(datastreamId)
        .orElseThrow(() -> new DatastreamNotFoundException(
            "Cannot update datastream. Datastream not found: " + dsid
                + " on digital object: " + digitalObjectId
        ));

    // 3. Merge: only apply non-null fields from patch
    applyPatch(existing, patch);

    // 4. Validate invariants after merge
    validateInvariants(existing);

    // 5. Persist
    datastreamRepository.save(existing);

    // 6. Publish modification event (updates parent object + project timestamps)
    String currentUser = userPrincipalAuditorMapping.getCurrentAuditor()
        .orElseThrow(() -> new UserAuthenticationRequiredException(
            "Failed to update datastream " + dsid + ". Current user is not logged in"
        ));

    applicationEventPublisher.publishEvent(
        new DigitalObjectModifiedEvent(
            this,
            new DigitalObjectId(digitalObjectId),
            Instant.now(),
            currentUser
        )
    );

    log.info("Successfully updated datastream {} on object {}", dsid, digitalObjectId);
    return findDatastreamDetailsById(datastreamId);
  }

  /**
   * Updates the content of an existing datastream via file upload.
   * Recomputes checksums and file size, updates MIME type.
   * The dsid and digitalObject reference remain unchanged.
   *
   * @param digitalObjectId the parent digital object ID
   * @param dsid the datastream identifier
   * @param file the new file content
   * @return the updated datastream details projection
   * @throws DatastreamNotFoundException if the datastream does not exist
   * @throws DigitalObjectNotFoundException if the digital object does not exist
   * @throws DatastreamValidationException if the file is empty
   * @throws DatastreamCannotWriteFileException if file write fails
   */
  @Override
  @Transactional
  public IDatastreamDetailsView updateDatastreamContent(String digitalObjectId, String dsid,
                                                        MultipartFile file) {

    // 1. Validate digital object exists
    if (!digitalObjectRepository.existsById(digitalObjectId)) {
      throw new DigitalObjectNotFoundException(
          "Cannot update datastream content. Digital object not found: " + digitalObjectId
      );
    }

    // 2. Validate file is present
    if (file == null || file.isEmpty()) {
      throw new DatastreamValidationException(
          "Cannot update datastream content for " + dsid
              + " on object " + digitalObjectId + ". File must not be empty"
      );
    }

    // 3. Find existing datastream
    DatastreamId datastreamId = new DatastreamId(dsid, digitalObjectId);
    Datastream existing = datastreamRepository.findById(datastreamId)
        .orElseThrow(() -> new DatastreamNotFoundException(
            "Cannot update datastream content. Datastream not found: " + dsid
                + " on digital object: " + digitalObjectId
        ));

    // 4. Stream new file content to disk with checksum computation
    //    saveWithChecksums uses TRUNCATE_EXISTING, so the old file is overwritten
    WriteResult writeResult;
    try (InputStream inputStream = file.getInputStream()) {
      writeResult = datastreamContentRepository.saveWithChecksums(inputStream, datastreamId);
    } catch (IOException e) {
      throw new DatastreamCannotWriteFileException(
          "Failed to update file content for datastream " + datastreamId
              + " at digital object: " + digitalObjectId
              + " Original error: " + e.getMessage(), e);
    }

    // 5. Update content-derived fields
    existing.setMd5Checksum(writeResult.md5Checksum());
    existing.setSha512Checksum(writeResult.sha512Checksum());
    existing.setSize(file.getSize());
    existing.setMimeType(resolveMimeType(file));

    // 6. Persist
    datastreamRepository.save(existing);

    // 7. Publish modification event
    String currentUser = userPrincipalAuditorMapping.getCurrentAuditor()
        .orElseThrow(() -> new UserAuthenticationRequiredException(
            "Failed to update datastream content " + dsid + ". Current user is not logged in"
        ));

    applicationEventPublisher.publishEvent(
        new DigitalObjectModifiedEvent(
            this,
            new DigitalObjectId(digitalObjectId),
            Instant.now(),
            currentUser
        )
    );

    log.info("Successfully updated content of datastream {} on object {}", dsid, digitalObjectId);
    return findDatastreamDetailsById(datastreamId);
  }


  /**
   * Applies non-null fields from the patch DTO to the existing datastream.
   * Follows the same pattern as DigitalObjectService.applyPatch().
   */
  private void applyPatch(Datastream existing, DatastreamUpdateDto patch) {
    MetadataBaseEntity metadata = existing.getBaseMetadata();

    if (patch.getTitle() != null) {
      metadata.setTitle(patch.getTitle());
    }
    if (patch.getDescription() != null) {
      metadata.setDescription(patch.getDescription());
    }
    if (patch.getCreator() != null) {
      metadata.setCreator(patch.getCreator());
    }
    if (patch.getRights() != null) {
      metadata.setRights(patch.getRights());
    }
    if (patch.getTags() != null) {
      existing.setTags(new HashSet<>(patch.getTags()));
    }
    if (patch.getLang() != null) {
      existing.setLang(new HashSet<>(patch.getLang()));
    }
  }

  /**
   * Validates that required fields are not empty after applying a patch.
   * Collects ALL violations before throwing, so the client sees everything at once.
   */
  private void validateInvariants(Datastream datastream) {
    List<String> violations = new ArrayList<>();

    MetadataBaseEntity metadata = datastream.getBaseMetadata();
    if (metadata.getTitle() == null || metadata.getTitle().isEmpty()) {
      violations.add("Title must not be empty");
    }
    if (metadata.getRights() == null || metadata.getRights().isEmpty()) {
      violations.add("Rights must not be empty");
    }
    if (metadata.getCreator() == null || metadata.getCreator().isEmpty()) {
      violations.add("Creator must not be empty");
    }

    if (!violations.isEmpty()) {
      throw new DatastreamValidationException(
          "PATCH would violate constraints on datastream " + datastream.getDsid()
              + " of object " + datastream.getDigitalObject().getId() + ": "
              + String.join(", ", violations)
      );
    }
  }

}
