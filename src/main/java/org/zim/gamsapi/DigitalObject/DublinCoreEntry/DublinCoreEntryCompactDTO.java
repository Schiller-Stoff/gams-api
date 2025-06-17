package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DublinCoreEntryCompactDTO(String value, String language) {

}
