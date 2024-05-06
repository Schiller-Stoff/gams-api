package org.zim.gamsapi.Datastream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;

  @Autowired
  private IDatastreamService datastreamService;

  private Project testProject;

  private DigitalObject testDigitalObject;

  @BeforeAll
  public void setup() {
    testProject = Project.builder().projectAbbr("testProject").build();
    testDigitalObject = new DigitalObjectBuilder()
        .id("testId")
        .project(testProject)
        .baseMetadata(TestMetadataBaseEntity.generate())
        .build();
    projectRepository.save(testProject);
    digitalObjectRepository.save(testDigitalObject);
  }

  @AfterAll
  public void tearDown() {
    digitalObjectRepository.delete(testDigitalObject);
    projectRepository.delete(testProject);
    org.assertj.core.api.Assertions.assertThat(projectRepository.findAll())
        .isNotNull()
        .isEmpty();
  }

  @Nested
  public class WebClientTests {


    @Test
    public void getDatastreamRendersExpectedDsidInView() throws Exception {

      Datastream datastream = new DatastreamBuilder()
          .dsid("testDsid")
          .digitalObject(testDigitalObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      datastreamRepository.save(datastream);

      String url = String.format("/api/v1/projects/%s/objects/%s/datastreams/%s", testProject.getProjectAbbr(), testDigitalObject.getId(), datastream.getDsid());

      MvcResult mvcResult = mockMvc.perform(
          MockMvcRequestBuilders.get(url)
              .contentType(MediaType.TEXT_HTML)
              .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("Datastream/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              datastream.getDsid(),
              testDigitalObject.getId(),
              testProject.getProjectAbbr()
          );


      // cleanup
      datastreamRepository.delete(datastream);


    }


    @Test
    public void datastreamViewDisplaysExpectedMetadata() throws Exception {

      Datastream datastream = new DatastreamBuilder()
          .dsid("testDsid")
          .digitalObject(testDigitalObject)
          .mimeType(MediaType.APPLICATION_CBOR.toString())
          .fileName("testFileName")
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      datastreamRepository.save(datastream);

      String url = String.format("/api/v1/projects/%s/objects/%s/datastreams/%s", testProject.getProjectAbbr(), testDigitalObject.getId(), datastream.getDsid());

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.TEXT_HTML)
                  .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("Datastream/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              datastream.getDsid(),
              datastream.getMimeType(),
              datastream.getFileName(),
              datastream.getBaseMetadata().getTitle(),
              datastream.getBaseMetadata().getDescription(),
              datastream.getBaseMetadata().getCreator()
          );


      // cleanup
      datastreamRepository.delete(datastream);


    }

  }


  @Nested
  public class DELETEDatastream {

    @Test
    public void deleteDatastreamRemovesDatastreamFromDatabase() throws Exception {

      Datastream testDatastream =  new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(testDigitalObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      datastreamService.save(testDatastream);

      // DELETE request
      String url = String.format("/api/v1/projects/%s/objects/%s/datastreams/%s", testProject.getProjectAbbr(), testDigitalObject.getId(), testDatastream.getDsid());
      mockMvc.perform(
          MockMvcRequestBuilders.delete(url))
          .andExpect(status().is3xxRedirection());

      // assertions
      org.junit.jupiter.api.Assertions.assertThrows(DatastreamNotFoundException.class, () -> {
        datastreamService.findById(testDatastream.deriveDatastreamId());
      });

      Assertions.assertThat(datastreamService.findAll(testDigitalObject))
          .isNotNull()
          .isEmpty();

      // cleanup
      datastreamRepository.deleteAll();

    }


  }


  @Nested
  public class GETDatastreamJSON {

    @Test
    public void getDatastreamJsonContainsExpectedValues() throws Exception {
      // Arrange
      Datastream datastream = new DatastreamBuilder()
          .dsid("testDsid")
          .digitalObject(testDigitalObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      datastreamRepository.save(datastream);

      String url = String.format("/api/v1/projects/%s/objects/%s/datastreams/%s", testProject.getProjectAbbr(), testDigitalObject.getId(), datastream.getDsid());

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              datastream.getDsid(),
              testDigitalObject.getId(),
              datastream.getBaseMetadata().getTitle(),
              datastream.getBaseMetadata().getDescription(),
              datastream.getBaseMetadata().getCreator()
          );

      // Cleanup
      datastreamRepository.delete(datastream);
    }


  }


  @Nested
  public class GETDatastreamContent {


    @Test
    public void getDatastreamContentReturnsExpectedDatastreamContent() throws Exception {
      // Arrange
      Datastream datastream = new DatastreamBuilder()
          .dsid("testDsid")
          .digitalObject(testDigitalObject)
          .data(TestDatastream.CONTENT.getValue().getBytes())
          .baseMetadata(TestMetadataBaseEntity.generate())
          .mimeType(MediaType.TEXT_PLAIN_VALUE)
          .build();

      datastreamRepository.save(datastream);

      String url = String.format("/api/v1/projects/%s/objects/%s/datastreams/%s/content", testProject.getProjectAbbr(), testDigitalObject.getId(), datastream.getDsid());

      // Act
      MvcResult mvcResult = mockMvc.perform(
          MockMvcRequestBuilders.get(url)
      )
        .andExpect(status().isOk())
        .andReturn();

      // Assert
      Assertions.assertThat(mvcResult.getResponse()).isNotNull();
      Assertions.assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(TestDatastream.CONTENT.getValue());

      // Cleanup
      datastreamRepository.delete(datastream);
    }

  }

}
