package org.zim.gamsapi.application.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.DatastreamBuilder;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagFile;
import org.zim.gamsapi.MetadataBaseEntityBuilder;

@Component
public class BagFileToDatastreamConverter implements Converter<BagFile, Datastream> {

    @Override
    public Datastream convert(BagFile source) {
        return new DatastreamBuilder()
                .dsid(source.getDsid())
                .mimeType(source.getMimetype())
                .size(source.getSize())
                .tags(source.getTags())
                .lang(source.getLang())
                .bagPath(source.getBagpath())
                // omitted fields - because not available in a BagitContentFile
                //.fileName()
                //.data()
                //.type()
                //.digitalObject()
                .baseMetadata(new MetadataBaseEntityBuilder()
                        .title(source.getTitle())
                        .creator(source.getCreator())
                        .description(source.getDescription())
                        .rights(source.getRights())
                        .md5Checksum(source.getMd5Checksum())
                        .sha512Checksum(source.getSha512Checksum())
                        .build())
                .build();

    }

}
