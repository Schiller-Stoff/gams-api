package org.zim.gamsapi.Ingest.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Ingest.utils.Bagit.BagFile;
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
