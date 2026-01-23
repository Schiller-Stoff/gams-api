package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.ArchivalRecord;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.ddh.gamsapi.domain.Project.Project;

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
    ArchivalRecord archivalRecord,
    Datastream mainDatastream,
    DublinCoreEntry dublinCoreEntry
) {
}
