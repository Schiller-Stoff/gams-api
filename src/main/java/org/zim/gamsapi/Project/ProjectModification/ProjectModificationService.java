package org.zim.gamsapi.Project.ProjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Project.exceptions.ProjectException;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectModificationService implements IProjectModificationService {
  private final IProjectRepository projectRepository;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  @Transactional
  public ProjectModification findLatestModificationDate(String projectAbbr) {

    if(!projectRepository.existsById(projectAbbr)){
      String msg = String.format("Project with project-abbreviation %s does not exist", projectAbbr);
      log.warn(msg);
      throw new ProjectNotFoundException(msg);
    }

    ProjectModification projectModification = calculateLatestModificationDate(projectAbbr);
    log.info("Calculated latest modification date for project {} as {}", projectAbbr, projectModification);
    return projectModification;
  }


  @Transactional(readOnly = true)
  public ProjectModification calculateLatestModificationDate(String projectAbbr) {

    log.trace("Calculating latest modification date for project {} ", projectAbbr);

    // Get latest modification date from project
    Optional<Date> projectLastModifiedOptional = projectRepository
        .findLastModifiedDateByProjectAbbr(projectAbbr);

    if (projectLastModifiedOptional.isEmpty()) {
      String msg = String.format("Project with project-abbreviation %s has no modification date assigned (shouldn't happen!)", projectAbbr);
      log.error(msg);
      throw new ProjectException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
    }

    // Get latest modification date from digital objects
    Optional<Date> digitalObjectLastModifiedOptional = digitalObjectRepository
        .findMaxLastModifiedDateByProjectAbbr(projectAbbr);

    if (digitalObjectLastModifiedOptional.isEmpty()) {
      String msg = String.format("There are no last modified dates of digital objects for project %s. There might be none digital objects ingested", projectAbbr);
      log.debug(msg);
    }

    // Get latest modification date from datastreams
    Optional<Date> datastreamLastModifiedOptional = datastreamRepository
        .findMaxLastModifiedDateByProjectAbbr(projectAbbr);

    if(datastreamLastModifiedOptional.isEmpty()) {
      String msg = String.format("Cannot retrieve last modified date of datastreams for project %s. There might be no datastreams associated with objects of the project.", projectAbbr);
      log.debug(msg);
    }

    List<LocalDateTime> dates = Stream.of(
        digitalObjectLastModifiedOptional,
        datastreamLastModifiedOptional,
        projectLastModifiedOptional
    )
        .filter(Optional::isPresent)
        .map(date -> convertToLocalDateTime(date.get()))
        .toList();


    LocalDateTime latestDateTime = dates.stream()
        .max(LocalDateTime::compareTo)
        .orElseThrow(() -> {
          // at least the modified date on the Project relation should be available.
          // so this point should never be reached -> throw an error.
          String msg = String.format("Cannot determine latest modification date for project %s", projectAbbr);
          log.error(msg);
          return new ProjectException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        });

    Date latestModified = Date
        .from(latestDateTime.atZone(ZoneId.systemDefault())
            .toInstant());

    ProjectModification projectModification = new ProjectModification();
    projectModification.setProjectAbbr(projectAbbr);
    projectModification.setLatestModificationDate(latestModified);

    return projectModification;

  }

  /**
   * Converts a Date object to a LocalDateTime object using the system default time zone.
   * @param date The Date object to convert.
   * @return The LocalDateTime object.
   */
  public LocalDateTime convertToLocalDateTime(Date date) {
    return date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }



}
