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
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestDublinCoreEntry;
import org.zim.gamsapi.enums.TestProject;

import java.util.List;
import java.util.Set;

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

  /**
   * Tests for saving DublinCoreEntries.
   */
  @Nested
  public class Save {

    final DigitalObject TEST_OBJECT = TestDigitalObject.generate();
    final DublinCoreEntry TEST_DC_ENTRY = TestDublinCoreEntry.generate(TEST_OBJECT.getId());

    @Test
    public void afterSavingThereAreMoreThan0DublinCoreEntries() {
      // need to first save the digital object
      digitalObjectRepository.save(TEST_OBJECT);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(TEST_OBJECT.getId()));
      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);
    }

    @Test
    public void savingFailsIfObjectDoesntExist() {
      Assertions.assertThatThrownBy(() -> {
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate("nix"));
      }).isInstanceOf(Exception.class);
    }

    @Test
    public void savesEntryWithExpectedLanguage() {
      digitalObjectRepository.save(TEST_OBJECT);
      dublinCoreEntryRepository.save(TEST_DC_ENTRY);

      var foundDcEntry = dublinCoreEntryRepository.findById(TEST_DC_ENTRY.getId());
      Assertions.assertThat(foundDcEntry).isPresent();

      Assertions.assertThat(foundDcEntry.get().getLanguage())
              .isEqualTo(TEST_DC_ENTRY.getLanguage());

    }

    @Test
    public void savesDublinCoreEntryWithLanguageNull(){
      TEST_DC_ENTRY.setLanguage(null);
      digitalObjectRepository.save(TEST_OBJECT);
      dublinCoreEntryRepository.save(TEST_DC_ENTRY);
      var foundDcEntry = dublinCoreEntryRepository.findById(TEST_DC_ENTRY.getId());
      Assertions.assertThat(foundDcEntry).isPresent();
      Assertions.assertThat(foundDcEntry.get().getLanguage()).isNull();
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

      var dublinCoreEntries = dublinCoreEntryRepository.findByDigitalObjectAndName(
          digitalObject, DC_TITLE
      );

      Assertions.assertThat(dublinCoreEntries)
          .isNotEmpty()
          // test data contains two subject entries
          .hasSize(1);

    }



  }

  @Nested
  public class FindDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues {

    @Test
    public void findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementFixedValuesIsNotEmpty() {
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
      var foundObjects = dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          TestDublinCoreEntry.NAME.getValue(),
          List.of(TestDublinCoreEntry.VALUE.getValue()),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(foundObjects)
          .isNotEmpty()
          .hasSize(1);
    }

    /**
     * Test if the method returns a match even when just one of the given values is found.
     */
    @Test
    public void matchesIfOneDCValueWasFound() {

      final String TEST_DC_FIELD = TestDublinCoreEntry.NAME.getValue();
      final List<String> TEST_DUBLIN_CORE_VALUES = List.of(
          // first fields don't exist in test data
          "foo",
          "bar",
          "hudri",
          TestDublinCoreEntry.VALUE.getValue()
      );

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
      var foundObjects = dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          TEST_DC_FIELD,
          TEST_DUBLIN_CORE_VALUES,
          PageRequest.of(0, 10)
      );

      // expect one match even though "wrong" test values were given
      Assertions.assertThat(foundObjects)
          .isNotEmpty()
          .hasSize(1);
    }

    @Test
    public void findDigitalObjectListItemViewsByProjectAbbrAndDublinCoreElementFixedValuesIsEmpty_whenNoMatch() {
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
      var foundObjects = dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "foo",
          List.of("bar"),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(foundObjects).isEmpty();
    }

    @Test
    public void findByDublinCoreFixedValuesOverTwoProjects(){
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

      // save another object to a different project
      Project additionalProject = TestProject.generate("foo");
      projectRepository.save(additionalProject);
      DigitalObject digitalObject2 = TestDigitalObject.generate(
          additionalProject.getProjectAbbr(), additionalProject.getProjectAbbr() + ".bar"
      );

      digitalObjectRepository.save(digitalObject2);
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(
          additionalProject.getProjectAbbr(), digitalObject2.getId())
      );

      var foundObjects = dublinCoreEntryRepository.findDigitalObjectListItemViewsByProjectAbbrsAndDublinCoreElementFixedValues(
          // search across both projects
          Set.of(TestProject.PROJECT_ABBR.getValue(), additionalProject.getProjectAbbr()),
          TestDublinCoreEntry.NAME.getValue(),
          List.of(TestDublinCoreEntry.VALUE.getValue()),
          PageRequest.of(0, 10)
      );

      Assertions.assertThat(foundObjects)
          .isNotEmpty()
          .hasSize(2)
      ;

    }

    @Nested
    public class FindDigitalObjectsByFulltext {

      @Test
      public void fulltextDCSearchShouldReturnSavedObject(){

        // take an arbitrary part of the test data
        final String FULLTEXT_SEARCH_VALUE = TestDublinCoreEntry.VALUE.getValue().substring(
            0,
            4
        );

        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObjectRepository.save(digitalObject);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

        var foundObjects = dublinCoreEntryRepository.findDigitalObjectsByDCFulltext(
            Set.of(TestProject.PROJECT_ABBR.getValue()),
            FULLTEXT_SEARCH_VALUE,
            PageRequest.of(0, 10)
        );

        Assertions.assertThat(foundObjects)
            .isNotEmpty()
            .hasSize(1);
      }

      @Test
      public void fulltextDCSearchReturnsNothingWhenExpected(){

        final String FULLTEXT_SEARCH_VALUE = "DEFINITELY__01239_NO_öä_MATCH";

        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObjectRepository.save(digitalObject);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
        var foundObjects = dublinCoreEntryRepository.findDigitalObjectsByDCFulltext(
            Set.of(TestProject.PROJECT_ABBR.getValue()),
            FULLTEXT_SEARCH_VALUE,
            PageRequest.of(0, 10)
        );

        Assertions.assertThat(foundObjects)
            .isEmpty();

      }

      @Test
      public void fulltextDCSearchFindsValuesOverTwoProjects(){

        // take an arbitrary part of the test data
        final String FULLTEXT_SEARCH_VALUE = TestDublinCoreEntry.VALUE.getValue().substring(0, 4);

        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObjectRepository.save(digitalObject);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

        // save another object to a different project
        Project additionalProject = TestProject.generate("foo");
        projectRepository.save(additionalProject);
        DigitalObject digitalObject2 = TestDigitalObject.generate(
            additionalProject.getProjectAbbr(), additionalProject.getProjectAbbr() + ".bar"
        );

        digitalObjectRepository.save(digitalObject2);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(
            additionalProject.getProjectAbbr(), digitalObject2.getId())
        );

        var foundObjects = dublinCoreEntryRepository.findDigitalObjectsByDCFulltext(
            // search across both projects
            Set.of(TestProject.PROJECT_ABBR.getValue(), additionalProject.getProjectAbbr()),
            FULLTEXT_SEARCH_VALUE,
            PageRequest.of(0, 10)
        );

        Assertions.assertThat(foundObjects)
            .isNotEmpty()
            .hasSize(2)
        ;

      }

    }


    @Nested
    public class FindDigitalObjectsByFulltextOnSpecificElements {

      @Test
      public void findsExpectedDigitalObject(){

        // take an arbitrary part of the test data
        final String TEST_FULLTEXT_SEARCH_VALUE = TestDublinCoreEntry.VALUE.getValue().substring(
            0,
            4
        );

        // field to search for according to test data
        final Set<String> TEST_DC_FULLTEXT_SEARCH_FIELDS = Set.of(
            TestDublinCoreEntry.NAME.getValue(),
            "foo"
        );

        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObjectRepository.save(digitalObject);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

        var foundObjects = dublinCoreEntryRepository.findDigitalObjectsByFulltextOnSpecificElements(
            Set.of(TestProject.PROJECT_ABBR.getValue()),
            TEST_DC_FULLTEXT_SEARCH_FIELDS,
            TEST_FULLTEXT_SEARCH_VALUE,
            PageRequest.of(0, 10)
        );

        Assertions.assertThat(foundObjects)
            .isNotEmpty()
            .hasSize(1);

      }


      @Test
      public void fulltextDCSearchReturnsNothingWhenDCElementsValueDontMatch(){

        // take an arbitrary part of the test data
        final String TEST_FULLTEXT_SEARCH_VALUE = TestDublinCoreEntry.VALUE.getValue().substring(
            0,
            4
        );

        // set search field to nonsense value
        final Set<String> TEST_DC_FULLTEXT_SEARCH_FIELDS = Set.of(
            "foo","bar", "hudri"
        );

        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObjectRepository.save(digitalObject);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));
        var foundObjects = dublinCoreEntryRepository.findDigitalObjectsByFulltextOnSpecificElements(
            Set.of(TestProject.PROJECT_ABBR.getValue()),
            TEST_DC_FULLTEXT_SEARCH_FIELDS,
            TEST_FULLTEXT_SEARCH_VALUE,
            PageRequest.of(0, 10)
        );

        Assertions.assertThat(foundObjects)
            .isEmpty();

      }

      @Test
      public void fulltextDCSpecifiedSearchFindsValuesOverTwoProjects(){

        // take an arbitrary part of the test data
        final String TEST_FULLTEXT_SEARCH_VALUE = TestDublinCoreEntry.VALUE.getValue().substring(0, 4);

        // field to search for according to test data
        final Set<String> TEST_DC_FULLTEXT_SEARCH_FIELDS = Set.of(
            TestDublinCoreEntry.NAME.getValue()
        );

        DigitalObject digitalObject = TestDigitalObject.generate();
        digitalObjectRepository.save(digitalObject);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(digitalObject.getId()));

        // save another object to a different project
        Project additionalProject = TestProject.generate("foo");
        projectRepository.save(additionalProject);
        DigitalObject digitalObject2 = TestDigitalObject.generate(
            additionalProject.getProjectAbbr(), additionalProject.getProjectAbbr() + ".bar"
        );

        digitalObjectRepository.save(digitalObject2);
        dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(
            additionalProject.getProjectAbbr(), digitalObject2.getId())
        );

        var foundObjects = dublinCoreEntryRepository.findDigitalObjectsByFulltextOnSpecificElements(
            // search across both projects
            Set.of(TestProject.PROJECT_ABBR.getValue(), additionalProject.getProjectAbbr()),
            TEST_DC_FULLTEXT_SEARCH_FIELDS,
            TEST_FULLTEXT_SEARCH_VALUE,
            PageRequest.of(0, 10)
        );

        Assertions.assertThat(foundObjects)
            .isNotEmpty()
            .hasSize(2)
        ;

      }


    }


  }

}
