package org.zim.gamsapi.DigitalObject.DigitalObjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalObjectModificationService  implements IDigitalObjectModificationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IProjectRepository projectRepository;

  public DigitalObjectModification findLatestModificationDate(String projectAbbr, String digitalObjectId) {
    if(!projectRepository.existsById(projectAbbr)){
      String msg = String.format("Project with id %s does not exist", projectAbbr);
      log.error(msg);
      throw new ProjectNotFoundException(msg);
    }

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      String msg = String.format("DigitalObject with id %s does not exist", digitalObjectId);
      log.warn(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    DigitalObjectModification digitalObjectModification = calculateLatestModificationDate(digitalObjectId);
    log.info("Calculated latest modification date for digital object {} as {}", digitalObjectId, digitalObjectModification);
    return digitalObjectModification;
  }

  @Override
  public DigitalObjectModification findLastModifiedDate(String projectAbbr, String digitalObjectId) {
    if(!projectRepository.existsById(projectAbbr)){
      String msg = String.format("Project with id %s does not exist", projectAbbr);
      log.error(msg);
      throw new ProjectNotFoundException(msg);
    }

    var foundDigitalObject = digitalObjectRepository.findDigitalObjectById(digitalObjectId)
        .orElseThrow(() -> {
          String msg = String.format("DigitalObject with id %s does not exist", digitalObjectId);
          log.warn(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    DigitalObjectModification digitalObjectModification = new DigitalObjectModification();
    digitalObjectModification.setId(digitalObjectId);
    digitalObjectModification.setLatestModificationDate(foundDigitalObject.getModified());
    log.info("Found last modified date for digital object {} as {}", digitalObjectId, digitalObjectModification.getLatestModificationDate());
    return digitalObjectModification;
  }

  public DigitalObjectModification calculateLatestModificationDate(String digitalObjectId) {

    // setup for return
    DigitalObjectModification digitalObjectModification = new DigitalObjectModification();
    digitalObjectModification.setId(digitalObjectId);

    // this could be a more efficient query?
    Optional<DigitalObject> digitalObjectOptional = digitalObjectRepository.findById(digitalObjectId);

    if(digitalObjectOptional.isEmpty()) {
      String msg = String.format("DigitalObject with id %s does not exist", digitalObjectId);
      log.error(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    DigitalObject foundDigitalObject = digitalObjectOptional.get();

    Optional<Date> datastreamLastModifiedDate = datastreamRepository.findMaxLastModifiedDateByDigitalObjectId(digitalObjectId);

    // return last modified (if no datastreams are available)
    if(datastreamLastModifiedDate.isEmpty()) {
       String msg = String.format("No datastreams found for digital object with id %s", digitalObjectId);
       log.debug(msg);
       digitalObjectModification.setLatestModificationDate(foundDigitalObject.getModified());
       return digitalObjectModification;
    }

    Date lastDigitalObjectModifiedDate = foundDigitalObject.getModified();
    Date lastDatastreamModifiedDate = datastreamLastModifiedDate.get();

    // set the latest modification date
    if(lastDigitalObjectModifiedDate.after(lastDatastreamModifiedDate)) {
      digitalObjectModification.setLatestModificationDate(lastDigitalObjectModifiedDate);
    } else {
      digitalObjectModification.setLatestModificationDate(lastDatastreamModifiedDate);
    }

    return digitalObjectModification;

  }

}
