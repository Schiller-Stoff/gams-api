package org.ddh.gamsapi.application.WebDeployment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.events.ProjectPreDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebDeploymentEventListener {

  private final WebDeploymentService webDeploymentService;

  /**
   * Listens for ProjectDeletedEvent to ensure that when a project is deleted,
   * its associated web deployment content is also removed from the filesystem.
   *
   * @param event the project deletion event
   */
  @EventListener
  public void handleProjectPreDeletedEvent(ProjectPreDeletedEvent event) {
    log.info("Project {} deleted. Cleaning up web deployment content.", event.getProjectAbbr());
    try {
      webDeploymentService.undeploy(event.getProjectAbbr());
    } catch (org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentNotFoundException _) {
      log.debug("No web deployment found for deleted project {}. Nothing to clean up.", event.getProjectAbbr());
    } catch (Exception e) {
      log.error("Failed to undeploy web content for deleted project {}: {}", event.getProjectAbbr(), e.getMessage());
    }
  }

}
