package org.zim.gamsapi.GAMSCollection;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestGAMSCollection;
import org.zim.gamsapi.enums.TestDigitalObject;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GAMSCollectionControllerIT extends IntegrationTest {


  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AuditingHandler auditingHandler;

  @Autowired
  private IGAMSCollectionRepository collectionRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IProjectRepository projectRepository;

  private Project testProject;

  private DigitalObject testDigitalObject;

  @BeforeEach
  public void setUp() {
    testDigitalObject = TestDigitalObject.generate();
    testProject = testDigitalObject.getProject();
    projectRepository.save(testProject);
    digitalObjectRepository.save(testDigitalObject);

  }


  @Nested
  public class GETGAMSCollection {

    GAMSCollection testGAMSCollection;

    @BeforeEach
    public void setup() {
      testGAMSCollection = TestGAMSCollection.generate();
      collectionRepository.save(testGAMSCollection);
    }


    @Test
    public void GETAllCollectionsContainsExpectedJsonValues() throws Exception {

      final String URL = "/api/v1/collections/";

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testGAMSCollection.getId(),
              testGAMSCollection.getTitle(),
              testGAMSCollection.getDescription(),
              testGAMSCollection.getDigitalObjects().iterator().next().getId(),
              testGAMSCollection.getProject().getProjectAbbr()
          );

    }

    @Test
    public void GETSingularCollectionContainsExpectedJsonValues() throws Exception {

      final String URL = "/api/v1/collections/" + testGAMSCollection.getId();

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testGAMSCollection.getId(),
              testGAMSCollection.getTitle(),
              testGAMSCollection.getDescription(),
              testGAMSCollection.getDigitalObjects().iterator().next().getId(),
              testGAMSCollection.getProject().getProjectAbbr()
          );

    }

    @Test
    public void GETcollectionsForProjectContainsExpectedJsonValues() throws Exception {
      final String URL = String.format("/api/v1/projects/%s/collections", testProject.getProjectAbbr());

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testGAMSCollection.getId(),
              testGAMSCollection.getTitle(),
              testGAMSCollection.getDescription(),
              testGAMSCollection.getDigitalObjects().iterator().next().getId(),
              testGAMSCollection.getProject().getProjectAbbr()
          );

    }

    @Test
    public void GETObjectsInCollectionContainsExpectedJsonValues() throws Exception {
      final String URL = String.format("/api/v1/collections/%s/objects", testGAMSCollection.getId());

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      DigitalObject expectedDigitalObject = testGAMSCollection.getDigitalObjects().iterator().next();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              expectedDigitalObject.getId()
          );

    }

    @Test
    public void GETAllCollectionsForASpecificObject() throws Exception {

      // saving a second collection to be found
      final String SECOND_COLLCTION_ID = "test-collection-id-2";
      final GAMSCollection SECOND_TEST_GAMS_COLLECTION = TestGAMSCollection.generate(
          testProject.getProjectAbbr(),
          testDigitalObject.getId(),
          SECOND_COLLCTION_ID
      );
      collectionRepository.save(SECOND_TEST_GAMS_COLLECTION);

      final String URL = String.format(
          "/api/v1/projects/%s/objects/%s/collections",
          testProject.getProjectAbbr(),
          testDigitalObject.getId()
      );

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              // response contains first collection data
              testGAMSCollection.getId(),
              testGAMSCollection.getTitle(),
              testGAMSCollection.getDescription(),
              testGAMSCollection.getDigitalObjects().iterator().next().getId(),
              testGAMSCollection.getProject().getProjectAbbr(),
              // response contains second collection data
              SECOND_TEST_GAMS_COLLECTION.getId(),
              SECOND_TEST_GAMS_COLLECTION.getTitle(),
              SECOND_TEST_GAMS_COLLECTION.getDescription(),
              SECOND_TEST_GAMS_COLLECTION.getProject().getProjectAbbr()
          );

    }

  }


  @Nested
  public class PUTGAMSCollection {

    GAMSCollection testGAMSCollection;

    @BeforeEach
    public void setup() {
      testGAMSCollection = TestGAMSCollection.generate();
      collectionRepository.save(testGAMSCollection);
    }

   // TODO implement tests


  }


}
