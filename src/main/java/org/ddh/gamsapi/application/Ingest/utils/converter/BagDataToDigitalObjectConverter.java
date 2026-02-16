package org.ddh.gamsapi.application.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectBuilder;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.BagData;
import org.ddh.gamsapi.domain.MetadataBaseEntityBuilder;

@Component
public class BagDataToDigitalObjectConverter implements Converter<BagData, DigitalObject> {

  @Override
  public DigitalObject convert(BagData source) {

    DigitalObjectBuilder digitalObjectBuilder = new DigitalObjectBuilder()
        .id(source.getId())
        .project(source.getProject())
        .objectType(source.getObjectType())
        .publisher(source.getPublisher())
        .funder(source.getFunder())
        .mainResource(source.getMainResource())
        .tags(source.getTags())
        .baseMetadata(new MetadataBaseEntityBuilder()
            .title(source.getTitle())
            .creator(source.getCreator())
            .description(source.getDescription())
            .rights(source.getRights())
            .build()
        );

    return digitalObjectBuilder.build();
  }
}
