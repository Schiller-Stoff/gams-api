package org.zim.gamsapi.GAMSCollection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.exceptions.CollectionNotFoundException;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.System.dto.PagedResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GAMSCollectionService implements IGAMSCollectionService {

  private final IGAMSCollectionRepository collectionRepository;
  private final IDigitalObjectRepository digitalObjectRepository;

  @Override
  @Transactional
  public void save(GAMSCollection gamsCollection) {
    if(collectionRepository.existsById(gamsCollection.getId())) {
      String msg = String.format("Collection with id %s already exists", gamsCollection.getId());
      log.error(msg);
      throw new CollectionNotFoundException(msg);
    }
    GAMSCollection savedGAMSCollection = collectionRepository.save(gamsCollection);
    log.info("Successfully saved collection:  {}", savedGAMSCollection);
  }

  @Override
  @Transactional
  public void delete(GAMSCollection GAMSCollection) {
    collectionRepository.delete(GAMSCollection);
  }

  @Override
  @Transactional(readOnly = true)
  public GAMSCollectionDetailsView findById(String id) {

    return collectionRepository.findProjectedById(id)
        .orElseThrow(() -> {
          String msg = String.format("Collection with id %s not found", id);
          log.error(msg);
          return new CollectionNotFoundException(msg);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<GamsCollectionCompactView> findAll(Pageable pageable) {
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
  public List<GAMSCollection> findByProjectAbbr(String projectAbbr) {
    return collectionRepository.findByProject_ProjectAbbr(projectAbbr);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<GamsCollectionCompactView> findByProjectAbbr(String projectAbbr, Pageable pageable) {
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
          return new CollectionNotFoundException(msg);
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
          return new CollectionNotFoundException(msg);
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
  public PagedResponse<GamsCollectionCompactView> findByDigitalObject(String digitalObjectId, Pageable pageable) {
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
      throw new CollectionNotFoundException(msg);
    }

    return PagedResponse.from(
        digitalObjectRepository.findDigitalObjectsByCollectionId(collectionId, pageable)
    );

  }

  @Override
  @Transactional
  public void updateMetadata(GAMSCollection gamsCollection) {
    GAMSCollection existingCollection = collectionRepository.findById(gamsCollection.getId())
        .orElseThrow(() -> {
          String msg = String.format("Collection with id %s not found", gamsCollection.getId());
          log.error(msg);
          return new CollectionNotFoundException(msg);
        });
    existingCollection.setTitle(gamsCollection.getTitle());
    existingCollection.setDescription(gamsCollection.getDescription());
  }

  @Transactional
  @Override
  public void deleteById(String id) {
    if(!collectionRepository.existsById(id)) {
      String msg = String.format("Collection with id %s does not exist and therefore cannot be deleted!", id);
      log.error(msg);
      throw new CollectionNotFoundException(msg);
    }
    collectionRepository.deleteById(id);
  }
}
