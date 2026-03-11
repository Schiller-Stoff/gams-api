package org.ddh.gamsapi.application.WebDeployment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.WebDeployment.dto.WebDeploymentInfo;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentNotFoundException;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserAuthenticationRequiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebDeploymentService {

  private final IProjectRepository projectRepository;
  private final WebDeploymentRepository webDeploymentRepository;
  private final WebDeploymentContentRepository webDeploymentContentRepository;
  private final IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  /**
   * Deploys a static web site for the given project.
   * Extracts the zip to the filesystem and records deployment metadata.
   * <p>
   * Follows the two-phase pattern from IngestService:
   * Phase 1 (I/O): extract zip to filesystem (no DB connection held)
   * Phase 2 (DB): persist deployment metadata
   *
   * @param projectAbbr project abbreviation
   * @param zipStream   input stream of the zip archive
   * @return deployment metadata
   */
  public WebDeploymentInfo deploy(String projectAbbr, InputStream zipStream) {

    // Validate project exists
    if (!projectRepository.existsById(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot deploy web content. Project does not exist: " + projectAbbr);
    }

    // Resolve current user
    String currentUser = userPrincipalAuditorMapping.getCurrentAuditor()
        .orElseThrow(() -> new UserAuthenticationRequiredException(
            "Cannot deploy web content for project " + projectAbbr
                + ". Current user is not logged in"));

    // Phase 1: Filesystem I/O (no DB connection held)
    WebDeploymentContentRepository.DeploymentStats stats =
        webDeploymentContentRepository.deploy(projectAbbr, zipStream);

    // Phase 2: Persist metadata
    // TODO do i need this method? (the problem: @transactional is not being reached!)
    return persistDeploymentMetadata(projectAbbr, currentUser, stats);
  }

  /**
   * Returns deployment metadata for the given project.
   *
   * @param projectAbbr project abbreviation
   * @return deployment info
   * @throws WebDeploymentNotFoundException if no deployment exists
   */
  @Transactional(readOnly = true)
  public WebDeploymentInfo getDeploymentInfo(String projectAbbr) {

    if (!projectRepository.existsById(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot retrieve web deployment info. Project does not exist: " + projectAbbr);
    }

    WebDeployment deployment = webDeploymentRepository.findById(projectAbbr)
        .orElseThrow(() -> new WebDeploymentNotFoundException(
            "No web deployment found for project: " + projectAbbr));

    return toDto(deployment);
  }

  /**
   * Removes the deployed static site for the given project.
   * Deletes both filesystem content and database record.
   *
   * @param projectAbbr project abbreviation
   */
  public void undeploy(String projectAbbr) {

    if (!projectRepository.existsById(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot undeploy web content. Project does not exist: " + projectAbbr);
    }

    // Phase 1: Filesystem deletion
    boolean existed = webDeploymentContentRepository.delete(projectAbbr);

    if (!existed) {
      throw new WebDeploymentNotFoundException(
          "No web deployment found for project: " + projectAbbr);
    }

    // Phase 2: Database record deletion
    deleteDeploymentMetadata(projectAbbr);

    log.info("Successfully undeployed web content for project {}", projectAbbr);
  }


  // ─── Private helpers ─────────────────────────────────────────────────

  @Transactional
  protected WebDeploymentInfo persistDeploymentMetadata(
      String projectAbbr,
      String deployedBy,
      WebDeploymentContentRepository.DeploymentStats stats) {

    WebDeployment deployment = webDeploymentRepository.findById(projectAbbr)
        .orElse(new WebDeployment());

    deployment.setProjectAbbr(projectAbbr);
    deployment.setDeployedAt(Instant.now());
    deployment.setDeployedBy(deployedBy);
    deployment.setFileCount(stats.fileCount());
    deployment.setTotalSize(stats.totalSize());

    WebDeployment saved = webDeploymentRepository.save(deployment);
    log.info("Recorded web deployment for project {}: {} files, {} bytes, by {}",
        projectAbbr, stats.fileCount(), stats.totalSize(), deployedBy);

    return toDto(saved);
  }

  @Transactional
  protected void deleteDeploymentMetadata(String projectAbbr) {
    webDeploymentRepository.deleteById(projectAbbr);
  }

  private WebDeploymentInfo toDto(WebDeployment entity) {
    return new WebDeploymentInfo(
        entity.getProjectAbbr(),
        entity.getDeployedAt(),
        entity.getDeployedBy(),
        entity.getFileCount(),
        entity.getTotalSize()
    );
  }
}