package org.zim.gamsapi.domain.DigitalObject.SubmissionRecord;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;

@Slf4j
public class SubmissionRecordBuilder {

    private final SubmissionRecord submissionRecord = new SubmissionRecord();

    public SubmissionRecordBuilder createdBy(String createdBy) {
        submissionRecord.setBagCreatedBy(createdBy);
        return this;
    }

    public SubmissionRecordBuilder schema(String schema) {
        submissionRecord.setBagSchema(schema);
        return this;
    }

    public SubmissionRecordBuilder source(String source) {
        submissionRecord.setBagSource(source);
        return this;
    }

    public SubmissionRecordBuilder baggingTimeStamp(java.time.Instant baggingTimeStamp) {
        submissionRecord.setBaggingTimeStamp(baggingTimeStamp);
        return this;
    }

    public SubmissionRecordBuilder contactMail(String contactMail) {
        submissionRecord.setBagContactMail(contactMail);
        return this;
    }

    public SubmissionRecordBuilder externalDescription(String externalDescription) {
        submissionRecord.setBagExternalDescription(externalDescription);
        return this;
    }

    public SubmissionRecordBuilder payloadOxum(Float payloadOxum) {
        submissionRecord.setBagPayloadOxum(payloadOxum);
        return this;
    }

    public SubmissionRecordBuilder digitalObject(DigitalObject digitalObject) {
        submissionRecord.setDigitalObject(digitalObject);
        return this;
    }

    public SubmissionRecordBuilder bagVersion(String bagVersion) {
        submissionRecord.setBagVersion(bagVersion);
        return this;
    }

    public SubmissionRecordBuilder tagFileCharacterEncoding(String tagFileCharacterEncoding) {
        submissionRecord.setBagTagFileCharacterEncoding(tagFileCharacterEncoding);
        return this;
    }

    public SubmissionRecord build() {
        if(submissionRecord.getDigitalObject() == null){
            String msg = String.format("Failed to build BagEntity: DigitalObject is null and must be set. BagEntity: %s", submissionRecord);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return submissionRecord;
    }
}
