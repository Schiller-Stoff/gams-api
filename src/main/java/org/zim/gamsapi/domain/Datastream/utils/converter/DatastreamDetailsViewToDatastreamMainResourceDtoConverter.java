package org.zim.gamsapi.domain.Datastream.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;

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
