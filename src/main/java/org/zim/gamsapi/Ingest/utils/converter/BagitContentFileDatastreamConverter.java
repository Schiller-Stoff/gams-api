package org.zim.gamsapi.Ingest.utils.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitContentFile;
import org.zim.gamsapi.MetadataBaseEntityBuilder;


/**
 * Converts a BagitContentFile to a Datastream.
 * Cannot set all properties, like reading the content from the file or filename etc for a datastream instance
 */
@Slf4j
public class BagitContentFileDatastreamConverter implements Converter<BagitContentFile, Datastream>{


  @Override
  public Datastream convert(BagitContentFile source) {
    return new DatastreamBuilder()
        .dsid(source.getDsid())
        .mimeType(source.getMimetype())
        .size(source.getSize())
        // omitted fields - because not available in a BagitContentFile
        //.fileName()
        //.data()
        //.type()
        //.digitalObject()
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
