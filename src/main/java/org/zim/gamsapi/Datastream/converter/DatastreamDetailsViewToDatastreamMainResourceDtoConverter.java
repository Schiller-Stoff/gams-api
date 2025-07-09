package org.zim.gamsapi.Datastream.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.Datastream.dto.DatastreamMainResourceDto;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;

@Component
public class DatastreamDetailsViewToDatastreamMainResourceDtoConverter implements Converter<IDatastreamDetailsView, DatastreamMainResourceDto> {

  @Override
  public DatastreamMainResourceDto convert(IDatastreamDetailsView source) {
    return DatastreamMainResourceDto.builder()
            .dsid(source.getDsid())
            .mimeType(source.getMimeType())
            .title(source.getBaseMetadata().getTitle())
            .rights(source.getBaseMetadata().getRights())
            .creator(source.getBaseMetadata().getCreator())
            .description(source.getBaseMetadata().getDescription())
            .tags(source.getTags())
            .lang(source.getLang())
            .build();
  }
}
