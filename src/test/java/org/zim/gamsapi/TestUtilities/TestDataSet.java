package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.zim.gamsapi.domain.Project.Project;

/**
 * TestDataSet is a record that encapsulates the test data set used in unit tests.
 * @param project
 * @param digitalObject
 * @param mainDatastream
 * @param submissionRecord
 * @param dublinCoreEntry
 */
public record TestDataSet(
    Project project,
    DigitalObject digitalObject,
    SubmissionRecord submissionRecord,
    Datastream mainDatastream,
    DublinCoreEntry dublinCoreEntry
) {
}
