package org.zim.gamsapi.domain.DigitalObjectCollection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.domain.DigitalObjectCollection.exceptions.DigitalObjectCollectionNotFoundException;
import org.zim.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionDetailsView;
import org.zim.gamsapi.domain.DigitalObjectCollection.interfaces.DigitalObjectCollectionCompactView;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.infrastructure.System.dto.PagedResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalObjectCollectionService implements IDigitalObjectCollectionService {

  private final IDigitalObjectCollectionRepository collectionRepository;
  private final IDigitalObjectRepository digitalObjectRepository;

  @Override
  @Transactional
  public void save(DigitalObjectCollection digitalObjectCollection) {
    if(collectionRepository.existsById(digitalObjectCollection.getId())) {
      String msg = String.format("Collection with id %s already exists", digitalObjectCollection.getId());
      log.error(msg);
      throw new DigitalObjectCollectionNotFoundException(msg);
    }
    DigitalObjectCollection savedDigitalObjectCollection = collectionRepository.save(digitalObjectCollection);
    log.info("Successfully saved collection:  {}", savedDigitalObjectCollection);
  }

  @Override
  @Transactional
  public void delete(DigitalObjectCollection DigitalObjectCollection) {
    collectionRepository.delete(DigitalObjectCollection);
  }

  @Override
  @Transactional(readOnly = true)
  public DigitalObjectCollectionDetailsView findById(String id) {

    return collectionRepository.findProjectedById(id)
        .orElseThrow(() -> {
          String msg = String.format("Collection with id %s not found", id);
          log.error(msg);
          return new DigitalObjectCollectionNotFoundException(msg);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<DigitalObjectCollectionCompactView> findAll(Pageable pageable) {
    log.error("TRIGGERED SERVICE findAll(Pageable pageable)");
    log.error("Pageable: {}", pageable);
    var collections = collectionRepository.findAllProjectedBy(pageable);
    log.error("TRIGGERED SERVICE findAll(Pageable pageable) - collections: {}", collections);
//    collections.stream().forEach(collection -> {
//      log.error("Collection Title: {}", collection.getTitle());
//    });

    log.error("FOUND {} collections", collections.getTotalElements());

    return PagedResponse.from(
        collectionRepository.findAllProjectedBy(pageable)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<DigitalObjectCollection> findByProjectAbbr(String projectAbbr) {
    return collectionRepository.findByProject_ProjectAbbr(projectAbbr);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<DigitalObjectCollectionCompactView> findByProjectAbbr(String projectAbbr, Pageable pageable) {
    return PagedResponse.from(
        collectionRepository.findByProject_ProjectAbbr(projectAbbr, pageable)
    );
  }

  @Override
  @Transactional
  public void addDigitalObjectToCollection(String collectionId, String digitalObjectId) {

    var gamsCollection =  collectionRepository.findById(collectionId)
        .orElseThrow(() -> {
          String msg = String.format("Collection with id %s not found", collectionId);
          log.error(msg);
          return new DigitalObjectCollectionNotFoundException(msg);
        });

    var digitalObject = digitalObjectRepository.findById(digitalObjectId)
        .orElseThrow(() -> {
          String msg = String.format("Digital object with id %s not found", digitalObjectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    Set<DigitalObject> digitalObjects = new HashSet<>(gamsCollection.getDigitalObjects());
    digitalObjects.add(digitalObject);

    gamsCollection.setDigitalObjects(digitalObjects);
    collectionRepository.save(gamsCollection);
  }

  @Override
  @Transactional
  public void removeDigitalObjectFromCollection(String collectionId, String digitalObjectId) {
    var gamsCollection =  collectionRepository.findById(collectionId)
        .orElseThrow(() -> {
          String msg = String.format("Collection with id %s not found. Tried to remove digital object with id %s fr the collection. Aborting operation.", collectionId, digitalObjectId);
          log.error(msg);
          return new DigitalObjectCollectionNotFoundException(msg);
        });

    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
        .orElseThrow(() -> {
          String msg = String.format("Digital object %s to be removed from collection with id %s not found", collectionId, digitalObjectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    gamsCollection.getDigitalObjects().remove(digitalObject);
    collectionRepository.save(gamsCollection);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<DigitalObjectCollectionCompactView> findByDigitalObject(String digitalObjectId, Pageable pageable) {
    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
        .orElseThrow(() -> {
          String msg = String.format("Digital object with id %s not found", digitalObjectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    return PagedResponse.from(
        collectionRepository.findByDigitalObjectsId(digitalObject.getId(), pageable)
    );
  }

  @Override
  public PagedResponse<DigitalObjectListItemView> findDigitalObjectsByCollectionId(String collectionId, Pageable pageable){

    if(!collectionRepository.existsById(collectionId)) {
      String msg = String.format("Collection with id %s does not exist!", collectionId);
      log.error(msg);
      throw new DigitalObjectCollectionNotFoundException(msg);
    }

    return PagedResponse.from(
        digitalObjectRepository.findDigitalObjectsByCollectionId(collectionId, pageable)
    );

  }

  @Override
  @Transactional
  public void updateMetadata(DigitalObjectCollection digitalObjectCollection) {
    DigitalObjectCollection existingCollection = collectionRepository.findById(digitalObjectCollection.getId())
        .orElseThrow(() -> {
          String msg = String.format("Collection with id %s not found", digitalObjectCollection.getId());
          log.error(msg);
          return new DigitalObjectCollectionNotFoundException(msg);
        });
    existingCollection.setTitle(digitalObjectCollection.getTitle());
    existingCollection.setDescription(digitalObjectCollection.getDescription());
  }

  @Transactional
  @Override
  public void deleteById(String id) {
    if(!collectionRepository.existsById(id)) {
      String msg = String.format("Collection with id %s does not exist and therefore cannot be deleted!", id);
      log.error(msg);
      throw new DigitalObjectCollectionNotFoundException(msg);
    }
    collectionRepository.deleteById(id);
  }
}
