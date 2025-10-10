package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.Ingest.IngestRecord;
import org.zim.gamsapi.Project.Project;

/**
 * TestDataSet is a record that encapsulates the test data set used in unit tests.
 * @param project
 * @param digitalObject
 * @param mainDatastream
 * @param ingestRecord
 * @param dublinCoreEntry
 */
public record TestDataSet(
    Project project,
    DigitalObject digitalObject,
    IngestRecord ingestRecord,
    Datastream mainDatastream,
    DublinCoreEntry dublinCoreEntry
) {
}
