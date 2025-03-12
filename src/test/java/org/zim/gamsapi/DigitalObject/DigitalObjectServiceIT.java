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
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import org.zim.gamsapi.enums.TestProject;

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
      String projectAbbr = "existing";
      Project project = ProjectBuilder.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);

      DigitalObject digitalObject = TestDigitalObject.generate(project.getProjectAbbr());
      digitalObjectRepository.save(digitalObject);

      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(projectAbbr, Pageable.unpaged());

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

      Project project = ProjectBuilder.builder().projectAbbr("random").build();
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


  }


}
