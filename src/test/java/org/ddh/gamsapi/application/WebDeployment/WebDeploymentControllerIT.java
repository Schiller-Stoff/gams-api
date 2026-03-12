package org.ddh.gamsapi.application.WebDeployment;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSStorageProperties;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link WebDeploymentController}.
 *
 * <p>Tests the HTTP layer (status codes, response body structure, content negotiation)
 * against real service/repository/filesystem infrastructure. Security filters are
 * disabled — authentication and authorization are tested separately in
 * {@code AuthenticationIT} and {@code AuthorizationIT}.
 *
 * <p>Uses the Spring-configured {@code gams.storage.webRootPath}
 * (defaults to {@code gams-web-test} in the test profile).
 */
@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebDeploymentControllerIT extends IntegrationTest {

  private static final String BASE_URL = "/api/v1/projects/%s/web";

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuditingHandler auditingHandler;

  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private WebDeploymentRepository webDeploymentRepository;

  @Autowired
  private GAMSStorageProperties gamsStorageProperties;

  /** Resolved absolute path to the web root directory. */
  private Path webRoot;

  @BeforeEach
  public void setup() {
    webRoot = Paths.get(gamsStorageProperties.getWebRootPath()).toAbsolutePath();

    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of("test-user"));
    projectRepository.save(
        ProjectBuilder.builder()
            .projectAbbr(TestProject.PROJECT_ABBR.getValue())
            .build()
    );
  }

  @Override
  @AfterEach
  public void tearDown() {
    cleanupProjectWebDir(TestProject.PROJECT_ABBR.getValue());
    webDeploymentRepository.deleteAll();
    super.tearDown();
  }

  private void cleanupProjectWebDir(String projectAbbr) {
    Path projectDir = webRoot.resolve(projectAbbr);
    if (!Files.exists(projectDir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(projectDir)) {
      paths.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try { Files.delete(p); } catch (IOException ignored) {}
          });
    } catch (IOException ignored) {}
  }

  private String url(String projectAbbr) {
    return String.format(BASE_URL, projectAbbr);
  }

  // ==================================================================================
  // PUT (deploy)
  // ==================================================================================

  @Nested
  @DisplayName("PUT /api/v1/projects/{projectAbbr}/web")
  public class PUTDeploy {

    @Test
    public void returns200WithDeploymentInfoOnSuccess() throws Exception {
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html>Hello</html>")
      );

      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.projectAbbr")
              .value(TestProject.PROJECT_ABBR.getValue()))
          .andExpect(MockMvcResultMatchers.jsonPath("$.deployedBy")
              .value("test-user"))
          .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount")
              .value(1))
          .andExpect(MockMvcResultMatchers.jsonPath("$.totalSize").isNumber())
          .andExpect(MockMvcResultMatchers.jsonPath("$.deployedAt").isNotEmpty())
          .andReturn();

      // Verify response Content-Type
      Assertions.assertThat(result.getResponse().getContentType())
          .contains("application/json");
    }

    @Test
    public void createsFilesOnFilesystem() throws Exception {
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html/>"),
          new ZipEntryData("css/styles.css", "body {}")
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.exists(projectDir.resolve("index.html"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("css/styles.css"))).isTrue();
    }

    @Test
    public void createsDatabaseRecord() throws Exception {
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html/>")
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      Assertions.assertThat(
          webDeploymentRepository.findById(TestProject.PROJECT_ABBR.getValue())
      ).isPresent();
    }

    @Test
    public void returns404WhenProjectDoesNotExist() throws Exception {
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html/>")
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url("nonexist"))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    public void returns400WhenFileIsEmpty() throws Exception {
      MockMultipartFile emptyFile = new MockMultipartFile(
          "file", "empty.zip", "application/zip", new byte[0]
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(emptyFile)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    public void redeploymentReplacesContentAndUpdatesMetadata() throws Exception {
      // First deployment
      MockMultipartFile firstFile = createMockZipFile(
          new ZipEntryData("old.html", "<html>old</html>")
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(firstFile)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.exists(projectDir.resolve("old.html"))).isTrue();

      // Second deployment with different content
      MockMultipartFile secondFile = createMockZipFile(
          new ZipEntryData("new1.html", "<html>new1</html>"),
          new ZipEntryData("new2.html", "<html>new2</html>")
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(secondFile)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount").value(2));

      // Old content gone, new content present
      Assertions.assertThat(Files.exists(projectDir.resolve("old.html"))).isFalse();
      Assertions.assertThat(Files.exists(projectDir.resolve("new1.html"))).isTrue();
      Assertions.assertThat(Files.exists(projectDir.resolve("new2.html"))).isTrue();

      // Still only one DB record (upsert)
      Assertions.assertThat(webDeploymentRepository.count()).isEqualTo(1);
    }
  }

  // ==================================================================================
  // GET (deployment info)
  // ==================================================================================

  @Nested
  @DisplayName("GET /api/v1/projects/{projectAbbr}/web")
  public class GETDeploymentInfo {

    @Test
    public void returns200WithDeploymentInfoWhenDeploymentExists() throws Exception {
      // Deploy first
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html/>"),
          new ZipEntryData("app.js", "console.log();")
      );
      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      // Then query
      mockMvc.perform(
              MockMvcRequestBuilders.get(url(TestProject.PROJECT_ABBR.getValue()))
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.projectAbbr")
              .value(TestProject.PROJECT_ABBR.getValue()))
          .andExpect(MockMvcResultMatchers.jsonPath("$.deployedBy")
              .value("test-user"))
          .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount").value(2))
          .andExpect(MockMvcResultMatchers.jsonPath("$.totalSize").isNumber())
          .andExpect(MockMvcResultMatchers.jsonPath("$.deployedAt").isNotEmpty());
    }

    @Test
    public void returns404WhenProjectDoesNotExist() throws Exception {
      mockMvc.perform(
              MockMvcRequestBuilders.get(url("nonexist"))
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    public void returns404WhenNoDeploymentExists() throws Exception {
      mockMvc.perform(
              MockMvcRequestBuilders.get(url(TestProject.PROJECT_ABBR.getValue()))
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());
    }
  }

  // ==================================================================================
  // DELETE (undeploy)
  // ==================================================================================

  @Nested
  @DisplayName("DELETE /api/v1/projects/{projectAbbr}/web")
  public class DELETEUndeploy {

    @Test
    public void returns204OnSuccessfulUndeploy() throws Exception {
      // Deploy first
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html/>")
      );
      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      // Undeploy
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url(TestProject.PROJECT_ABBR.getValue()))
          )
          .andExpect(status().isNoContent());
    }

    @Test
    public void removesFilesystemContentAndDatabaseRecord() throws Exception {
      // Deploy first
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html/>")
      );
      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url(TestProject.PROJECT_ABBR.getValue()))
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      Path projectDir = webRoot.resolve(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(Files.isDirectory(projectDir)).isTrue();
      Assertions.assertThat(
          webDeploymentRepository.findById(TestProject.PROJECT_ABBR.getValue())
      ).isPresent();

      // Undeploy
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url(TestProject.PROJECT_ABBR.getValue()))
          )
          .andExpect(status().isNoContent());

      // Verify clean state
      Assertions.assertThat(Files.exists(projectDir)).isFalse();
      Assertions.assertThat(
          webDeploymentRepository.findById(TestProject.PROJECT_ABBR.getValue())
      ).isEmpty();
    }

    @Test
    public void returns404WhenProjectDoesNotExist() throws Exception {
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url("nonexist"))
          )
          .andExpect(status().isNotFound());
    }

    @Test
    public void returns404WhenNoDeploymentExists() throws Exception {
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url(TestProject.PROJECT_ABBR.getValue()))
          )
          .andExpect(status().isNotFound());
    }
  }

  // ==================================================================================
  // Full HTTP lifecycle
  // ==================================================================================

  @Nested
  @DisplayName("Full HTTP lifecycle")
  public class FullHTTPLifecycle {

    @Test
    public void putThenGetThenDeleteThenGetReturns404() throws Exception {
      String projectUrl = url(TestProject.PROJECT_ABBR.getValue());

      // Step 1: PUT → 200
      MockMultipartFile file = createMockZipFile(
          new ZipEntryData("index.html", "<html>v1</html>"),
          new ZipEntryData("style.css", "body {}")
      );
      mockMvc.perform(
              MockMvcRequestBuilders.multipart(projectUrl)
                  .file(file)
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount").value(2));

      // Step 2: GET → 200 with matching metadata
      mockMvc.perform(
              MockMvcRequestBuilders.get(projectUrl)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount").value(2))
          .andExpect(MockMvcResultMatchers.jsonPath("$.deployedBy").value("test-user"));

      // Step 3: DELETE → 204
      mockMvc.perform(
              MockMvcRequestBuilders.delete(projectUrl)
          )
          .andExpect(status().isNoContent());

      // Step 4: GET → 404
      mockMvc.perform(
              MockMvcRequestBuilders.get(projectUrl)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());
    }
  }


  // ==================================================================================
  // Zip creation helpers
  // ==================================================================================

  private record ZipEntryData(String path, String content) {}

  /**
   * Creates a {@link MockMultipartFile} with the part name "file"
   * (matching the controller's {@code @RequestParam("file")}) containing
   * the given zip entries.
   */
  private static MockMultipartFile createMockZipFile(ZipEntryData... entries) {
    byte[] zipBytes = createZipBytes(entries);
    return new MockMultipartFile(
        "file", "site.zip", "application/zip", zipBytes
    );
  }

  private static byte[] createZipBytes(ZipEntryData... entries) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      for (ZipEntryData entry : entries) {
        zos.putNextEntry(new ZipEntry(entry.path()));
        zos.write(entry.content().getBytes());
        zos.closeEntry();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create test zip", e);
    }
    return baos.toByteArray();
  }
}