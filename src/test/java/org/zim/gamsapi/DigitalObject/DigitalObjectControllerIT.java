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

  // Add more tests for other methods in the DigitalObjectController class
}