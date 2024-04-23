package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
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

  @Autowired
  private IDatastreamRepository datastreamRepository;

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

  @Nested
  public class WebclientTests {



    @Test
    public void getDigitalObjectRendersExpectedViewValues() throws Exception {

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(testProject)
          .objectType("TEI")
          .build();

      digitalObjectRepository.save(digitalObject);

      String url = String.format("/api/v1/projects/%s/objects/%s", testProject.getProjectAbbr(), digitalObject.getId());

      MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              digitalObject.getId(),
              digitalObject.getProject().getProjectAbbr(),
              digitalObject.getObjectType()
          );

      // cleanup
      digitalObjectRepository.delete(digitalObject);


    }


    @Test
    public void digitalObjectShowsExpectedDatastreamDsids() throws Exception {

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(testProject)
          .objectType("TEI")
          .build();

      digitalObjectRepository.save(digitalObject);

      Datastream datastream = new DatastreamBuilder()
          .dsid("testDsId")
          .digitalObject(digitalObject)
          .build();

      Datastream datastream2 = new DatastreamBuilder()
          .dsid("testDsId2")
          .digitalObject(digitalObject)
          .build();

      datastreamRepository.save(datastream);
      datastreamRepository.save(datastream2);

      String url = String.format("/api/v1/projects/%s/objects/%s", testProject.getProjectAbbr(), digitalObject.getId());

      MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              datastream.getDsid(),
              datastream2.getDsid()
          );



      // cleanup
      datastreamRepository.delete(datastream);
      datastreamRepository.delete(datastream2);
      digitalObjectRepository.delete(digitalObject);

    }


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