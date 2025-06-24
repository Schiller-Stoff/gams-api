package org.zim.gamsapi.DigitalObject.DublinCoreEntry.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;

@Component
public class DublinCoreEntrySummaryViewToDublinCoreCompactDTOConverter implements Converter<DublinCoreEntrySummaryView, DublinCoreEntryCompactDTO> {
    @Override
    public DublinCoreEntryCompactDTO convert(DublinCoreEntrySummaryView source) {
        return new DublinCoreEntryCompactDTO(source.getValue(), source.getLanguage());
    }
}
