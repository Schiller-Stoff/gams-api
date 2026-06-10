package org.ddh.gamsapi.application.WebDeployment;

import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.WebDeployment.dto.WebDeploymentInfo;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentNotFoundException;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserAuthenticationRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebDeploymentService}.
 *
 * <p>Tests cover the three public methods: deploy, getDeploymentInfo, undeploy.
 * Each method is tested for its expected success path and all known failure modes
 * (project not found, user not authenticated, deployment not found).
 *
 * <p>Filesystem operations (WebDeploymentContentRepository) and database operations
 * (WebDeploymentRepository) are mocked. The two-phase design (I/O first, then DB)
 * is verified by checking invocation order where relevant.
 */
public class WebDeploymentServiceTest extends UnitTest {

  private static final String PROJECT_ABBR = "test";
  private static final String DEPLOYING_USER = "test-user";
  private static final int EXPECTED_FILE_COUNT = 42;
  private static final long EXPECTED_TOTAL_SIZE = 123456L;

  @Mock
  private IProjectRepository projectRepository;

  @Mock
  private WebDeploymentRepository webDeploymentRepository;

  @Mock
  private WebDeploymentContentRepository webDeploymentContentRepository;

  @Mock
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @InjectMocks
  private WebDeploymentService webDeploymentService;


  // ==================================================================================
  // deploy()
  // ==================================================================================

  @Nested
  @DisplayName("deploy()")
  public class Deploy {

    private InputStream zipStream;

    @BeforeEach
    public void setup() {
      zipStream = new ByteArrayInputStream("fake-zip-content".getBytes());
    }

    @Test
    public void throwsProjectNotFoundExceptionWhenProjectDoesNotExist() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(false);

      assertThatThrownBy(() -> webDeploymentService.deploy(PROJECT_ABBR, zipStream))
          .isInstanceOf(ProjectNotFoundException.class)
          .hasMessageContaining(PROJECT_ABBR);

      verify(projectRepository).existsById(PROJECT_ABBR);
      verifyNoInteractions(webDeploymentContentRepository);
      verifyNoInteractions(webDeploymentRepository);
    }

    @Test
    public void throwsUserAuthenticationRequiredExceptionWhenUserNotLoggedIn() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(userPrincipalAuditorMapping.getCurrentAuditor()).thenReturn(Optional.empty());

      assertThatThrownBy(() -> webDeploymentService.deploy(PROJECT_ABBR, zipStream))
          .isInstanceOf(UserAuthenticationRequiredException.class)
          .hasMessageContaining(PROJECT_ABBR);

      verify(projectRepository).existsById(PROJECT_ABBR);
      verify(userPrincipalAuditorMapping).getCurrentAuditor();
      verifyNoInteractions(webDeploymentContentRepository);
      verifyNoInteractions(webDeploymentRepository);
    }

    @Test
    public void delegatesToContentRepositoryAndPersistsMetadata() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(userPrincipalAuditorMapping.getCurrentAuditor())
          .thenReturn(Optional.of(DEPLOYING_USER));
      when(webDeploymentContentRepository.deploy(eq(PROJECT_ABBR), any(InputStream.class)))
          .thenReturn(new WebDeploymentContentRepository.DeploymentStats(
              EXPECTED_FILE_COUNT, EXPECTED_TOTAL_SIZE));
      // first deployment — no existing record
      when(webDeploymentRepository.findById(PROJECT_ABBR)).thenReturn(Optional.empty());
      when(webDeploymentRepository.save(any(WebDeployment.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      WebDeploymentInfo result = webDeploymentService.deploy(PROJECT_ABBR, zipStream);

      assertThat(result).isNotNull();
      assertThat(result.projectAbbr()).isEqualTo(PROJECT_ABBR);
      assertThat(result.deployedBy()).isEqualTo(DEPLOYING_USER);
      assertThat(result.fileCount()).isEqualTo(EXPECTED_FILE_COUNT);
      assertThat(result.totalSize()).isEqualTo(EXPECTED_TOTAL_SIZE);
      assertThat(result.deployedAt()).isNotNull();

      verify(webDeploymentContentRepository).deploy(eq(PROJECT_ABBR), any(InputStream.class));
      verify(webDeploymentRepository).save(any(WebDeployment.class));
    }

    @Test
    public void updatesExistingDeploymentMetadataOnRedeployment() {
      Instant previousDeploymentTime = Instant.parse("2025-01-01T00:00:00Z");

      WebDeployment existingDeployment = WebDeployment.builder()
          .projectAbbr(PROJECT_ABBR)
          .deployedAt(previousDeploymentTime)
          .deployedBy("old-user")
          .fileCount(10)
          .totalSize(5000L)
          .build();

      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(userPrincipalAuditorMapping.getCurrentAuditor())
          .thenReturn(Optional.of(DEPLOYING_USER));
      when(webDeploymentContentRepository.deploy(eq(PROJECT_ABBR), any(InputStream.class)))
          .thenReturn(new WebDeploymentContentRepository.DeploymentStats(
              EXPECTED_FILE_COUNT, EXPECTED_TOTAL_SIZE));
      when(webDeploymentRepository.findById(PROJECT_ABBR))
          .thenReturn(Optional.of(existingDeployment));
      when(webDeploymentRepository.save(any(WebDeployment.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      WebDeploymentInfo result = webDeploymentService.deploy(PROJECT_ABBR, zipStream);

      assertThat(result.deployedBy()).isEqualTo(DEPLOYING_USER);
      assertThat(result.fileCount()).isEqualTo(EXPECTED_FILE_COUNT);
      assertThat(result.totalSize()).isEqualTo(EXPECTED_TOTAL_SIZE);
      assertThat(result.deployedAt()).isAfter(previousDeploymentTime);

      verify(webDeploymentRepository).save(any(WebDeployment.class));
    }

    @Test
    public void performsFilesystemOperationsBeforeDatabaseOperations() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(userPrincipalAuditorMapping.getCurrentAuditor())
          .thenReturn(Optional.of(DEPLOYING_USER));
      when(webDeploymentContentRepository.deploy(eq(PROJECT_ABBR), any(InputStream.class)))
          .thenReturn(new WebDeploymentContentRepository.DeploymentStats(1, 100L));
      when(webDeploymentRepository.findById(PROJECT_ABBR)).thenReturn(Optional.empty());
      when(webDeploymentRepository.save(any(WebDeployment.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      webDeploymentService.deploy(PROJECT_ABBR, zipStream);

      // Verify order: content repository (filesystem) is called before
      // deployment repository (database)
      var inOrder = inOrder(webDeploymentContentRepository, webDeploymentRepository);
      inOrder.verify(webDeploymentContentRepository).deploy(eq(PROJECT_ABBR), any(InputStream.class));
      inOrder.verify(webDeploymentRepository).findById(PROJECT_ABBR);
      inOrder.verify(webDeploymentRepository).save(any(WebDeployment.class));
    }

    @Test
    public void doesNotPersistMetadataWhenFilesystemDeploymentFails() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(userPrincipalAuditorMapping.getCurrentAuditor())
          .thenReturn(Optional.of(DEPLOYING_USER));
      when(webDeploymentContentRepository.deploy(eq(PROJECT_ABBR), any(InputStream.class)))
          .thenThrow(new RuntimeException("Disk full"));

      assertThatThrownBy(() -> webDeploymentService.deploy(PROJECT_ABBR, zipStream))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Disk full");

      verifyNoInteractions(webDeploymentRepository);
    }
  }

  // ==================================================================================
  // getDeploymentInfo()
  // ==================================================================================

  @Nested
  @DisplayName("getDeploymentInfo()")
  public class GetDeploymentInfo {

    @Test
    public void throwsProjectNotFoundExceptionWhenProjectDoesNotExist() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(false);

      assertThatThrownBy(() -> webDeploymentService.getDeploymentInfo(PROJECT_ABBR))
          .isInstanceOf(ProjectNotFoundException.class)
          .hasMessageContaining(PROJECT_ABBR);

      verify(projectRepository).existsById(PROJECT_ABBR);
      verifyNoInteractions(webDeploymentRepository);
    }

    @Test
    public void throwsWebDeploymentNotFoundExceptionWhenNoDeploymentExists() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentRepository.findById(PROJECT_ABBR)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> webDeploymentService.getDeploymentInfo(PROJECT_ABBR))
          .isInstanceOf(WebDeploymentNotFoundException.class)
          .hasMessageContaining(PROJECT_ABBR);

      verify(webDeploymentRepository).findById(PROJECT_ABBR);
    }

    @Test
    public void returnsExpectedDeploymentInfoWhenDeploymentExists() {
      Instant deployedAt = Instant.parse("2026-03-11T10:00:00Z");

      WebDeployment deployment = WebDeployment.builder()
          .projectAbbr(PROJECT_ABBR)
          .deployedAt(deployedAt)
          .deployedBy(DEPLOYING_USER)
          .fileCount(EXPECTED_FILE_COUNT)
          .totalSize(EXPECTED_TOTAL_SIZE)
          .build();

      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentRepository.findById(PROJECT_ABBR))
          .thenReturn(Optional.of(deployment));

      WebDeploymentInfo result = webDeploymentService.getDeploymentInfo(PROJECT_ABBR);

      assertThat(result).isNotNull();
      assertThat(result.projectAbbr()).isEqualTo(PROJECT_ABBR);
      assertThat(result.deployedAt()).isEqualTo(deployedAt);
      assertThat(result.deployedBy()).isEqualTo(DEPLOYING_USER);
      assertThat(result.fileCount()).isEqualTo(EXPECTED_FILE_COUNT);
      assertThat(result.totalSize()).isEqualTo(EXPECTED_TOTAL_SIZE);
    }
  }

  // ==================================================================================
  // undeploy()
  // ==================================================================================

  // ==================================================================================
  // undeploy()
  // ==================================================================================

  @Nested
  @DisplayName("undeploy()")
  public class Undeploy {

    @Test
    public void throwsProjectNotFoundExceptionWhenProjectDoesNotExist() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(false);

      assertThatThrownBy(() -> webDeploymentService.undeploy(PROJECT_ABBR))
          .isInstanceOf(ProjectNotFoundException.class)
          .hasMessageContaining(PROJECT_ABBR);

      verify(projectRepository).existsById(PROJECT_ABBR);
      verifyNoInteractions(webDeploymentContentRepository);
      verifyNoInteractions(webDeploymentRepository);
    }

    @Test
    public void throwsWebDeploymentNotFoundExceptionWhenNoDeploymentExistsInFilesystemOrDatabase() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      // Simulate BOTH missing
      when(webDeploymentContentRepository.delete(PROJECT_ABBR)).thenReturn(false);
      when(webDeploymentRepository.existsById(PROJECT_ABBR)).thenReturn(false);

      assertThatThrownBy(() -> webDeploymentService.undeploy(PROJECT_ABBR))
          .isInstanceOf(WebDeploymentNotFoundException.class)
          .hasMessageContaining(PROJECT_ABBR);

      verify(webDeploymentContentRepository).delete(PROJECT_ABBR);
      verify(webDeploymentRepository).existsById(PROJECT_ABBR);
      verify(webDeploymentRepository, never()).deleteById(any());
    }

    @Test
    public void deletesFilesystemContentAndDatabaseRecordWhenBothExist() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentContentRepository.delete(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentRepository.existsById(PROJECT_ABBR)).thenReturn(true);

      webDeploymentService.undeploy(PROJECT_ABBR);

      verify(webDeploymentContentRepository).delete(PROJECT_ABBR);
      verify(webDeploymentRepository).deleteById(PROJECT_ABBR);
    }

    @Test
    public void recoversFromSplitBrainByDeletingDatabaseRecordWhenFilesystemContentIsMissing() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      // Simulate split brain: Folder is gone, but DB record still exists
      when(webDeploymentContentRepository.delete(PROJECT_ABBR)).thenReturn(false);
      when(webDeploymentRepository.existsById(PROJECT_ABBR)).thenReturn(true);

      webDeploymentService.undeploy(PROJECT_ABBR);

      // It should successfully recover and delete the orphaned DB record
      verify(webDeploymentContentRepository).delete(PROJECT_ABBR);
      verify(webDeploymentRepository).deleteById(PROJECT_ABBR);
    }

    @Test
    public void recoversFromSplitBrainByIgnoringMissingDatabaseRecordWhenFilesystemContentDeleted() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      // Simulate split brain: Folder exists, but DB record is missing
      when(webDeploymentContentRepository.delete(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentRepository.existsById(PROJECT_ABBR)).thenReturn(false);

      webDeploymentService.undeploy(PROJECT_ABBR);

      // It should delete the folder and skip the DB deletion
      verify(webDeploymentContentRepository).delete(PROJECT_ABBR);
      verify(webDeploymentRepository, never()).deleteById(any());
    }

    @Test
    public void performsFilesystemDeletionBeforeDatabaseDeletion() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentContentRepository.delete(PROJECT_ABBR)).thenReturn(true);
      when(webDeploymentRepository.existsById(PROJECT_ABBR)).thenReturn(true);

      webDeploymentService.undeploy(PROJECT_ABBR);

      var inOrder = inOrder(webDeploymentContentRepository, webDeploymentRepository);
      inOrder.verify(webDeploymentContentRepository).delete(PROJECT_ABBR);
      inOrder.verify(webDeploymentRepository).existsById(PROJECT_ABBR);
      inOrder.verify(webDeploymentRepository).deleteById(PROJECT_ABBR);
    }

    @Test
    public void doesNotDeleteDatabaseRecordWhenFilesystemDeletionFailsWithException() {
      when(projectRepository.existsById(PROJECT_ABBR)).thenReturn(true);
      // Simulating an actual I/O failure, not just a "not found"
      when(webDeploymentContentRepository.delete(PROJECT_ABBR))
          .thenThrow(new RuntimeException("Permission denied"));

      assertThatThrownBy(() -> webDeploymentService.undeploy(PROJECT_ABBR))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Permission denied");

      // Verify that if a real exception occurs in phase 1, phase 2 is aborted safely
      verify(webDeploymentRepository, never()).existsById(any());
      verify(webDeploymentRepository, never()).deleteById(any());
    }
  }
}