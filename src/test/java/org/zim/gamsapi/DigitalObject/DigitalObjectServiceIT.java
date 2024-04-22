package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
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
        .isInstanceOf(DigitalObjectNotFoundException.class)
        .hasMessageContaining("Cannot find contained parent object nonExistentParentPid in digital object testPid");

    }





  }



}
