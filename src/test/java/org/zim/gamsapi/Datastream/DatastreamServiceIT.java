package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.enums.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamServiceIT extends IntegrationTest {

  @Autowired
  IDatastreamService datastreamService;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  final private MockMultipartFile TEST_MULTIPART_FILE = TestDatastreamContent.generate();

  @BeforeEach
  public void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class SaveDatastream {

    @Test
    public void throwsIfReferencedDigitalObjectNotFound(){

      final DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObject.setId("SOME_RANDOM_PID");

      Datastream datastream = TestDatastream.generate(digitalObject);

      Assertions.assertThrows(
          DigitalObjectNotFoundException.class,
          () -> datastreamService.save(datastream, TEST_MULTIPART_FILE)
      );

    }

    @Test
    public void datastreamExistsAfterSaving(){

      final String RANDOM_DSID = "SOME_RANDOM_DSID.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();

      datastreamService.save(datastream, TEST_MULTIPART_FILE);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isPresent();

    }

    @Test
    public void savedDatastreamHasExpectedTagProperty(){
      final String RANDOM_DSID = "SOME_RANDOM_DSID.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();

      Datastream savedDatastream = datastreamService.save(datastream, TEST_MULTIPART_FILE);
      org.assertj.core.api.Assertions.assertThat(savedDatastream)
          .isNotNull()
          .extracting(Datastream::getTags)
          .isEqualTo(datastream.getTags());

    }

    @Test
    public void savedDatastreamHasExpectedLangProperty(){
      final String RANDOM_DSID = "SOME_RANDOM_DSID.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();

      Datastream savedDatastream = datastreamService.save(datastream, TEST_MULTIPART_FILE);
      org.assertj.core.api.Assertions.assertThat(savedDatastream)
          .isNotNull()
          .extracting(Datastream::getLang)
          .isEqualTo(datastream.getLang());

    }


  }

  @Nested
  public class DeleteDatastream {
    @Test
    public void successfullyDeletesDatastream() {

      // actual deletion
      datastreamService.delete(testDataSet.mainDatastream());

      // check if datastream is deleted
      org.assertj.core.api.Assertions.assertThat(datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId()))
          .isNotNull()
          .isEmpty();

      // check if datastream content is also deleted
      org.assertj.core.api.Assertions.assertThat(datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId()))
          .isFalse();

    }

    @Test
    public void deleteThrowsWhenDigitalObjectIsNull() {
      Datastream datastream = new DatastreamBuilder()
          .dsid("SOME_RANDOM_DSID")
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Assertions.assertThrows(
          DigitalObjectNotFoundException.class,
          () -> datastreamService.delete(datastream)
      );
    }

    @Test
    public void deleteThrowsWhenDatastreamDoesNotExist() {
      Datastream datastream = new DatastreamBuilder()
          .dsid("SOME_RANDOM_DSID")
          .digitalObject(testDataSet.digitalObject())
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.delete(datastream)
      );
    }


  }

  @Nested
  public class FindAll {

    @Test
    public void returnsExpectedCountOfDatastreams(){
      testDataBuilder.addRandomDatastream(testDataSet);
      org.assertj.core.api.Assertions.assertThat(datastreamService.findAll(testDataSet.digitalObject()))
          .isNotNull()
          .isNotEmpty()
          .hasSize(2);
    }

  }

  @Nested
  public class FindById {

    @Test
    public void returnsExpectedDatastream(){
      org.assertj.core.api.Assertions.assertThat(
          datastreamService.findById(testDataSet.mainDatastream().deriveDatastreamId()))
            .isNotNull()
            .isEqualTo(testDataBuilder.buildTestDataSet().mainDatastream());
    }

    @Test
    public void throwsIfDatastreamNotFound(){

      DatastreamId randomId = new DatastreamId(
          "SOME_RANDOM_PID",
          "SOME_RANDOM_DSID");

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findById(randomId)
      );

    }


  }

  @Nested
  public class FindDatastreamDetailsById {

    @Test
    public void returnsExpectedDatastreamDetailsView(){

      org.assertj.core.api.Assertions.assertThat(
          datastreamService.findDatastreamDetailsById(testDataSet.mainDatastream().deriveDatastreamId()))
          .isNotNull()
          .extracting("dsid")
          .isEqualTo(testDataSet.mainDatastream().getDsid());

    }

    @Test
    public void throwsIfDatastreamDetailsViewNotFound(){

      DatastreamId randomId = new DatastreamId("SOME_RANDOM_PID", "SOME_RANDOM_DSID");

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findDatastreamDetailsById(randomId)
      );

    }

  }


}
