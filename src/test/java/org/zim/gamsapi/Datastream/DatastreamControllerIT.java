package org.zim.gamsapi.Datastream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
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

  private Project testProject;

  private DigitalObject testDigitalObject;

  @BeforeAll
  public void setup() {
    testProject = Project.builder().projectAbbr("testProject").build();
    testDigitalObject = new DigitalObjectBuilder()
        .id("testId")
        .project(testProject)
        .baseMetadata(
            new MetadataBaseEntityBuilder()
                .title("test-title")
                .rights("test-rights")
                .publisher("test-publisher")
                .creator("test-creator")
                .description("test-description")
                .build()
        )
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
          .baseMetadata(
              new MetadataBaseEntityBuilder()
                .title("testTitle")
                .description("testDescription")
                .creator("testCreator")
                .rights("testRights")
                .publisher("testPublisher")
                .build()
          )
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


}
