package org.zim.gamsapi.domain.Datastream.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.domain.Datastream.utils.dto.DatastreamMainResourceDto;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamMainResourceView;
import java.util.HashSet;

@Component
public class DatastreamMainResourceViewToDatastreamMainResourceDtoConverter implements Converter<IDatastreamMainResourceView, DatastreamMainResourceDto> {

  @Override
  public DatastreamMainResourceDto convert(IDatastreamMainResourceView source) {
    return DatastreamMainResourceDto.builder()
        .dsid(source.getDsid())
        .mimeType(source.getMimeType())
        .title(source.getBaseMetadata().getTitle())
        .rights(source.getBaseMetadata().getRights())
        .creator(source.getBaseMetadata().getCreator())
        .description(source.getBaseMetadata().getDescription())
        .tags(source.getTags() != null ? new HashSet<>(source.getTags()) : new HashSet<>())
        .lang(source.getLang() != null ? new HashSet<>(source.getLang()) : new HashSet<>())
        .build();
  }
}
