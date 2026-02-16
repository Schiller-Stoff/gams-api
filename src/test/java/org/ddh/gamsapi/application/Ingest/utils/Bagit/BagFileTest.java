package org.ddh.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.TestUtilities.TestDatastream;
import org.ddh.gamsapi.UnitTest;

public class BagFileTest extends UnitTest {

    @Nested
    public class From {

        final Datastream TEST_DATASTREAM = TestDatastream.generate();

        @Test
        public void fromDatastreamIsNotNull(){
            BagFile bagFile = BagFile.from(TEST_DATASTREAM);
            Assertions.assertThat(bagFile).isNotNull();
        }

        @Test
        public void fromCreatesDatastreamWithNoNullFields(){
            BagFile bagFile = BagFile.from(TEST_DATASTREAM);
            Assertions.assertThat(bagFile).hasNoNullFieldsOrProperties();
        }

        @Test
        public void fromCreatesExpectedBagFile(){
            BagFile bagFile = BagFile.from(TEST_DATASTREAM);
            Assertions.assertThat(bagFile.getDsid()).isEqualTo(TEST_DATASTREAM.getDsid());
            Assertions.assertThat(bagFile.getMimetype()).isEqualTo(TEST_DATASTREAM.getMimeType());
            Assertions.assertThat(bagFile.getSize()).isEqualTo(TEST_DATASTREAM.getSize());
            Assertions.assertThat(bagFile.getTitle()).isEqualTo(TEST_DATASTREAM.getBaseMetadata().getTitle());
            Assertions.assertThat(bagFile.getDescription()).isEqualTo(TEST_DATASTREAM.getBaseMetadata().getDescription());
            Assertions.assertThat(bagFile.getBagpath()).isEqualTo(TEST_DATASTREAM.getBagPath());
            Assertions.assertThat(bagFile.getMd5Checksum()).isEqualTo(TEST_DATASTREAM.getMd5Checksum());
            Assertions.assertThat(bagFile.getSha512Checksum()).isEqualTo(TEST_DATASTREAM.getSha512Checksum());
            Assertions.assertThat(bagFile.getCreator()).isEqualTo(TEST_DATASTREAM.getBaseMetadata().getCreator());
            Assertions.assertThat(bagFile.getRights()).isEqualTo(TEST_DATASTREAM.getBaseMetadata().getRights());
            Assertions.assertThat(bagFile.getTags()).isEqualTo(TEST_DATASTREAM.getTags());
            Assertions.assertThat(bagFile.getLang()).isEqualTo(TEST_DATASTREAM.getLang());
        }

    }

}
