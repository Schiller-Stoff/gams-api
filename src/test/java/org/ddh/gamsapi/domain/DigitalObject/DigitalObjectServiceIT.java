package org.ddh.gamsapi.domain.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectService;
import org.ddh.gamsapi.application.Ingest.IngestService;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;

import java.util.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectServiceIT extends IntegrationTest {

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectService digitalObjectService;

  @Autowired
  IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Autowired
  IngestService ingestService;

  // Deactivates the auditing process.
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @BeforeEach
  public void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class Save {


    @Test
    public void successFullySavesSimpleDigitalObject() {
      // given
      DigitalObject digitalObject = TestDigitalObject.generate(
          testDataSet.project().getProjectAbbr(),
          testDataSet.project().getProjectAbbr() + ".randomid"
      );

      DigitalObject savedDigitalObject = digitalObjectService.save(digitalObject);

      // then
      Assertions.assertThat(savedDigitalObject).isNotNull();
      Assertions.assertThat(savedDigitalObject.getId()).isNotNull();
      Assertions.assertThat(savedDigitalObject.getProject()).isEqualTo(testDataSet.project());
      // considered equal because of same id
      Assertions.assertThat(savedDigitalObject).isEqualTo(digitalObject);

    }

  }

  @Nested
  public class FindAllByProjectAbbr {

    @Test
    public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      testDataBuilder.removeAllExceptProjects(testDataSet);
      PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(),
          Optional.empty(),
          Pageable.unpaged()
      );
      Assertions.assertThat(result.getContent()).isEmpty();
    }

    @Test
    public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {

      var result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(),
          Optional.empty(),
          Pageable.unpaged()
      );

      Assertions.assertThat(result.getContent()).isNotEmpty();
      Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(
          testDataSet.digitalObject().getId()
      );

    }

    @Test
    public void throwsExceptionWhenProjectDoesNotExist() {
      String projectAbbr = "nonExistentProject";
      Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged()))
          .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    public void findsDigitalObjectViaContainedInId(){

      digitalObjectService.findAllByProjectAbbr(
              testDataSet.project().getProjectAbbr(),
              testDataSet.digitalObject().getId(),
              PageRequest.of(0,100))
          .getContent()
          .forEach(digitalObject -> {
            Assertions.assertThat(digitalObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
            Assertions.assertThat(digitalObject.getProject().getProjectAbbr()).isEqualTo(testDataSet.project().getProjectAbbr());
          });

    }


  }

  @Nested
  public class FindById {

    @Test
    public void returnsDigitalObjectWhenItExists() {
      DigitalObject result = digitalObjectService.findById(
          testDataSet.digitalObject().getId()
      );
      Assertions.assertThat(result).isEqualTo(testDataSet.digitalObject());
    }

    @Test
    public void throwsExceptionWhenDigitalObjectDoesNotExist() {
      String id = "nonExistentId";
      org.junit.jupiter.api.Assertions.assertThrows(
          DigitalObjectNotFoundException.class, () -> digitalObjectService.findById(id)
      );
    }

    @Test
    public void returnsDigitalObjectWithExpectedProperties(){
      DigitalObject foundObject = digitalObjectService.findById(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundObject.getFunder()).isEqualTo(testDataSet.digitalObject().getFunder());
      Assertions.assertThat(foundObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundObject.getObjectType()).isEqualTo(testDataSet.digitalObject().getObjectType());
      Assertions.assertThat(foundObject.getPublisher()).isEqualTo(testDataSet.digitalObject().getPublisher());
      Assertions.assertThat(foundObject.getProject()).isEqualTo(testDataSet.digitalObject().getProject());
      Assertions.assertThat(foundObject.getBaseMetadata()).isEqualTo(testDataSet.digitalObject().getBaseMetadata());
      Assertions.assertThat(foundObject.getMainResource()).isEqualTo(testDataSet.digitalObject().getMainResource());
      // cannot be equal is being assigned by the database
      Assertions.assertThat(foundObject.getModified()).isNotEqualTo(testDataSet.digitalObject().getModified());
      Assertions.assertThat(foundObject.getCreated()).isNotEqualTo(testDataSet.digitalObject().getCreated());

      Assertions.assertThat(
          foundObject.getTags()
      ).containsAll(
          testDataSet.digitalObject().getTags()
      );

    }

  }

  @Nested
  public class FindAllByProjectAbbrWithOptionalParameters {

    @Test
    public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      testDataBuilder.removeAllExceptProjects(testDataSet);
      PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(), Optional.empty(), Pageable.unpaged()
      );
      Assertions.assertThat(result.getContent()).isEmpty();
    }

    @Test
    public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {

      PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(),
          Optional.of(testDataSet.digitalObject().getObjectType()),
          Pageable.unpaged()
      );

      Assertions.assertThat(result.getContent()).isNotEmpty();
      Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(
          testDataSet.digitalObject().getId()
      );

    }

    @Test
    public void throwsExceptionWhenProjectDoesNotExist() {
      String projectAbbr = "nonExistentProject";

      Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged()))
          .isInstanceOf(ProjectNotFoundException.class);
    }
  }

  @Nested
  public class Delete {

    @Test
    public void deletesDigitalObject() {
      digitalObjectService.delete(testDataSet.digitalObject());
      Assertions.assertThatThrownBy(() -> digitalObjectService.findById(
              testDataSet.digitalObject().getId())
          )
          .isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    public void deletesChildDatastreamsWithFileContent() {
      digitalObjectService.delete(testDataSet.digitalObject());
      Assertions.assertThat(datastreamRepository.existsById(testDataSet.mainDatastream().deriveDatastreamId())).isFalse();
      Assertions.assertThat(datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId())).isFalse();
    }

    @Test
    public void deletesReferencedDublinCoreEntries(){
      digitalObjectService.delete(
          testDataSet.digitalObject()
      );
      Assertions.assertThat(
          dublinCoreEntryRepository.existsById(testDataSet.dublinCoreEntry().getId())
      ).isFalse();

    }

    @Test
    public void projectContentIsUpdatedWhenDigitalObjectIsDeleted() {

      Date projectContentLastModifiedBeforeDelete = testDataSet.project().getContentLastModified();

      // this is a side effect of the delete operation, but we want to ensure that it works
      digitalObjectService.delete(testDataSet.digitalObject());

      var updatedProject = projectRepository.findById(testDataSet.project().getProjectAbbr())
          .orElseThrow(() ->  new ProjectNotFoundException(testDataSet.project().getProjectAbbr()));

      Date projectContentLastModifedAfterDelete = updatedProject.getContentLastModified();

      Assertions.assertThat(projectContentLastModifedAfterDelete)
          .isNotNull()
          .isAfter(projectContentLastModifiedBeforeDelete);

    }

  }


  @Nested
  public class FindDigitalObjectCompactDTOById {

    @Test
    @Transactional
    public void containsExpectedDublinCoreEntry(){

      var foundDigitalObject = digitalObjectService.findDigitalObjectCompactDTOById(
          testDataSet.digitalObject().getId()
      );

      Assertions.assertThat(foundDigitalObject)
          .isNotNull();

      var entries = foundDigitalObject.getDublinCore();

      Assertions.assertThat(entries)
          .isNotEmpty()
          .hasSize(1)
          .containsKey(testDataSet.dublinCoreEntry().getName());

      var testElementEntries = entries.get(testDataSet.dublinCoreEntry().getName());
      Assertions.assertThat(testElementEntries)
          .anySatisfy(entry -> {
            Assertions.assertThat(entry.language()).isEqualTo(testDataSet.dublinCoreEntry().getLanguage());
            Assertions.assertThat(entry.value()).isEqualTo(testDataSet.dublinCoreEntry().getValue());
          });
    }

  }


  @Nested
  public class DublinCoreFulltextSearch {

    Project additionalProject;

    @BeforeEach
    public void setup(){

      // 1 object belongs to a different project
      additionalProject =  testDataBuilder.addRandomProject(testDataSet);


      List<DigitalObject> digitalObjects = List.of(
          TestDigitalObject.generate("test", "test.foo"),
          TestDigitalObject.generate("test", "test.bar"),
          TestDigitalObject.generate("test", "test.baz"),
          // belongs to a different project
          TestDigitalObject.generate(additionalProject.getProjectAbbr(), additionalProject.getProjectAbbr() + ".peter")
      );

      digitalObjectRepository.saveAll(digitalObjects);

      List<DublinCoreEntry> dublinCoreEntries = List.of(
          TestDublinCoreEntry.generate(digitalObjects.get(0).getId()),
          TestDublinCoreEntry.generate(digitalObjects.get(1).getId()),
          TestDublinCoreEntry.generate(digitalObjects.get(2).getId()),
          TestDublinCoreEntry.generate(additionalProject.getProjectAbbr(), digitalObjects.get(3).getId())
      );

      dublinCoreEntryRepository.saveAll(dublinCoreEntries);

    }

  }


  @Nested
  public class SearchByDublinCoreCriteria {

    @Test
    @Transactional
    public void findsExpectedDigitalObject(){

      var dcFilters = Map.of(
          testDataSet.dublinCoreEntry().getName(),
          List.of(testDataSet.dublinCoreEntry().getValue())
      );

      var foundObjects = digitalObjectService.searchDigitalObjectsByDublinCoreCriteria(
          MultiValueMap.fromMultiValue(dcFilters),
          Set.of(testDataSet.project().getProjectAbbr()),
          DigitalObjectDublinCoreSpecification.SearchMode.FULLTEXT,
          PageRequest.of(0,100)
      );

      Assertions.assertThat(foundObjects.getContent())
          .isNotNull()
          .isNotEmpty()
          .hasSize(1)
          .allSatisfy(digitalObject -> {
            Assertions.assertThat(digitalObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
            Assertions.assertThat(digitalObject.getProjectAbbr()).isEqualTo(testDataSet.project().getProjectAbbr());
          });

    }
  }

  @Nested
  public class FindAllIdsByProjectAbbr {

    @Test
    public void returnsExpectedDigitalObjectIds(){

      // adding two additional digital objects to the test data set
      DigitalObject digitalObject1 = testDataBuilder.addRandomObject(testDataSet);
      DigitalObject digitalObject2 = testDataBuilder.addRandomObject(testDataSet);
      final int EXPECTED_OBJECT_COUNT = 3;

      var paginatedIds = digitalObjectService.findAllIdsByProjectAbbr(
          testDataSet.project().getProjectAbbr(), PageRequest.of(0,1000)
      );

      Assertions
          .assertThat(paginatedIds)
          .isNotNull();

      Assertions.assertThat(paginatedIds.getContent())
          .hasSize(EXPECTED_OBJECT_COUNT)
          .contains(digitalObject1.getId(), digitalObject2.getId());

    }

  }

  @Nested
  public class FindAllByProjectAndTags {

    @Test
    public void returnsDigitalObjectsWithExpectedTags(){

      var TAG_TO_FIND = TestDigitalObject.getTags();

      // adding two additional digital objects to the test data set
      var foundObjects = digitalObjectService.findAllByProjectAndTags(
          testDataSet.project().getProjectAbbr(),
          TAG_TO_FIND,
          PageRequest.of(0,100)
      );

      Assertions.assertThat(foundObjects.getPagination().getTotalElements())
          .isGreaterThan(0);

      var firstFoundObject = foundObjects.getContent().get(0);

      Assertions.assertThat(firstFoundObject.getTags())
          .isNotNull()
          .isNotEmpty()
          .containsAll(TAG_TO_FIND);

    }

  }

}
