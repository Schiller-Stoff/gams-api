package org.zim.gamsapi.enums;

import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.GAMSCollection.GAMSCollection;
import org.zim.gamsapi.Project.Project;

/**
 * TestDataSet is a record that encapsulates the test data set used in unit tests.
 * @param project
 * @param digitalObject
 * @param mainDatastream
 * @param dublinCoreEntry
 */
public record TestDataSet(
    Project project,
    DigitalObject digitalObject,
    Datastream mainDatastream,
    DublinCoreEntry dublinCoreEntry,
    GAMSCollection gamsCollection
) {
}
