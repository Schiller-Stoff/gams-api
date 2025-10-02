package org.zim.gamsapi.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.Ingest.utils.Bagit.BagData;
import org.zim.gamsapi.MetadataBaseEntityBuilder;

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
                .baseMetadata(new MetadataBaseEntityBuilder()
                        .title(source.getTitle())
                        .creator(source.getCreator())
                        .description(source.getDescription())
                        .rights(source.getRights())
                        .sha512Checksum(source.getSha512Checksum())
                        .md5Checksum(source.getMd5Checksum())
                        .build()
                );

        return digitalObjectBuilder.build();
    }


}
