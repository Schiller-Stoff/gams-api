package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestDublinCoreEntry;
import org.zim.gamsapi.enums.TestProject;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DublinCoreEntryRepositoryIT extends IntegrationTest {

  @Autowired
  private IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IProjectRepository projectRepository;

  @MockBean
  private AuditingHandler auditingHandler;

  @BeforeEach
  public void setup() {
    projectRepository.save(TestProject.generate());
  }


  @Nested
  public class Delete {

    @Test
    public void deleteAllWorksAsExpected_dublinCoreEntryShouldBeEmpty(){

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);

      dublinCoreEntryRepository.deleteAll();

      Assertions.assertThat(dublinCoreEntryRepository.count()).isEqualTo(0);

    }

    @Test
    public void deletionOfDublinCoreEntryWorksAsExpected_dublinCoreEntryShouldBeEmpty(){

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      DublinCoreEntry dublinCoreEntry = TestDublinCoreEntry.generate(digitalObject.getId());
      dublinCoreEntryRepository.save(dublinCoreEntry);

      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);

      dublinCoreEntryRepository.delete(dublinCoreEntry);

      Assertions.assertThat(dublinCoreEntryRepository.count()).isEqualTo(0);

    }

    @Test
    public void deletionOfdigitalObjectThrowsExceptionIfDublinCoreEntryStillExists(){

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      DublinCoreEntry dublinCoreEntry = TestDublinCoreEntry.generate(digitalObject.getId());
      dublinCoreEntryRepository.save(dublinCoreEntry);

      org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
        digitalObjectRepository.delete(digitalObject);
      });

    }

    @Test
    @Transactional
    public void deletionOfAllDublinCoreEntriesForObjectDeletesCreatedEntry(){

      DigitalObject digitalObject = TestDigitalObject.generate();
      DigitalObject savedObject =  digitalObjectRepository.save(digitalObject);
      DublinCoreEntry dublinCoreEntry = TestDublinCoreEntry.generate(savedObject);
      dublinCoreEntryRepository.save(dublinCoreEntry);

      Assertions.assertThat(digitalObjectRepository.count()).isGreaterThan(0);
      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);

      dublinCoreEntryRepository.deleteAllByDigitalObject(savedObject);
      Assertions.assertThat(dublinCoreEntryRepository.count()).isEqualTo(0);

    }

  }

  @Nested
  public class Save {

    @Test
    public void afterSavingThereAreMoreThan0DublinCoreEntries() {
      // need to first save the digital object
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);
    }

    @Test
    public void savingFailsIfObjectDoesntExist() {
      Assertions.assertThatThrownBy(() -> {
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate("nix"));
      }).isInstanceOf(Exception.class);
    }
  }


  @Nested
  public class FindingDigitalObjectsByDublinCore {

    @Test
    public void findDigitalObjectsByDublinCoreElementValueIsNotEmpty() {

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

      Assertions.assertThat(dublinCoreEntryRepository.findDigitalObjectsByDublinCoreElementValue(
          TestDublinCoreEntry.NAME.getValue(),
          TestDublinCoreEntry.VALUE.getValue()
      )).isNotEmpty();


    }

    @Test
    public void returnsEmptyListIfNoDigitalObjectsMatch() {
      Assertions.assertThat(dublinCoreEntryRepository.findDigitalObjectsByDublinCoreElementValue(
          "foo",
          TestDublinCoreEntry.VALUE.getValue()
      )).isEmpty();
    }

    @Test
    public void mayFindExpectedDublinCoreTitlesForGivenDigitalObject(){

      final String DC_TITLE = TestDublinCoreEntry.NAME.getValue();

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

      List<DublinCoreEntry> dublinCoreEntries = dublinCoreEntryRepository.findByDigitalObjectAndName(
          digitalObject, DC_TITLE
      );

      Assertions.assertThat(dublinCoreEntries)
          .isNotEmpty()
          // test data contains two subject entries
          .hasSize(1);

    }



  }


  @Nested
  public class FindDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementValue {

    @Test
    public void findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementValueIsNotEmpty() {
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
      var foundObjects = dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementValue(
          TestProject.PROJECT_ABBR.getValue(),
          TestDublinCoreEntry.NAME.getValue(),
          TestDublinCoreEntry.VALUE.getValue(),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(foundObjects).isNotEmpty();
    }

    @Test
    public void findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementValueIsEmpty_whenNoMatch() {
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
      var foundObjects = dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementValue(
          TestProject.PROJECT_ABBR.getValue(),
          "foo",
          "bar",
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(foundObjects).isEmpty();
    }

  }

}
