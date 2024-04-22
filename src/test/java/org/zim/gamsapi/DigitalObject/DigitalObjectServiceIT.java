package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestProject;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectServiceIT extends IntegrationTest {

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDigitalObjectService digitalObjectService;

  Project testProject;

  @BeforeAll
  public void setup(){

    testProject = Project
      .builder()
      .projectAbbr(TestProject.PROJECT_ABBR.getValue())
      .build();

    projectRepository.save(testProject);

  }

  @AfterAll
  public void tearDown(){

    projectRepository.deleteAll();
    digitalObjectRepository.deleteAll();

    Assertions.assertThat(digitalObjectRepository.findAll())
      .isNotNull()
      .isEmpty();

    Assertions.assertThat(projectRepository.findAll())
        .isNotNull()
        .isEmpty();
  }

  @Nested
  public class Save {


    @Test
    public void successFullySavesSimpleDigitalObject() {
      // given

      DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("testPid")
        .project(testProject)
        .build();

      // when
      DigitalObject savedDigitalObject = digitalObjectService.save(digitalObject);

      // then
      Assertions.assertThat(savedDigitalObject).isNotNull();
      Assertions.assertThat(savedDigitalObject.getId()).isNotNull();
      Assertions.assertThat(savedDigitalObject.getProject()).isEqualTo(testProject);
      // parent was not set
      Assertions.assertThat(savedDigitalObject.getParent()).isNull();
      // considered equal because of same id
      Assertions.assertThat(savedDigitalObject).isEqualTo(digitalObject);

      // cleanup
      digitalObjectRepository.delete(digitalObject);
    }

    @Test
    public void successFullySavesDigitalObjectWithParent() {
      // given

      DigitalObject parent = new DigitalObjectBuilder()
        .id("parentPid")
        .project(testProject)
        .build();

      digitalObjectRepository.save(parent);

      DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("testPid")
        .project(testProject)
        .parent(parent)
        .build();

      // when
      DigitalObject savedDigitalObject = digitalObjectService.save(digitalObject);

      // then
      Assertions.assertThat(savedDigitalObject).isNotNull();
      Assertions.assertThat(savedDigitalObject.getId()).isNotNull();
      Assertions.assertThat(savedDigitalObject.getProject()).isEqualTo(testProject);
      Assertions.assertThat(savedDigitalObject.getParent()).isEqualTo(parent);
      // considered equal because of same id
      Assertions.assertThat(savedDigitalObject).isEqualTo(digitalObject);

      // cleanup
      digitalObjectRepository.delete(digitalObject);
      digitalObjectRepository.delete(parent);
    }


    @Test
    public void throwsExceptionWhenParentDoesNotExist() {
      // given

      DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("testPid")
        .project(testProject)
        .parent(new DigitalObjectBuilder().id("nonExistentParentPid").project("12345").build())
        .build();

      // when
      // then
      Assertions.assertThatThrownBy(() -> digitalObjectService.save(digitalObject))
        .isInstanceOf(DigitalObjectNotFoundException.class);

    }
  }

  @Nested
  public class FindAllByProjectAbbr {

    @Test
    public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      String projectAbbr = "nonExistentProject";
      Project project = Project.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);

      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(projectAbbr, Pageable.unpaged());

      Assertions.assertThat(result).isEmpty();

      projectRepository.delete(project);
    }

    @Test
    public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {
      String projectAbbr = "existingProject";
      Project project = Project.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(project)
          .build();
      digitalObjectRepository.save(digitalObject);

      Page<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(projectAbbr, Pageable.unpaged());

      Assertions.assertThat(result).isNotEmpty();
      Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(digitalObject.getId());

      digitalObjectRepository.delete(digitalObject);
      projectRepository.delete(project);
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

      Project project = Project.builder().projectAbbr("random").build();
      projectRepository.save(project);

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(project)
          .build();

      digitalObjectRepository.save(digitalObject);

      DigitalObject result = digitalObjectService.findById(digitalObject.getId());

      Assertions.assertThat(result).isEqualTo(digitalObject);

      digitalObjectRepository.delete(digitalObject);
      projectRepository.delete(project);
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
  public class DeleteAllForProject {

    @Test
    public void deletesAllDigitalObjectsForProject() {
      String projectAbbr = "existingProject";
      Project project = Project.builder().projectAbbr(projectAbbr).build();
      projectRepository.save(project);

      DigitalObject digitalObject1 = new DigitalObjectBuilder()
          .id("testPid1")
          .project(project)
          .build();
      digitalObjectRepository.save(digitalObject1);

      DigitalObject digitalObject2 = new DigitalObjectBuilder()
          .id("testPid2")
          .project(project)
          .build();
      digitalObjectRepository.save(digitalObject2);

      digitalObjectService.deleteAllForProject(project);

      Assertions.assertThat(digitalObjectRepository.findAll()).isEmpty();

      projectRepository.delete(project);
    }

    @Test
    public void doesNotDeleteDigitalObjectsForOtherProjects() {
      String projectAbbr1 = "existingProject1";
      Project project1 = Project.builder().projectAbbr(projectAbbr1).build();
      projectRepository.save(project1);

      String projectAbbr2 = "existingProject2";
      Project project2 = Project.builder().projectAbbr(projectAbbr2).build();
      projectRepository.save(project2);

      DigitalObject digitalObject1 = new DigitalObjectBuilder()
          .id("testPid1")
          .project(project1)
          .build();
      digitalObjectRepository.save(digitalObject1);

      DigitalObject digitalObject2 = new DigitalObjectBuilder()
          .id("testPid2")
          .project(project2)
          .build();
      digitalObjectRepository.save(digitalObject2);

      digitalObjectService.deleteAllForProject(project1);

      Assertions.assertThat(digitalObjectRepository.findAll()).containsOnly(digitalObject2);

      digitalObjectRepository.delete(digitalObject2);
      projectRepository.delete(project1);
      projectRepository.delete(project2);
    }
  }

  @Nested
  public class AssignParentObject {

    @Test
    public void assignsParentObjectSuccessfully() {
      DigitalObject parent = new DigitalObjectBuilder()
          .id("parentPid")
          .project(testProject)
          .build();
      digitalObjectRepository.save(parent);

      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(testProject)
          .build();
      digitalObjectRepository.save(digitalObject);

      digitalObjectService.assignParentObject(digitalObject, parent);

      DigitalObject result = digitalObjectService.findById(digitalObject.getId());

      Assertions.assertThat(result.getParent()).isEqualTo(parent);

      digitalObjectRepository.delete(digitalObject);
      digitalObjectRepository.delete(parent);
    }

    @Test
    public void throwsExceptionWhenParentObjectDoesNotExist() {
      DigitalObject digitalObject = new DigitalObjectBuilder()
          .id("testPid")
          .project(testProject)
          .build();
      digitalObjectRepository.save(digitalObject);

      DigitalObject nonExistentParent = new DigitalObjectBuilder()
          .id("nonExistentParentPid")
          .project(testProject)
          .build();

      org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
        digitalObjectService.assignParentObject(digitalObject, nonExistentParent);
      });

      digitalObjectRepository.delete(digitalObject);
    }
  }





}
