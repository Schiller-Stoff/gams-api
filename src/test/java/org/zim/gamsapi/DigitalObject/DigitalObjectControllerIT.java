package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  private Project testProject;

  @BeforeAll
  public void setup() {
    testProject = Project.builder().projectAbbr("testProject").build();
    projectRepository.save(testProject);
  }

  @AfterAll
  public void tearDown() {
    projectRepository.delete(testProject);
    org.assertj.core.api.Assertions.assertThat(projectRepository.findAll())
        .isNotNull()
        .isEmpty();
  }

  @Test
  public void getObjectJsonReturnsDigitalObjectWhenItExists() throws Exception {

    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject).build());

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), OBJECT_TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(OBJECT_TEST_ID));

    digitalObjectRepository.deleteById(OBJECT_TEST_ID);
  }

  @Test
  public void getObjectJsonThrowsExceptionWhenDigitalObjectDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Disabled("TODO returns status code 401 instead of 204 - because of failing auth config during testing")
  @Test
  public void getProjectObjectsJsonReturnsEmptyListWhenNoDigitalObjectsExistForProject() throws Exception {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().isEmpty());
  }

  @Test
  public void getProjectObjectsJsonReturnsDigitalObjectsWhenTheyExistForProject() throws Exception {
    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject).build());

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(OBJECT_TEST_ID));

    digitalObjectRepository.deleteById(OBJECT_TEST_ID);
  }

  @Test
  @Disabled("TODO returns status code 401 instead of 204 - because of failing auth config during testing")
  public void deleteObjectRemovesDigitalObjectWhenItExists() throws Exception {
    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject).build());

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), OBJECT_TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    Assertions.assertFalse(digitalObjectRepository.existsById(OBJECT_TEST_ID));
  }

  @Test
  @Disabled("TODO returns status code 401 instead of 204 - because of failing auth config during testing")
  public void deleteObjectDoesNotThrowExceptionWhenDigitalObjectDoesNotExist() throws Exception {

    // TODO returns status code 401 instead of 204 - because of failing auth config during testing

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }
}