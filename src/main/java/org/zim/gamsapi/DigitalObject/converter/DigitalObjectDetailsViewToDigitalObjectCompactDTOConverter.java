package org.zim.gamsapi.DigitalObject.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.DigitalObjectCompactDTO;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;

import java.util.ArrayList;
import java.util.stream.Collectors;

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
                .types(source.getTypes())
                .datastreams(new ArrayList<>())
                .parent(source.getParent())
                .build();
    }
}
