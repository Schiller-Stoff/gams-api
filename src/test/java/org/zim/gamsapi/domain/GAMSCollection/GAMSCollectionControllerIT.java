package org.zim.gamsapi.domain.GAMSCollection;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.domain.GAMSCollection.GAMSCollection;
import org.zim.gamsapi.domain.GAMSCollection.IGAMSCollectionRepository;
import org.zim.gamsapi.domain.GAMSCollection.IGAMSCollectionService;
import org.zim.gamsapi.domain.GAMSCollection.exceptions.CollectionNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.domain.Project.Project;
import org.zim.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.TestUtilities.TestGAMSCollection;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GAMSCollectionControllerIT extends IntegrationTest {


  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private IGAMSCollectionRepository collectionRepository;

  @Autowired
  private IGAMSCollectionService collectionService;

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

      final String URL = "/api/v1/collections";

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
    public void GETCollectionsForProjectContainsExpectedJsonValues() throws Exception {
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
      final String SECOND_COLLECTION_ID = "test-collection-id-2";
      final GAMSCollection SECOND_TEST_GAMS_COLLECTION = TestGAMSCollection.generate(
          testProject.getProjectAbbr(),
          testDigitalObject.getId(),
          SECOND_COLLECTION_ID
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

    @Test
    public void savesExpectedGamsCollection() throws Exception {

      final String GAMS_COLLECTION_ID = "test-collection-id-random";

      final String URL = String.format(
          "/api/v1/projects/%s/collections/%s",
          testProject.getProjectAbbr(),
          GAMS_COLLECTION_ID
      );

      GAMSCollection gamsCollection = TestGAMSCollection.generate(
          testProject.getProjectAbbr(),
          testDigitalObject.getId(),
          GAMS_COLLECTION_ID
      );

      final String REQUEST_BODY = """
          {
            "title": "%s",
            "description": "%s",
            "projectAbbr": "%s"
          }
          """.formatted(
              gamsCollection.getTitle(),
              gamsCollection.getDescription(),
              gamsCollection.getProject().getProjectAbbr()
          );

      mockMvc.perform(
              MockMvcRequestBuilders.put(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(REQUEST_BODY)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      // assert that the collection exists in the database
      Assertions.assertThat(
           collectionRepository.existsById(GAMS_COLLECTION_ID)
       ).isTrue();

    }


  }

  @Nested
  public class POSTGAMSCollection {

    GAMSCollection testGAMSCollection;

    @BeforeEach
    public void setup() {
      testGAMSCollection = TestGAMSCollection.generate();
      collectionRepository.save(testGAMSCollection);
    }

    @Test
    @Transactional
    public void addsExpectedDigitalObjectToCollection() throws Exception {

      // save an additional gams collection
      final String GAMS_COLLECTION_ID = "test-collection-id-random";
      final GAMSCollection TEST_GAMS_COLLECTION = TestGAMSCollection.generate(
          testProject.getProjectAbbr(),
          testDigitalObject.getId(),
          GAMS_COLLECTION_ID
      );
      collectionRepository.save(TEST_GAMS_COLLECTION);

      final String URL = String.format(
          "/api/v1/projects/%s/collections/%s/objects/%s",
          // add object to different collection
          testProject.getProjectAbbr(),
          GAMS_COLLECTION_ID,
          testDigitalObject.getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.post(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      var foundCollection = collectionRepository.findById(TEST_GAMS_COLLECTION.getId())
          .orElseThrow(() -> {
            String msg = String.format("Collection with id %s not found", TEST_GAMS_COLLECTION.getId());
            return new AssertionError(msg);});

      // assert that the collection contains the digital object
      Assertions.assertThat(foundCollection
              .getDigitalObjects()
      ).contains(testDigitalObject);

    }


  }

  @Nested
  public class PATCHGAMSCollection {

    GAMSCollection testGAMSCollection;

    @BeforeEach
    public void setup() {
      testGAMSCollection = TestGAMSCollection.generate();
      collectionRepository.save(testGAMSCollection);
    }

    @Test
    public void updatesGamsCollectionToExpectedValues() throws Exception {

      final String URL = String.format(
          "/api/v1/projects/%s/collections/%s",
          testProject.getProjectAbbr(),
          testGAMSCollection.getId()
      );

      final String TEST_COLLECTION_CHANGED_TITLE = "changed-title";
      final String TEST_COLLECTION_CHANGED_DESCRIPTION = "changed-description";

      final String REQUEST_BODY = """
          {
            "title": "%s",
            "description": "%s",
            "projectAbbr": "%s"
          }
          """.formatted(
              TEST_COLLECTION_CHANGED_TITLE,
              TEST_COLLECTION_CHANGED_DESCRIPTION,
              testGAMSCollection.getProject().getProjectAbbr()
          );

      mockMvc.perform(
              MockMvcRequestBuilders.patch(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(REQUEST_BODY)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk());

      GAMSCollection foundCollection = collectionRepository.findById(testGAMSCollection.getId())
          .orElseThrow(() -> {
            String msg = String.format("Collection with id %s not found", testGAMSCollection.getId());
            return new AssertionError(msg);
          });

      // assert that the collection now has changed values
      Assertions.assertThat(foundCollection.getTitle())
          .isEqualTo(TEST_COLLECTION_CHANGED_TITLE);
      Assertions.assertThat(foundCollection.getDescription())
          .isEqualTo(TEST_COLLECTION_CHANGED_DESCRIPTION);

      // assert that the collection does not have the old values
      Assertions.assertThat(foundCollection.getTitle())
          .isNotEqualTo(testGAMSCollection.getTitle());
      Assertions.assertThat(foundCollection.getDescription())
          .isNotEqualTo(testGAMSCollection.getDescription());


    }

    @Test
    public void throwsIfExpectedCollectionWasNotFound() throws Exception {

      final String TEST_NON_EXISTENT_COLLECTION_ID = "test-non-existent-collection-id";

      final String URL = String.format(
          "/api/v1/projects/%s/collections/%s",
          testProject.getProjectAbbr(),
          TEST_NON_EXISTENT_COLLECTION_ID
      );

      final String REQUEST_BODY = """
          {
            "title": "%s",
            "description": "%s",
            "projectAbbr": "%s"
          }
          """.formatted(
          testGAMSCollection.getId(),
          testGAMSCollection.getTitle(),
          testGAMSCollection.getProject().getProjectAbbr()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.patch(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(REQUEST_BODY)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());

    }


  }

  @Nested
  public class DELETEGAMSCollection {

    GAMSCollection testGAMSCollection;

    @BeforeEach
    public void setup() {
      testGAMSCollection = TestGAMSCollection.generate();
      collectionRepository.save(testGAMSCollection);
    }

    @Test
    public void deletesExpectedGamsCollection() throws Exception {

      final String URL = String.format(
          "/api/v1/projects/%s/collections/%s",
          testProject.getProjectAbbr(),
          testGAMSCollection.getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.delete(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNoContent());

      // assert that the collection does not exist in the database
      Assertions.assertThat(
          collectionRepository.existsById(testGAMSCollection.getId())
      ).isFalse();

    }

    @Test
    public void deletionWorksEvenIfDigitalObjectsIsStillBeingReferenced() throws Exception {

      // try to delete a collection that still references a digital object
      collectionService.addDigitalObjectToCollection(
          testGAMSCollection.getId(),
          testDigitalObject.getId()
      );

      var foundObjects = collectionService.findDigitalObjectsByCollectionId(
          testGAMSCollection.getId(), PageRequest.of(0, 100)
      );

      Assertions.assertThat(foundObjects.getContent())
          .hasSize(1);

      final String URL = String.format(
          "/api/v1/projects/%s/collections/%s",
          testProject.getProjectAbbr(),
          testGAMSCollection.getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.delete(URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNoContent());

      // assert that the collection does not exist in the database
      Assertions.assertThat(
          collectionRepository.existsById(testGAMSCollection.getId())
      ).isFalse();

      // assert that now finding digital objects by the defined collection id throws an exception
      Assertions.assertThatThrownBy(() -> collectionService.findDigitalObjectsByCollectionId(
          testGAMSCollection.getId(), PageRequest.of(0, 100)
      )).isInstanceOf(CollectionNotFoundException.class);

    }

    @Nested
    public class RemoveDigitalObjectFromCollection {

      @Test
      public void removesExpectedDigitalObjectFromCollection() throws Exception {

        collectionService.addDigitalObjectToCollection(
            testGAMSCollection.getId(),
            testDigitalObject.getId()
        );

        // assert that both exist
        Assertions.assertThat(collectionRepository.existsById(testGAMSCollection.getId()))
            .isTrue();
        Assertions.assertThat(digitalObjectRepository.existsById(testDigitalObject.getId()))
            .isTrue();

        var foundObjectsInTestCollection = digitalObjectRepository.findDigitalObjectsByCollectionId(
            testGAMSCollection.getId(),
            PageRequest.of(0, 100)
        );

        Assertions.assertThat(foundObjectsInTestCollection.getContent())
            .hasSize(1);

        final String URL = String.format(
            "/api/v1/projects/%s/collections/%s/objects/%s",
            testProject.getProjectAbbr(),
            testGAMSCollection.getId(),
            testDigitalObject.getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.delete(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNoContent());

        // assert that the collection is now empty
        var foundObjectsInTestCollectionAfterDeletion = digitalObjectRepository.findDigitalObjectsByCollectionId(
            testGAMSCollection.getId(), PageRequest.of(0, 100)
        );

        Assertions.assertThat(foundObjectsInTestCollectionAfterDeletion.getContent()).hasSize(0);


      }

    }

  }
}
