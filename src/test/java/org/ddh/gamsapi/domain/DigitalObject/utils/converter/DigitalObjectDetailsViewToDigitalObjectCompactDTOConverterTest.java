package org.ddh.gamsapi.domain.DigitalObject.utils.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectDetailsView;
import org.ddh.gamsapi.domain.MetadataBaseEntity;
import org.ddh.gamsapi.UnitTest;
import java.util.Date;

public class DigitalObjectDetailsViewToDigitalObjectCompactDTOConverterTest extends UnitTest {

  /**
   * Test the conversion of a DigitalObjectDetailsView to a DigitalObjectCompactDTO.
   */
  @Test
  public void convertsToExpectedDigitalObjectCompactDTO(){

    DigitalObjectDetailsView testObjectDetailsView = new DigitalObjectDetailsViewImpl();

    DigitalObjectCompactDTO digitalObjectCompactDTO =  new DigitalObjectDetailsViewToDigitalObjectCompactDTOConverter().convert(testObjectDetailsView);

    org.assertj.core.api.Assertions.assertThat(digitalObjectCompactDTO).isNotNull();
    org.assertj.core.api.Assertions.assertThat(digitalObjectCompactDTO).hasNoNullFieldsOrPropertiesExcept("mainResource","dublinCore");

    //
    Assertions.assertEquals(digitalObjectCompactDTO.getId(),testObjectDetailsView.getId());
    Assertions.assertEquals(digitalObjectCompactDTO.getProjectAbbr(), testObjectDetailsView.getProject().getProjectAbbr());


    Assertions.assertEquals(digitalObjectCompactDTO.getBaseMetadata(), testObjectDetailsView.getBaseMetadata());
    Assertions.assertEquals(digitalObjectCompactDTO.getObjectType(),testObjectDetailsView.getObjectType());

    Assertions.assertEquals(digitalObjectCompactDTO.getCreated(), testObjectDetailsView.getCreated());

    Assertions.assertEquals(digitalObjectCompactDTO.getModified(), testObjectDetailsView.getModified());
    Assertions.assertEquals(digitalObjectCompactDTO.getCreatedBy(), testObjectDetailsView.getCreatedBy());
    Assertions.assertEquals(digitalObjectCompactDTO.getModifiedBy(), testObjectDetailsView.getModifiedBy());
    Assertions.assertEquals(digitalObjectCompactDTO.getPublisher(), testObjectDetailsView.getPublisher());
    Assertions.assertEquals(digitalObjectCompactDTO.getPublished(), testObjectDetailsView.getPublished());

    Assertions.assertEquals(digitalObjectCompactDTO.getFunder(), testObjectDetailsView.getFunder());

  }

  /**
   * Test implementation of the *DetailsView - just needed to test the conversion process.
   */
  private static class DigitalObjectDetailsViewImpl implements DigitalObjectDetailsView {

    private final Date created = new Date();
    private final Date modified = new Date();
    private final Date published = new Date();

    @Override
    public String getId() {
      return "test";
    }

    @Override
    public ProjectView getProject() {
      return new ProjectView() {
        @Override
        public String getProjectAbbr() {
          return "test";
        }
      };
    }

    @Override
    public String getObjectType() {
      return "test";
    }

    @Override
    public MetadataBaseEntity getBaseMetadata() {
      return new MetadataBaseEntity();
    }

    @Override
    public Date getCreated() {
      return created;
    }

    @Override
    public Date getPublished() {
      return published;
    }

    @Override
    public Date getModified() {
      return modified;
    }

    @Override
    public String getCreatedBy() {
      return "test";
    }

    @Override
    public String getModifiedBy() {
      return "test";
    }

    @Override
    public String getPublisher() {
      return "test";
    }

    @Override
    public String getFunder() {
      return "test";
    }

    @Override
    public String getMainResource() {
      return "test-mainresource";
    }

  }


}
