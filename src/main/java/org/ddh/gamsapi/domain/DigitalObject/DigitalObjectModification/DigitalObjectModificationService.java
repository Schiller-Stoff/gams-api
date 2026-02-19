package org.ddh.gamsapi.domain.DigitalObject.DigitalObjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalObjectModificationService  implements IDigitalObjectModificationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IProjectRepository projectRepository;

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

}
