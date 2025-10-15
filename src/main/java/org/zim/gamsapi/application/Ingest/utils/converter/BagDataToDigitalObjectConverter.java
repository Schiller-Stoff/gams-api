package org.zim.gamsapi.application.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagData;
import org.zim.gamsapi.domain.MetadataBaseEntityBuilder;

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
