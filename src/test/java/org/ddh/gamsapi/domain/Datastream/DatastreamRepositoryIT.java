package org.ddh.gamsapi.domain.Datastream;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.TestUtilities.*;

import java.util.Date;
import java.util.Set;

/**
 * Integration test for the DatastreamRepository.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamRepositoryIT extends IntegrationTest {

    /**
     * Mocks the auditing behavior of the app.
     * Without mocking the auditing handler, the tests would fail because of the missing oauth2 user info
     */
    @MockitoBean
    private AuditingHandler auditingHandler;

    @Autowired
    IDatastreamRepository datastreamRepository;

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private TestDataSet testDataSet;



    /**
     * Creates a test project, digital object and datastream before each test.
     * All created objects are deleted after each test.
     * All are in child relationship between project -> digital object -> datastream
     * no additional data is provided.
     */
    @BeforeEach
    public void setup(){
        testDataSet = testDataBuilder.buildTestDataSet();
    }

    @Nested
    public class SaveDatastreams {

        @Test
        public void throwsIfObjectIsNotSaved(){

            DigitalObject unsavedObject = TestDigitalObject.generate();
            unsavedObject.setId("NOT_SAVED_OBJECT_923");

            Datastream aDatastream = TestDatastream.generate(unsavedObject, "rand3.xml");


            org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> datastreamRepository.save(aDatastream)
            );

        }


        /**
         * Tests if a saved datastream exists with the expected globalID.
         */
        @Test
        public void saveDatastreamExistsWithExpectedID() {
            // using the DatastreamId from the test datastream
            Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), "RANDOM1.rdf");
            datastreamRepository.save(datastream);

            Assertions.assertThat(
                    datastreamRepository.findById(datastream.deriveDatastreamId()))
                .isNotNull()
                .isPresent()
                .get()
                .extracting(Datastream::deriveDatastreamId)
                .isEqualTo(datastream.deriveDatastreamId()
                );
        }

        @Test
        public void savingOfDatastreamShouldReturnExpectedProperties(){

            Datastream datastream = TestDatastream.generate(testDataSet.digitalObject());
            Datastream savedDatastream = datastreamRepository.save(datastream);

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getDsid)
                .isEqualTo(datastream.getDsid());

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getDigitalObject)
                .isEqualTo(datastream.getDigitalObject());

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getBaseMetadata)
                .isEqualTo(datastream.getBaseMetadata());

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getTags)
                .isEqualTo(datastream.getTags());

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getSize)
                .isEqualTo(datastream.getSize());

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getMimeType)
                .isEqualTo(datastream.getMimeType());

            Assertions.assertThat(savedDatastream)
                .isNotNull()
                .extracting(Datastream::getLang)
                .isEqualTo(datastream.getLang());

        }

    }

    @Nested
    public class DeleteDatastreams {

        /**
         * Tests if the datastream with the expected globalID was deleted.
         */
        @Test
        public void deleteDatastreamRemovesDatastream() {
            datastreamRepository.delete(testDataSet.mainDatastream());
            Assertions.assertThat(
                    datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId()))
                .isNotNull()
                .isNotPresent();
        }

        /**
         * Tests against delete cascading.
         */
        @Nested
        public class TestCascadingDelete {

            @Test
            public void digitalObjectWithSavedDatastreamsCannotBeHardDeleted(){
                // try to delete the object if datastream is still available
                org.junit.jupiter.api.Assertions.assertThrows(
                    DataIntegrityViolationException.class,
                    () -> digitalObjectRepository.delete(testDataSet.digitalObject())
                );

                // verify deletion is not cascaded
                Assertions.assertThat(
                    datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId())
                ).isPresent();

            }

            @Test
            public void datastreamDoesNotCascadeDeletedFromProject(){
                // because the test datastream should still exist
                org.junit.jupiter.api.Assertions.assertThrows(
                    DataIntegrityViolationException.class,
                    () -> projectRepository.delete(testDataSet.project())
                );
            }

            @Test
            public void deletionOfDatastreamDoesNotDeleteParentDigitalObject(){

                // delete datastream
                datastreamRepository.delete(testDataSet.mainDatastream());

                // datastream deleted
                Assertions.assertThat(
                        datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId()))
                    .isNotNull()
                    .isNotPresent();

                // object + project still available!
                Assertions.assertThat(
                        digitalObjectRepository.findById(testDataSet.digitalObject().getId()))
                    .isNotNull()
                    .isPresent();

                // additionally check if project is still available
                Assertions.assertThat(projectRepository.findById(testDataSet.project().getProjectAbbr()))
                    .isNotNull()
                    .isPresent();


            }

            @Test
            @Transactional
            public void deletionWorksAsExpected(){
                datastreamRepository.delete(testDataSet.mainDatastream());
                Assertions.assertThat(
                    datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId())
                ).isNotNull().isNotPresent();

            }

            @Test
            public void deleteAllRemovesTestDatastream(){


                datastreamRepository.deleteAll();

                // test datastream is not available anymore
                Assertions.assertThat(
                        datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId()))
                    .isNotNull()
                    .isNotPresent();

            }

        }

    }

    @Nested
    public class FindDatastreams {

        /**
         * Tests if a datastream that does not exist returns an empty optional.
         */
        @Test
        public void findByIdReturnsEmptyOptionalIfDatastreamDoesNotExist() {

            DatastreamId datastreamId = DatastreamId.builder()
                .dsid("NOT_THERE")
                .digitalObject("NOT_THERE")
                .build();

            Assertions.assertThat(
                    datastreamRepository.findById(datastreamId))
                .isNotNull()
                .isNotPresent();

        }

        @Test
        @Transactional
        public void findsExpectedDatastreamViaDatastreamId(){

            var foundDatastream = datastreamRepository.findById(
                testDataSet.mainDatastream().deriveDatastreamId()
            ).orElseThrow();

            String foundDsid = foundDatastream.getDsid();
            String foundDigitalObjectId = foundDatastream.getDigitalObject().getId();

            Assertions.assertThat(foundDsid)
                .isNotEmpty()
                .isEqualTo(foundDatastream.getDsid());

            Assertions.assertThat(foundDigitalObjectId)
                .isNotEmpty()
                .isEqualTo(foundDatastream.getDigitalObject().getId());

        }



        @Test
        public void findAllByDigitalObjectIdReturnsDatastreamDetailsViewWithSameDsid(){
            datastreamRepository.findAllByDigitalObjectId(testDataSet.digitalObject().getId())
                .forEach(datastreamDetailsView -> Assertions.assertThat(datastreamDetailsView)
                    .isNotNull()
                    .extracting(IDatastreamDetailsView::getDsid)
                    .isEqualTo(testDataSet.mainDatastream().getDsid()));
        }

        @Test
        public void findDatastreamDetailsViewByDigitalObjectAndDsidReturnsDatastreamDetailsView(){

            DatastreamId datastreamId = testDataSet.mainDatastream().deriveDatastreamId();

            datastreamRepository.findDatastreamDetailsViewByDigitalObject_IdAndDsid(datastreamId.getDigitalObject(), datastreamId.getDsid())
                .ifPresentOrElse(
                    datastreamDetailsView -> Assertions.assertThat(datastreamDetailsView)
                        .isNotNull()
                        .extracting(IDatastreamDetailsView::getDsid)
                        .isEqualTo(testDataSet.mainDatastream().getDsid()),
                    () -> Assertions.fail("Datastream not found")
                );
        }

        @Test
        public void findsExpectedMainDatastreamsByDigitalObjectIds(){

            var foundMainDatastreams = datastreamRepository.findMainDatastreamsByDigitalObjectIds(
                Set.of(testDataSet.digitalObject().getId())
            );

            Assertions.assertThat(foundMainDatastreams)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1)
                .allSatisfy(datastream -> {
                    Assertions.assertThat(datastream.getDsid())
                        .isEqualTo(testDataSet.mainDatastream().getDsid());
                    Assertions.assertThat(datastream.getDigitalObject().getId())
                        .isEqualTo(testDataSet.digitalObject().getId());
                    Assertions.assertThat(datastream.getBaseMetadata().getTitle())
                        .isEqualTo(testDataSet.mainDatastream().getBaseMetadata().getTitle());
                });
        }


    }

    @Nested
    public class Constraints {

        @Nested
        public class IDGeneration {
            @Test
            public void throwsIfDsidIsNull(){

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDataSet.digitalObject())
                    .baseMetadata(testDataSet.digitalObject().getBaseMetadata())
                    .build();

                datastream.setDsid(null);

                org.junit.jupiter.api.Assertions.assertThrows(
                    // exception is being thrown by ConstraintViolationException beneath
                    TransactionSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

            @Test
            public void throwsIfDigitalObjectIsNull(){

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDataSet.digitalObject())
                    .baseMetadata(testDataSet.digitalObject().getBaseMetadata())
                    .build();

                datastream.setDigitalObject(null);

                org.junit.jupiter.api.Assertions.assertThrows(
                    // exception is being thrown because composite primary key fails to be set
                    Exception.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

        }

        @Nested
        public class MetadataBaseEntityValidation {

            @Test
            public void throwsIfBaseMetadataIsNull(){

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDataSet.digitalObject())
                    .baseMetadata(testDataSet.digitalObject().getBaseMetadata())
                    .build();

                datastream.setBaseMetadata(null);

                org.junit.jupiter.api.Assertions.assertThrows(
                    TransactionSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

            @Test
            public void throwsIfMetadataDescriptionIsTooShort(){

                MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
                metadataBaseEntity.setDescription("1");

                Datastream datastream = new DatastreamBuilder()
                    .dsid("RANDOM_DSID_123456")
                    .digitalObject(testDataSet.digitalObject())
                    .baseMetadata(metadataBaseEntity)
                    .build();

                org.junit.jupiter.api.Assertions.assertThrows(
                    TransactionSystemException.class,
                    () -> datastreamRepository.save(datastream)
                );
            }

            @Test
            public void throwsIfMetadataRightsIsNull(){

                  MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
                  metadataBaseEntity.setRights(null);

                  Datastream datastream = new DatastreamBuilder()
                      .dsid("RANDOM_DSID_123456")
                      .digitalObject(testDataSet.digitalObject())
                      .baseMetadata(metadataBaseEntity)
                      .build();

                  org.junit.jupiter.api.Assertions.assertThrows(
                      TransactionSystemException.class,
                      () -> datastreamRepository.save(datastream)
                  );
            }

        }
    }

    @Nested
    public class FindMaxLastModifiedDateByProjectAbbr {

        @Test
        public void returnsExpectedModifiedDate(){

            // first find saved test datastream (done in beforeEach before)
            Datastream savedDatastream = datastreamRepository.findById(
                testDataSet.mainDatastream().deriveDatastreamId()
            ).orElseThrow();

            // query last modified date
            Date lastModified = datastreamRepository
                .findMaxLastModifiedDateByProjectAbbr(testDataSet.project().getProjectAbbr())
                .orElseThrow();

            // modified of datastream should be assigned
            Assertions.assertThat(savedDatastream.getModified())
                    .isNotNull();

            // last modified should be equal to the saved datastream
            Assertions.assertThat(lastModified)
                .isNotNull()
                .isEqualTo(savedDatastream.getModified());

        }

        @Test
        public void returnsLastModifiedDateOfDatastreams(){

            // first find saved test datastream (done in beforeEach before)
            Datastream savedDatastream = datastreamRepository.findById(
                testDataSet.mainDatastream().deriveDatastreamId()
            ).orElseThrow();

            // assert first datastream has a modified date
            Assertions.assertThat(savedDatastream.getModified())
                .isNotNull();

            // save another datastream
            Datastream savedLaterDatastream = testDataBuilder.addRandomDatastream(testDataSet);

            // assert second datastream has a modified date
            Assertions.assertThat(savedLaterDatastream.getModified())
                .isNotNull();

            // query last modified date
            Date lastModified = datastreamRepository
                .findMaxLastModifiedDateByProjectAbbr(testDataSet.project().getProjectAbbr()).
                orElseThrow();

            // last modified should be equal to the saved later datastream
            Assertions.assertThat(lastModified)
                .isNotNull()
                .hasSameTimeAs(savedLaterDatastream.getModified());

            // last modified should not be equal to the first datastream
            Assertions.assertThat(lastModified)
                .isNotEqualTo(savedDatastream.getModified());

        }

    }

    /**
     * Tests for time based modification auditing properties of the datastream entity.
     * createdBy and modifiedBy are excluded.
     */
    @Nested
    public class ModificationAuditing {


        /**
         * User auditing is disabled for this test-class
         */
        @Test
        public void userAuditingFieldsShouldBeNull(){

            Datastream foundDatastream = datastreamRepository.findById(
                testDataSet.mainDatastream().deriveDatastreamId()
            ).orElseThrow();

            org.assertj.core.api.Assertions.assertThat(foundDatastream.getCreatedBy()).isNull();
            org.assertj.core.api.Assertions.assertThat(foundDatastream.getModifiedBy()).isNull();

        }

        @Test
        public void modificationAuditingPropertiesAreNotNull(){
            // first some null assertions
            org.assertj.core.api.Assertions.assertThat(testDataSet.project().getCreated()).isNotNull();
            org.assertj.core.api.Assertions.assertThat(testDataSet.digitalObject().getModified()).isNotNull();

        }

        @Test
        public void modificationAuditingPropertiesAreUpdated(){

            Datastream foundDatastream = datastreamRepository.findById(
                testDataSet.mainDatastream().deriveDatastreamId()
            ).orElseThrow();

            Date modified = foundDatastream.getModified();


            // update the datastream
            foundDatastream.setType("bla");
            foundDatastream = datastreamRepository.save(foundDatastream);

            // check if the modification date has been updated
            org.assertj.core.api.Assertions.assertThat(
                foundDatastream.getModified()
            ).isAfter(modified);

            // modification date is different from created
            org.assertj.core.api.Assertions.assertThat(
                foundDatastream.getModified()
            ).isNotEqualTo(
                foundDatastream.getCreated()
            );

        }

    }

    @Nested
    public class FindAllByDsidAndProject {

      @Test
      public void findsExpectedDatastreamsByDsidAndProjectAbbr() {

          var foundDatastreams = datastreamRepository.findAllByDsidAndProject(
              testDataSet.mainDatastream().getDsid(),
              testDataSet.project().getProjectAbbr(),
              PageRequest.of(0,1000)
          );

          Assertions.assertThat(foundDatastreams)
              .isNotNull()
              .isNotEmpty()
              .hasSize(1)
              .allSatisfy(datastream -> {
                  Assertions.assertThat(datastream.getDsid())
                      .isEqualTo(testDataSet.mainDatastream().getDsid());
                  Assertions.assertThat(datastream.getDigitalObject().getId())
                      .isEqualTo(testDataSet.digitalObject().getId());
              });
      }

    }

}
