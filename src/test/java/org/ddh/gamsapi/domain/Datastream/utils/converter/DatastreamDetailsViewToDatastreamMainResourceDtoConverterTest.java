package org.ddh.gamsapi.domain.Datastream.utils.converter;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDatastream;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
import org.ddh.gamsapi.UnitTest;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

public class DatastreamDetailsViewToDatastreamMainResourceDtoConverterTest extends UnitTest {

  @Test
  public void convertsToExpectedDatastreamMainResourceDto() {
    DatastreamDetailsViewToDatastreamMainResourceDtoConverter converter = new DatastreamDetailsViewToDatastreamMainResourceDtoConverter();
    var toBeConverted = new DatastreamDetailsViewToDatastreamMainResourceDtoConverterImpl();

    DatastreamMainResourceDto convertedDatastreamMainResourceDto = converter.convert(toBeConverted);

    Assertions.assertThat(convertedDatastreamMainResourceDto).isNotNull();

    Assertions.assertThat(convertedDatastreamMainResourceDto).hasNoNullFieldsOrProperties();

    Assertions.assertThat(convertedDatastreamMainResourceDto.getCreator())
        .isEqualTo(toBeConverted.getBaseMetadata().getCreator());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getDescription())
        .isEqualTo(toBeConverted.getBaseMetadata().getDescription());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getDsid())
        .isEqualTo(toBeConverted.getDsid());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getTitle())
        .isEqualTo(toBeConverted.getBaseMetadata().getTitle());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getRights())
        .isEqualTo(toBeConverted.getBaseMetadata().getRights());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getTags())
        .isEqualTo(toBeConverted.getTags());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getLang())
        .isEqualTo(toBeConverted.getLang());

    Assertions.assertThat(convertedDatastreamMainResourceDto.getMimeType())
        .isEqualTo(toBeConverted.getMimeType());

  }




  private static class DatastreamDetailsViewToDatastreamMainResourceDtoConverterImpl implements IDatastreamDetailsView {

    @Override
    public DigitalObjectView getDigitalObject() {
      return new DigitalObjectView() {
        @Override
        public String getId() {
          return "demo.id";
        }
      };
    }

    @Override
    public String getDsid() {
      return "test-dsid";
    }

    @Override
    public String getMimeType() {
      return "application/json";
    }

    @Override
    public String getFileName() {
      return "demo.txt";
    }

    @Override
    public Long getSize() {
      return 0L;
    }

    @Override
    public String getType() {
      return "random";
    }

    @Override
    public Instant getCreated() {
      return Instant.parse("2023-10-01T00:00:00Z");
    }

    @Override
    public Instant getModified() {
      return Instant.parse("2023-10-01T00:00:00Z");
    }

    @Override
    public MetadataBaseEntity getBaseMetadata() {
      return new MetadataBaseEntity("Test Title", "Test Rights", "Test Creator", "Test Description");
    }

    @Override
    public String getCreatedBy() {
      return "foo";
    }

    @Override
    public String getModifiedBy() {
      return "foo";
    }

    @Override
    public Set<String> getTags() {
      return Set.of("test-tag");
    }

    @Override
    public Set<String> getLang() {
      return Set.of("en");
    }

    @Override
    public String getMd5Checksum() {
      return TestDatastream.MD5_CHECKSUM;
    }

    @Override
    public String getSha512Checksum() {
      return TestDatastream.SHA512_CHECKSUM;
    }
  }


}
