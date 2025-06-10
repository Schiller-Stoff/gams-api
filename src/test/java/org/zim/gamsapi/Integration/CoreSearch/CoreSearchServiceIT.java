package org.zim.gamsapi.Integration.CoreSearch;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Ingest.Ingest;
import org.zim.gamsapi.Ingest.IngestService;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestBag;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;
import java.io.File;
import java.io.IOException;

/**
 * Integration test for the ElasticSearch service.
 * This class extends the base integration test class to provide
 * a specific context for testing ElasticSearch functionalities.
 */
@Slf4j
public class CoreSearchServiceIT extends CoreSearchIntegrationTest {

  // disables auditing
  @MockBean
  private AuditingHandler auditingHandler;

  @Autowired
  private CoreSearchService coreSearchService;

  @Autowired
  private CoreSearchRepository coreSearchRepository;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IngestService ingestService;

  File bagFile;

  /**
   * Ingests the test bag before all tests and then
   * checks if the found entity has expected values.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class IngestResult {

    private CoreSearchEntity FOUND_ENTITY;

    @BeforeAll
    public void setup() throws IOException {
      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      Ingest ingest = new Ingest();
      ingest.setZippedBagItFolder(zippedBag);
      ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
      ingestService.ingest(ingest);

      // call index object
      coreSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(),
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      // verify existence in the repository
      Assertions.assertThat(
          coreSearchRepository.existsById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
      ).isTrue();

      FOUND_ENTITY = coreSearchRepository.findById(
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      ).orElseThrow();

    }

    /**
     * Checks if expected entity exists in elastic search.
     */
    @Test
    public void expectedObjectExists(){
      Assertions.assertThat(
          FOUND_ENTITY
      ).isNotNull();
    }

    @Test
    public void foundObjectHasExpectedId(){
      Assertions.assertThat(FOUND_ENTITY.getId())
          .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
    }

    @Test
    public void foundObjectHasExpectedTitle() {
      Assertions.assertThat(FOUND_ENTITY.getTitles())
          .isNotEmpty()
          .anySatisfy(title -> {
            Assertions.assertThat(title.getName()).isEqualTo("title");
            Assertions.assertThat(title.getValue()).isEqualTo("test-dc-title");
          });
    }

    @Test
    public void hasExpectedTitleWithLang() {
      Assertions.assertThat(FOUND_ENTITY.getTitles())
          .isNotEmpty()
          .anySatisfy(title -> {
            Assertions.assertThat(title.getName()).isEqualTo("title");
            Assertions.assertThat(title.getValue()).isEqualTo("test-dc-title-en");
            Assertions.assertThat(title.getLang()).isEqualTo("en");
          });
    }

    @Test
    public void doesNotContainDCElementsWithPrefix(){
      Assertions.assertThat(FOUND_ENTITY.getTitles())
          .noneMatch(dceLement -> dceLement.getName().contains(":"));
    }

    @Test
    public void hasExpectedDescriptionDCValues() {
      Assertions.assertThat(FOUND_ENTITY.getDescriptions())
          .isNotEmpty()
          .anySatisfy(dcElement -> {
            Assertions.assertThat(dcElement.getName()).isEqualTo("description");
            Assertions.assertThat(dcElement.getValue()).isEqualTo("test-dc-description");
          });
    }

    @Test
    public void hasExpectedMappedEnglishDCDescription() {
      Assertions.assertThat(FOUND_ENTITY.getDescriptions())
          .isNotEmpty()
          .anySatisfy(dcElement -> {
            Assertions.assertThat(dcElement.getName()).isEqualTo("description");
            Assertions.assertThat(dcElement.getValue()).isEqualTo("test-dc-description-en");
            Assertions.assertThat(dcElement.getLang()).isEqualTo("en");
          });
    }

  }



  @Nested
  public class IndexObject {

    @Test
    public void throwsExceptionWhenObjectNotFound() {

      final String NOT_EXISTENT_OBJECT_ID = "non-existing-id";

      // create a GAMS project
      projectRepository.save(
          TestProject.generate()
      );

      // do not create any object
      Assertions.assertThatThrownBy(() -> coreSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(),
          NOT_EXISTENT_OBJECT_ID
      )).isInstanceOf(DigitalObjectNotFoundException.class);
    }


  }

}
