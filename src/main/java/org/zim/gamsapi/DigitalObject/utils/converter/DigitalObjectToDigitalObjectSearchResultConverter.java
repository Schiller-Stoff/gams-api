package org.zim.gamsapi.DigitalObject.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.utils.dto.DigitalObjectSearchResultDTO;


/**
 * Converts a DigitalObject to a DigitalObjectSearchResultDTO.
 */
@Component
public class DigitalObjectToDigitalObjectSearchResultConverter implements Converter<DigitalObject, DigitalObjectSearchResultDTO> {

  @Override
  public DigitalObjectSearchResultDTO convert(DigitalObject source) {

    return DigitalObjectSearchResultDTO.builder()
        .id(source.getId())
        .title(source.getBaseMetadata().getTitle())
        .description(source.getBaseMetadata().getDescription())
        .objectType(source.getObjectType())
        .projectAbbr(source.getProject().getProjectAbbr())
        .build();


  }
}
