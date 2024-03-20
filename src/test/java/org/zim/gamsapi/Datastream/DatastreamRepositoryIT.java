package org.zim.gamsapi.Datastream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.IntegrationTest;

/**
 * Integration test for the DatastreamRepository.
 *
 */
public class DatastreamRepositoryIT extends IntegrationTest {

    @Autowired
    IDatastreamRepository datastreamRepository;


    /**
     * Tests if a saved datastream exists with the expected globalID.
     */
    @Test
    public void saveDatastreamExistsWithExpectedID() {
        Datastream datastream = datastreamRepository.save(Datastream.builder().build());
        Assertions.assertThat(
                datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isPresent()
                .get()
                .extracting(Datastream::getGlobalId)
                .isEqualTo(datastream.getGlobalId()
        );
        // clean up and check if successfully deleted
        datastreamRepository.delete(datastream);
        Assertions.assertThat(
                datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isNotPresent();
    }

    /**
     * Tests if the datastream with the expected globalID was deleted.
     */
    @Test
    public void deleteDatastreamRemovesDatastream() {
        Datastream datastream = datastreamRepository.save(Datastream.builder().build());
        datastreamRepository.delete(datastream);
        Assertions.assertThat(
                datastreamRepository.findById(datastream.getGlobalId()))
                .isNotNull()
                .isNotPresent();
    }


    @Test
    public void findByIdReturnsEmptyOptionalIfDatastreamDoesNotExist() {
        Assertions.assertThat(
                datastreamRepository.findById(5L))
                .isNotNull()
                .isNotPresent();
    }




}
