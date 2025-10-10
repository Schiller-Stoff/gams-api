package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.TestUtilities.*;

import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DublinCoreEntryRepositoryIT extends IntegrationTest {

  @Autowired
  private IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IProjectRepository projectRepository;

  @MockitoBean
  private AuditingHandler auditingHandler;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @BeforeEach
  public void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }


  @Nested
  public class Delete {

    @Test
    public void deleteAllWorksAsExpected_dublinCoreEntryShouldBeEmpty(){
      dublinCoreEntryRepository.deleteAll();
      Assertions.assertThat(dublinCoreEntryRepository.count()).isEqualTo(0);
    }

    @Test
    public void deletionOfDublinCoreEntryWorksAsExpected_dublinCoreEntryShouldBeEmpty(){
      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);
      dublinCoreEntryRepository.delete(testDataSet.dublinCoreEntry());
      Assertions.assertThat(dublinCoreEntryRepository.count()).isEqualTo(0);
    }

    @Test
    public void hardDeletionOfDigitalObjectDoesThrowExceptionIfDublinCoreEntryStillExists(){
      org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
        digitalObjectRepository.delete(testDataSet.digitalObject());
      });

    }

    @Test
    @Transactional
    public void deletionOfAllDublinCoreEntriesForObjectDeletesCreatedEntry(){
      Assertions.assertThat(digitalObjectRepository.count()).isGreaterThan(0);
      Assertions.assertThat(dublinCoreEntryRepository.count()).isGreaterThan(0);
      dublinCoreEntryRepository.deleteAllByDigitalObject(testDataSet.digitalObject());
      Assertions.assertThat(dublinCoreEntryRepository.count()).isEqualTo(0);
    }

  }

  /**
   * Tests for saving DublinCoreEntries.
   */
  @Nested
  public class Save {

    final DublinCoreEntry TEST_DC_ENTRY = TestDublinCoreEntry.generate(
        testDataSet.digitalObject().getId()
    );

    @Test
    public void afterSavingThereAreMoreThan0DublinCoreEntries() {
      // need to first save the digital object
      dublinCoreEntryRepository.save(TestDublinCoreEntry.generate(testDataSet.digitalObject().getId()));
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
      dublinCoreEntryRepository.save(TEST_DC_ENTRY);

      var foundDcEntry = dublinCoreEntryRepository.findById(TEST_DC_ENTRY.getId());
      Assertions.assertThat(foundDcEntry).isPresent();

      Assertions.assertThat(foundDcEntry.get().getLanguage())
              .isEqualTo(TEST_DC_ENTRY.getLanguage());

    }

    @Test
    public void savesDublinCoreEntryWithLanguageNull(){
      TEST_DC_ENTRY.setLanguage(null);
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

      Assertions.assertThat(dublinCoreEntryRepository.
          findDigitalObjectsByDublinCoreElementValue(
              testDataSet.dublinCoreEntry().getName(),
              testDataSet.dublinCoreEntry().getValue()
      )).isNotEmpty();


    }

    @Test
    public void returnsEmptyListIfNoDigitalObjectsMatch() {
      Assertions.assertThat(dublinCoreEntryRepository.findDigitalObjectsByDublinCoreElementValue(
          "foo",
          testDataSet.dublinCoreEntry().getValue()
      )).isEmpty();
    }

    @Test
    public void mayFindExpectedDublinCoreTitlesForGivenDigitalObject(){
      var dublinCoreEntries = dublinCoreEntryRepository.findByDigitalObjectAndName(
          testDataSet.digitalObject(), testDataSet.dublinCoreEntry().getName()
      );
      Assertions.assertThat(dublinCoreEntries)
          .isNotEmpty()
          .hasSize(1);
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
