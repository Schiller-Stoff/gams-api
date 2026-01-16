package org.ddh.gamsapi.domain.DigitalObject.DigitalObjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;

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
      throw new ProjectNotFoundException(
          "Project with id " + projectAbbr + " does not exist"
      );
    }

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      throw new DigitalObjectNotFoundException(
          "DigitalObject with id " + digitalObjectId + " does not exist"
      );
    }

    DigitalObjectModification digitalObjectModification = calculateLatestModificationDate(digitalObjectId);
    log.info("Calculated latest modification date for digital object {} as {}", digitalObjectId, digitalObjectModification);
    return digitalObjectModification;
  }

  @Override
  public DigitalObjectModification findLastModifiedDate(String projectAbbr, String digitalObjectId) {
    if(!projectRepository.existsById(projectAbbr)){
      throw new ProjectNotFoundException(
          "Project with id " + projectAbbr + " does not exist"
      );
    }

    var foundDigitalObject = digitalObjectRepository.findDigitalObjectById(digitalObjectId)
        .orElseThrow(() -> new DigitalObjectNotFoundException(
            "DigitalObject with id " + digitalObjectId + " does not exist"
        ));

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
      throw new DigitalObjectNotFoundException(
          "DigitalObject with id " + digitalObjectId + " does not exist"
      );
    }

    DigitalObject foundDigitalObject = digitalObjectOptional.get();

    Optional<Date> datastreamLastModifiedDate = datastreamRepository.findMaxLastModifiedDateByDigitalObjectId(digitalObjectId);

    // return last modified (if no datastreams are available)
    if(datastreamLastModifiedDate.isEmpty()) {
       log.debug("No datastreams found for digital object with id {}", digitalObjectId);
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
