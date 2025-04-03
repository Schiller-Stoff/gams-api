package org.zim.gamsapi.Datastream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDatastreamContent;
import org.zim.gamsapi.enums.TestDigitalObject;

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

  @Autowired
  private IDatastreamContentRepository datastreamContentRepository;

  private Project testProject;

  private DigitalObject testDigitalObject;

  @MockBean
  private AuditingHandler auditingHandler;


  @BeforeEach
  public void setup() {
    testDigitalObject = TestDigitalObject.generate();
    testProject = testDigitalObject.getProject();
    projectRepository.save(testProject);
    digitalObjectRepository.save(testDigitalObject);
  }

  @Nested
  public class WebClientTests {

    @Test
    @Transactional
    public void getDatastreamRendersExpectedDsidInView() throws Exception {

      Datastream datastream = TestDatastream.generate(testDigitalObject, "testDsid.txt");

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
    @Transactional
    public void datastreamViewDisplaysExpectedMetadata() throws Exception {

      Datastream datastream = TestDatastream.generate(testDigitalObject);

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

      String responseContent = mvcResult.getResponse().getContentAsString();

      Assertions.assertThat(responseContent)
          .contains(
              datastream.getDsid(),
              datastream.getMimeType(),
              datastream.getFileName(),
              datastream.getBaseMetadata().getTitle(),
              datastream.getBaseMetadata().getDescription(),
              datastream.getBaseMetadata().getCreator()
          );

      Assertions.assertThat(datastream.getTags()).isNotEmpty();
      datastream.getTags().forEach(tag -> {
        Assertions.assertThat(responseContent).contains(tag);
      });

      Assertions.assertThat(datastream.getLang()).isNotEmpty();
      datastream.getLang().forEach(lang -> {
        Assertions.assertThat(responseContent).contains(lang);
      });


      // cleanup
      datastreamRepository.delete(datastream);

    }

  }


  @Nested
  public class DELETEDatastream {

    @Test
    public void deleteDatastreamRemovesDatastreamFromDatabase() throws Exception {

      Datastream testDatastream = TestDatastream.generate(testDigitalObject);

      MockMultipartFile multipartFile = TestDatastreamContent.generate();
      datastreamService.save(testDatastream, multipartFile);

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
      Datastream datastream = TestDatastream.generate(testDigitalObject, "testDsid.bla");

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
      Datastream datastream = TestDatastream.generate(testDigitalObject);

      datastreamContentRepository.save(TestDatastreamContent.CONTENT.getValue().getBytes(), datastream.deriveDatastreamId());
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
      Assertions.assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(TestDatastreamContent.CONTENT.getValue());

      // Cleanup
      datastreamRepository.delete(datastream);
    }

  }


  @Nested
  public class DatastreamFiltering {

    @Nested
    public class MainResource {
      @Test
      public void returnsErrorIfNoMainResourceIsSetOnTheDigitalObject() throws Exception {

        // ensure that the digital object has no main resource set
        testDigitalObject.setMainResource("");
        digitalObjectRepository.save(testDigitalObject);

        Datastream datastream = TestDatastream.generate(
            testDigitalObject,
            "testDsid.txt"
        );
        datastreamRepository.save(datastream);


        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream",
            testProject.getProjectAbbr(),
            testDigitalObject.getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is5xxServerError());


      }

      @Test
      public void returnsClientErrorIfMainResourceIsNotFound() throws Exception {

        // set as main resource on digital object
        testDigitalObject.setMainResource("nonExistingDsid");
        digitalObjectRepository.save(testDigitalObject);

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream",
            testProject.getProjectAbbr(),
            testDigitalObject.getId()
        );

        // saving an unrelated datastream
        Datastream datastream = TestDatastream.generate(
            testDigitalObject,
            "testDsid.txt"
        );
        datastreamRepository.save(datastream);

        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());

      }


      @Test
      public void returnsExpectedMainDatastreamJSONByDefault() throws Exception {

        Datastream datastream = TestDatastream.generate(testDigitalObject, "testDsid.txt");
        datastreamRepository.save(datastream);

        // set as main resource on digital object
        testDigitalObject.setMainResource(datastream.getDsid());
        digitalObjectRepository.save(testDigitalObject);

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream",
            testProject.getProjectAbbr(),
            testDigitalObject.getId()
        );

        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(
                datastream.getDsid(),
                testDigitalObject.getId(),
                datastream.getSize().toString(),
                datastream.getFileName()
            );


      }



    }





  }


}
