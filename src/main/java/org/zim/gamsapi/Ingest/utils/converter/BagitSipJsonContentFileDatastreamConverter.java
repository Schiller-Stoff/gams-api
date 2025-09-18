package org.zim.gamsapi.Ingest.utils.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagitSipJsonContentFile;
import org.zim.gamsapi.MetadataBaseEntityBuilder;


/**
 * Converts a BagitContentFile to a Datastream.
 * Cannot set all properties, like reading the content from the file or filename etc for a datastream instance
 */
@Slf4j
@Component
public class BagitSipJsonContentFileDatastreamConverter implements Converter<BagitSipJsonContentFile, Datastream>{


  @Override
  public Datastream convert(BagitSipJsonContentFile source) {
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
            .build())
        .build();

  }
}
