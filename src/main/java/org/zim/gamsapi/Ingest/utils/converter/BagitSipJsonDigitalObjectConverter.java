package org.zim.gamsapi.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.MetadataBaseEntityBuilder;

/**
 * Converts a BagitSipJson to a DigitalObject.
 *
 */
@Component
public class BagitSipJsonDigitalObjectConverter implements Converter<BagSipJson, DigitalObject> {

    @Override
    public DigitalObject convert(BagSipJson source) {

        DigitalObjectBuilder digitalObjectBuilder = new DigitalObjectBuilder()
            .id(source.getRecid())
            .project(source.getProject())
            .objectType(source.getObjectType())
            .publisher(source.getPublisher())
            .funder(source.getFunder())
            .mainResource(source.getMainResource())
            .baseMetadata(new MetadataBaseEntityBuilder()
                .title(source.getTitle())
                .creator(source.getCreator())
                .description(source.getDescription())
                .rights(source.getRights())
                .build());

        return digitalObjectBuilder.build();
    }
}
