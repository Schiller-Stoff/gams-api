package org.zim.gamsapi.GAMSCollection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.GAMSCollection.exceptions.CollectionNotFoundException;
import org.zim.gamsapi.GAMSCollection.interfaces.GAMSCollectionDetailsView;
import org.zim.gamsapi.GAMSCollection.interfaces.GamsCollectionCompactView;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GAMSCollectionService implements IGAMSCollectionService {

  private final IGAMSCollectionRepository collectionRepository;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IProjectRepository projectRepository;

  @Override
  @Transactional
  public void save(GAMSCollection gamsCollection) {
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
  public Page<GamsCollectionCompactView> findAll(Pageable pageable) {
    log.error("TRIGGERED SERVICE findAll(Pageable pageable)");
    log.error("Pageable: {}", pageable);
    var collections = collectionRepository.findAllProjectedBy(pageable);
    log.error("TRIGGERED SERVICE findAll(Pageable pageable) - collections: {}", collections);
//    collections.stream().forEach(collection -> {
//      log.error("Collection Title: {}", collection.getTitle());
//    });

    log.error("FOUND {} collections", collections.getTotalElements());

    return collectionRepository.findAllProjectedBy(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GAMSCollection> findByProjectAbbr(String projectAbbr) {
    return collectionRepository.findByProject_ProjectAbbr(projectAbbr);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<GamsCollectionCompactView> findByProjectAbbr(String projectAbbr, Pageable pageable) {
    return collectionRepository.findByProject_ProjectAbbr(projectAbbr, pageable);
  }

  @Override
  @Transactional
  public GAMSCollection addDigitalObjectToCollection(String collectionId, String digitalObjectId) {
//    GAMSCollectionDetailsView gamsCollection = findById(collectionId);
//    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
//        .orElseThrow(() -> {
//          String msg = String.format("Digital object with id %s not found", digitalObjectId);
//          log.error(msg);
//          return new DigitalObjectNotFoundException(msg);
//        });
//
//    gamsCollection.getDigitalObjects().add(digitalObject);
    //TODO fix
    return null;
    //return collectionRepository.save(gamsCollection);
  }

  @Override
  @Transactional
  public GAMSCollection removeDigitalObjectFromCollection(String collectionId, String digitalObjectId) {
    GAMSCollectionDetailsView GAMSCollection = findById(collectionId);
    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
        .orElseThrow(() -> {
          String msg = String.format("Digital object with id %s not found", digitalObjectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    GAMSCollection.getDigitalObjects().remove(digitalObject);
    // TODO fix
    return null;
    //return collectionRepository.save(GAMSCollection);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<GamsCollectionCompactView> findByDigitalObject(String digitalObjectId, Pageable pageable) {
    DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
        .orElseThrow(() -> {
          String msg = String.format("Digital object with id %s not found", digitalObjectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    return collectionRepository.findByDigitalObjectsId(digitalObject.getId(), pageable);
  }

  @Override
  public Page<DigitalObjectListItemView> findDigitalObjectsByCollectionId(String collectionId, Pageable pageable){

    if(!collectionRepository.existsById(collectionId)) {
      String msg = String.format("Collection with id %s does not exist!", collectionId);
      log.error(msg);
      throw new CollectionNotFoundException(msg);
    }

    return digitalObjectRepository.findDigitalObjectsByCollectionId(collectionId, pageable);

  }

}
