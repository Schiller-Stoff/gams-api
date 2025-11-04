package org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;

@Component
public class DublinCoreEntrySummaryViewToDublinCoreCompactDTOConverter implements Converter<DublinCoreEntrySummaryView, DublinCoreEntryCompactDTO> {
    @Override
    public DublinCoreEntryCompactDTO convert(DublinCoreEntrySummaryView source) {
        return new DublinCoreEntryCompactDTO(source.getValue(), source.getLanguage());
    }
}
