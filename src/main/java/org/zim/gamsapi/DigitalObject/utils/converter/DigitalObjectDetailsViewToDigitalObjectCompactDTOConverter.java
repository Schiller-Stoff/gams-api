package org.zim.gamsapi.DigitalObject.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.zim.gamsapi.DigitalObject.utils.interfaces.DigitalObjectDetailsView;

@Component
public class DigitalObjectDetailsViewToDigitalObjectCompactDTOConverter implements Converter<DigitalObjectDetailsView, DigitalObjectCompactDTO> {
    @Override
    public DigitalObjectCompactDTO convert(DigitalObjectDetailsView source) {
        return DigitalObjectCompactDTO.builder()
                .id(source.getId())
                .objectType(source.getObjectType())
                .projectAbbr(source.getProject().getProjectAbbr())
                .baseMetadata(source.getBaseMetadata())
                .created(source.getCreated())
                .modified(source.getModified())
                .published(source.getPublished())
                .createdBy(source.getCreatedBy())
                .modifiedBy(source.getModifiedBy())
                .publisher(source.getPublisher())
                .funder(source.getFunder())
                .build();
    }
}
