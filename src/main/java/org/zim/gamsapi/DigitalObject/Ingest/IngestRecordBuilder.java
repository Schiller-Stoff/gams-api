package org.zim.gamsapi.DigitalObject.Ingest;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.DigitalObject.DigitalObject;

@Slf4j
public class IngestRecordBuilder {

    private final IngestRecord ingestRecord = new IngestRecord();

    public IngestRecordBuilder createdBy(String createdBy) {
        ingestRecord.setBagCreatedBy(createdBy);
        return this;
    }

    public IngestRecordBuilder schema(String schema) {
        ingestRecord.setBagSchema(schema);
        return this;
    }

    public IngestRecordBuilder source(String source) {
        ingestRecord.setBagSource(source);
        return this;
    }

    public IngestRecordBuilder baggingTimeStamp(java.time.Instant baggingTimeStamp) {
        ingestRecord.setBaggingTimeStamp(baggingTimeStamp);
        return this;
    }

    public IngestRecordBuilder contactMail(String contactMail) {
        ingestRecord.setBagContactMail(contactMail);
        return this;
    }

    public IngestRecordBuilder externalDescription(String externalDescription) {
        ingestRecord.setBagExternalDescription(externalDescription);
        return this;
    }

    public IngestRecordBuilder payloadOxum(Float payloadOxum) {
        ingestRecord.setBagPayloadOxum(payloadOxum);
        return this;
    }

    public IngestRecordBuilder digitalObject(DigitalObject digitalObject) {
        ingestRecord.setDigitalObject(digitalObject);
        return this;
    }

    public IngestRecordBuilder bagVersion(String bagVersion) {
        ingestRecord.setBagVersion(bagVersion);
        return this;
    }

    public IngestRecordBuilder tagFileCharacterEncoding(String tagFileCharacterEncoding) {
        ingestRecord.setBagTagFileCharacterEncoding(tagFileCharacterEncoding);
        return this;
    }

    public IngestRecord build() {
        if(ingestRecord.getDigitalObject() == null){
            String msg = String.format("Failed to build BagEntity: DigitalObject is null and must be set. BagEntity: %s", ingestRecord);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return ingestRecord;
    }
}
