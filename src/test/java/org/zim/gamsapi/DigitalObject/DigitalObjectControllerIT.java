package org.zim.gamsapi.DigitalObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
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

  @MockBean
  private AuditingHandler auditingHandler;

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
  public class PUTRequests {


    @Test
    public void createsExpectedDigitalObject() throws Exception {
      // Arrange
      DigitalObject expectedDigitalObject = new DigitalObjectBuilder()
          .id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .project(testProject)
          .objectType("TEI")
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      ObjectMapper objectMapper = new ObjectMapper();
      String expectedDigitalObjectJson = objectMapper.writeValueAsString(expectedDigitalObject);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), expectedDigitalObject.getId())
          .contentType(MediaType.APPLICATION_JSON)
          .content(expectedDigitalObjectJson))
          // PUT request will redirect to GET
          .andExpect(status().is3xxRedirection());

      // Assert - PUT digital object can be found via repository class
      org.assertj.core.api.Assertions.assertThat(
          digitalObjectRepository.findDigitalObjectById(expectedDigitalObject.getId()))
            .isPresent()
            .get()
            .isNotNull()
            .extracting(DigitalObjectDetailsView::getId)
            .isEqualTo(expectedDigitalObject.getId()
      );

      // clean up
      digitalObjectRepository.deleteAll();

    }





  }


  @Nested
  public class DELETERequests {

    @Test
    public void deleteDigitalObjectWhenItExists() throws Exception {
      // Arrange
      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .project(testProject)
          .objectType("TEI")
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      digitalObjectRepository.save(digitalObject);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId())
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is3xxRedirection());

      // Assert
      org.assertj.core.api.Assertions.assertThat(
          digitalObjectRepository.findDigitalObjectById(digitalObject.getId()))
            .isNotPresent();

      // clean up
      digitalObjectRepository.deleteAll();

    }

    @Test
    public void deleteDigitalObjectWhenItContainsDatastreams() throws Exception {
      // Arrange
      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .project(testProject)
          .objectType("TEI")
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      digitalObjectRepository.save(digitalObject);

      Datastream datastream = new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(digitalObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      datastreamRepository.save(datastream);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is3xxRedirection());

      // Assert
      org.assertj.core.api.Assertions.assertThat(
              digitalObjectRepository.findDigitalObjectById(digitalObject.getId()))
          .isNotPresent();

      org.assertj.core.api.Assertions.assertThat(
              datastreamRepository.findById(datastream.deriveDatastreamId()))
          .isNotPresent();

      // clean up
      digitalObjectRepository.deleteAll();
      datastreamRepository.deleteAll();
    }

  }

  @Nested
  public class WebclientTests {



    @Test
    public void getDigitalObjectRendersExpectedViewValues() throws Exception {

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(testProject)
          .objectType("TEI")
          .baseMetadata(TestMetadataBaseEntity.generate())
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
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      digitalObjectRepository.save(digitalObject);

      Datastream datastream = new DatastreamBuilder()
          .dsid("testDsId")
          .digitalObject(digitalObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Datastream datastream2 = new DatastreamBuilder()
          .dsid("testDsId2")
          .digitalObject(digitalObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
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

    @Test
    public void getDigitalObjectRendersExpectedBaseMetadata() throws Exception {

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(testProject)
          .objectType("TEI")
          .baseMetadata(TestMetadataBaseEntity.generate())
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

      // all values of the metadata base entity should be present in the view
      org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              digitalObject.getId(),
              digitalObject.getBaseMetadata().getTitle(),
              digitalObject.getBaseMetadata().getDescription(),
              digitalObject.getBaseMetadata().getCreator(),
              digitalObject.getBaseMetadata().getPublisher(),
              digitalObject.getBaseMetadata().getRights()
          );

      // cleanup
      digitalObjectRepository.delete(digitalObject);


    }


  }

  @Test
  public void getObjectJsonReturnsDigitalObjectWhenItExists() throws Exception {

    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(
        new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject).baseMetadata(TestMetadataBaseEntity.generate())
          .build()
        );

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

  @Test
  public void getProjectObjectsJsonReturnsEmptyListWhenNoDigitalObjectsExistForProject() throws Exception {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertEquals("[]", mvcResult.getResponse().getContentAsString());
  }

  @Test
  public void getProjectObjectsJsonReturnsDigitalObjectsWhenTheyExistForProject() throws Exception {
    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(
        new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject)
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build()

    );

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(OBJECT_TEST_ID));

    digitalObjectRepository.deleteById(OBJECT_TEST_ID);
  }

  @Test
  public void deleteObjectRemovesDigitalObjectWhenItExists() throws Exception {
    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject).baseMetadata(TestMetadataBaseEntity.generate()).build());

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), OBJECT_TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is3xxRedirection());

    Assertions.assertFalse(digitalObjectRepository.existsById(OBJECT_TEST_ID));
  }

  @Test
  public void deleteObjectDoesNotThrowExceptionWhenDigitalObjectDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  public void getFindAllIdsReturnsExpectedObjectIds() throws Exception {

    final String OBJECT_TEST_ID = "testPid";
    digitalObjectRepository.save(
        new DigitalObjectBuilder().id(OBJECT_TEST_ID).project(testProject).baseMetadata(TestMetadataBaseEntity.generate()).build()
    );

    final String OBJECT_TEST_ID2 = "testPid2";
    digitalObjectRepository.save(
        new DigitalObjectBuilder().id(OBJECT_TEST_ID2).project(testProject).baseMetadata(TestMetadataBaseEntity.generate()).build()
    );

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects?style=idlist", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
        .contains(OBJECT_TEST_ID, OBJECT_TEST_ID2);

    digitalObjectRepository.deleteById(OBJECT_TEST_ID);
    digitalObjectRepository.deleteById(OBJECT_TEST_ID2);


  }

}