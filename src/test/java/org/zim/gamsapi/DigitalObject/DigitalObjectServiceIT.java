package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectServiceIT extends IntegrationTest {

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IDigitalObjectService digitalObjectService;

  @Autowired
  IDublinCoreEntryRepository dublinCoreEntryRepository;

  Project testProject;

  MetadataBaseEntity testMetadataBaseEntity = TestMetadataBaseEntity.generate();


  // Deactivates the auditing process.
  @MockBean
  private AuditingHandler auditingHandler;

  @BeforeEach
  public void setup(){
    testProject = ProjectBuilder
      .builder()
      .projectAbbr(TestProject.PROJECT_ABBR.getValue())
      .build();

    projectRepository.save(testProject);

  }

  @Nested
  public class Save {


    @Test
    public void successFullySavesSimpleDigitalObject() {
      // given

      DigitalObject digitalObject = TestDigitalObject.generate();

      // when
      DigitalObject savedDigitalObject = digitalObjectService.save(digitalObject);

      // then
      Assertions.assertThat(savedDigitalObject).isNotNull();
      Assertions.assertThat(savedDigitalObject.getId()).isNotNull();
      Assertions.assertThat(savedDigitalObject.getProject()).isEqualTo(testProject);
      // considered equal because of same id
      Assertions.assertThat(savedDigitalObject).isEqualTo(digitalObject);

    }

  }

  @Nested
  public class FindAllByProjectAbbr {

    @Test
    public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      String projectAbbr = "nonexist";
      Project project = ProjectBuilder.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);

      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(projectAbbr, Pageable.unpaged());

      Assertions.assertThat(result).isEmpty();

    }

    @Test
    public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {
      Project project = TestProject.generate();
      projectRepository.save(project);

      DigitalObject digitalObject = TestDigitalObject.generate(project.getProjectAbbr());
      digitalObjectRepository.save(digitalObject);

      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(project.getProjectAbbr(), Pageable.unpaged());

      Assertions.assertThat(result).isNotEmpty();
      Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(digitalObject.getId());

    }

    @Test
    public void throwsExceptionWhenProjectDoesNotExist() {
      String projectAbbr = "nonExistentProject";

      Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Pageable.unpaged()))
          .isInstanceOf(ProjectNotFoundException.class);
    }
  }

  @Nested
  public class FindById {

    @Test
    public void returnsDigitalObjectWhenItExists() {

      Project project = TestProject.generate();
      projectRepository.save(project);

      DigitalObject digitalObject = TestDigitalObject.generate(project.getProjectAbbr());

      digitalObjectRepository.save(digitalObject);

      DigitalObject result = digitalObjectService.findById(digitalObject.getId());

      Assertions.assertThat(result).isEqualTo(digitalObject);

    }

    @Test
    public void throwsExceptionWhenDigitalObjectDoesNotExist() {
      String id = "nonExistentId";
      org.junit.jupiter.api.Assertions.assertThrows(DigitalObjectNotFoundException.class, () -> {
        digitalObjectService.findById(id);
      });
    }

    @Test
    public void returnsDigitalObjectWithExpectedProperties(){

      Project project = TestProject.generate();
      Project savedProject = projectRepository.save(project);

      DigitalObject digitalObject = TestDigitalObject.generate(savedProject.getProjectAbbr());
      DigitalObject savedDigitalObject = digitalObjectRepository.save(digitalObject);

      DigitalObject foundObject = digitalObjectService.findById(savedDigitalObject.getId());
      Assertions.assertThat(foundObject.getFunder()).isEqualTo(digitalObject.getFunder());
      Assertions.assertThat(foundObject.getId()).isEqualTo(digitalObject.getId());
      Assertions.assertThat(foundObject.getObjectType()).isEqualTo(digitalObject.getObjectType());
      Assertions.assertThat(foundObject.getPublisher()).isEqualTo(digitalObject.getPublisher());
      Assertions.assertThat(foundObject.getProject()).isEqualTo(digitalObject.getProject());
      Assertions.assertThat(foundObject.getBaseMetadata()).isEqualTo(digitalObject.getBaseMetadata());
      Assertions.assertThat(foundObject.getMainResource()).isEqualTo(digitalObject.getMainResource());
      // cannot be equal is being assigned by the database
      Assertions.assertThat(foundObject.getModified()).isNotEqualTo(digitalObject.getModified());
      Assertions.assertThat(foundObject.getCreated()).isNotEqualTo(digitalObject.getCreated());

    }

  }

  @Nested
  public class FindAllByProjectAbbrWithOptionalParameters {

    @Test
    public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      String projectAbbr = "nonexist";
      Project project = ProjectBuilder.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);
      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged());
      Assertions.assertThat(result).isEmpty();

    }

    @Test
    public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {
      String projectAbbr = "project";
      Project project = ProjectBuilder.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id(projectAbbr + ".testpid")
          .project(project)
          .publisher("testPublisher")
          .objectType("testType")
          .baseMetadata(testMetadataBaseEntity)
          .build();
      digitalObjectRepository.save(digitalObject);

      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.of("testType"), Pageable.unpaged());

      Assertions.assertThat(result).isNotEmpty();
      Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(digitalObject.getId());

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
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);

      digitalObjectService.delete(digitalObject);

      Assertions.assertThatThrownBy(() -> digitalObjectService.findById(digitalObject.getId()))
          .isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    public void deletesChildDatastreamsWithFileContent() {

      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      final Datastream TEST_DATASTREAM = TestDatastream.generate(digitalObject);

      datastreamRepository.save(TEST_DATASTREAM);
      datastreamContentRepository.save(new byte[0], TEST_DATASTREAM.deriveDatastreamId());

      digitalObjectService.delete(digitalObject);

      Assertions.assertThat(datastreamRepository.existsById(TEST_DATASTREAM.deriveDatastreamId())).isFalse();
      Assertions.assertThat(datastreamContentRepository.exists(TEST_DATASTREAM.deriveDatastreamId())).isFalse();


    }

    @Test
    public void deletesReferencedDublinCoreEntries(){

      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      final DublinCoreEntry TEST_DUBLIN_CORE_ENTRY = TestDublinCoreEntry.generate(digitalObject.getId());

      dublinCoreEntryRepository.save(TEST_DUBLIN_CORE_ENTRY);

      digitalObjectService.delete(digitalObject);

      Assertions.assertThat(dublinCoreEntryRepository.existsById(TEST_DUBLIN_CORE_ENTRY.getId())).isFalse();

    }

  }


}
