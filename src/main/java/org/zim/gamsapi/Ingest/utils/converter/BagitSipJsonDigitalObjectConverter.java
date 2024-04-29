package org.zim.gamsapi.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitSipJson;
import org.zim.gamsapi.MetadataBaseEntityBuilder;

/**
 * Converts a BagitSipJson to a DigitalObject.
 *
 */
@Component
public class BagitSipJsonDigitalObjectConverter implements Converter<BagitSipJson, DigitalObject> {

    @Override
    public DigitalObject convert(BagitSipJson source) {
        return new DigitalObjectBuilder()
            .id(source.getId())
            .project(source.getProject())
            .objectType(source.getObjectType())
            .parent(source.getParent())
            .types(source.getTypes())
            .baseMetadata(new MetadataBaseEntityBuilder()
                .title(source.getTitle())
                .creator(source.getCreator())
                .description(source.getDescription())
                .publisher(source.getPublisher())
                .rights(source.getRights())
                .build())
            .build();
    }
}
