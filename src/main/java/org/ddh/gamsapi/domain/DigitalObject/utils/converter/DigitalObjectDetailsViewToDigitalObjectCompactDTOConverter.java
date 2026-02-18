package org.ddh.gamsapi.domain.DigitalObject.utils.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectDetailsView;

import java.util.HashSet;

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
                .tags(source.getTags() != null ? source.getTags() : new HashSet<>())
                .isIngested(source.isIngested())
                .archiveState(source.getArchiveState())
                .build();
    }
}
