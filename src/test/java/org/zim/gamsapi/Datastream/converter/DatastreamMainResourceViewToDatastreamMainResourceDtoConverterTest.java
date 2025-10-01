package org.zim.gamsapi.Datastream.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamMainResourceView;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;

import java.util.Set;

public class DatastreamMainResourceViewToDatastreamMainResourceDtoConverterTest extends UnitTest {

  @Test
  public void convertsToExpectedDatastreamMainResourceDto() {

    DatastreamMainResourceViewToDatastreamMainResourceDtoConverter converter = new DatastreamMainResourceViewToDatastreamMainResourceDtoConverter();
    var toBeConverted = new DatastreamMainResourceViewToDatastreamMainResourceDtoConverterImpl();

    var convertedDatastreamMainResourceDto = converter.convert(toBeConverted);

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


  public static class DatastreamMainResourceViewToDatastreamMainResourceDtoConverterImpl implements IDatastreamMainResourceView {

    @Override
    public String getDsid() {
      return "demo.dsid";
    }

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
    public String getMimeType() {
      return "application/xml";
    }

    @Override
    public MetadataBaseEntity getBaseMetadata() {
      return new MetadataBaseEntity() {
        @Override
        public String getTitle() {
          return "Demo Title";
        }

        @Override
        public String getRights() {
          return "Demo Rights";
        }

        @Override
        public String getCreator() {
          return "Demo Creator";
        }

        @Override
        public String getDescription() {
          return "Demo Description";
        }
      };
    }

    @Override
    public Set<String> getTags() {
      return Set.of("bla");
    }

    @Override
    public Set<String> getLang() {
      return Set.of("en", "de");
    }
  }


}
