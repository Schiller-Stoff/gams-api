package org.zim.gamsapi.Datastream.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.dto.DatastreamMainResourceDto;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;

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




  private static class DatastreamDetailsViewToDatastreamMainResourceDtoConverterImpl implements org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView {

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
    public Date getCreated() {
      return Date.from(Instant.parse("2023-10-01T00:00:00Z"));
    }

    @Override
    public Date getModified() {
      return Date.from(Instant.parse("2023-10-01T00:00:00Z"));
    }

    @Override
    public MetadataBaseEntity getBaseMetadata() {
      return new MetadataBaseEntity("Test Title", "Test Rights", "Test Creator", "Test Description", "md5", "sha512");
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
  }


}
