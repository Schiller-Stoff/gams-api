package org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry;

/**
 * Data Transfer Object for Dublin Core entries.
 * This record holds the name, value, language, and digital object ID of a Dublin Core entry.
 */
public record DublinCoreEntryDTO(String name, String value, String language, String digitalObjectId) {
}
