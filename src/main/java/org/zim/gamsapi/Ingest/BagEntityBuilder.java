package org.zim.gamsapi.Ingest;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.DigitalObject.DigitalObject;

@Slf4j
public class BagEntityBuilder {

    private final BagEntity bagEntity = new BagEntity();

    public BagEntityBuilder createdBy(String createdBy) {
        bagEntity.setCreatedBy(createdBy);
        return this;
    }

    public BagEntityBuilder schema(String schema) {
        bagEntity.setSchema(schema);
        return this;
    }

    public BagEntityBuilder source(String source) {
        bagEntity.setSource(source);
        return this;
    }

    public BagEntityBuilder baggingTimeStamp(java.time.Instant baggingTimeStamp) {
        bagEntity.setBaggingTimeStamp(baggingTimeStamp);
        return this;
    }

    public BagEntityBuilder contactMail(String contactMail) {
        bagEntity.setContactMail(contactMail);
        return this;
    }

    public BagEntityBuilder externalDescription(String externalDescription) {
        bagEntity.setExternalDescription(externalDescription);
        return this;
    }

    public BagEntityBuilder payloadOxum(Float payloadOxum) {
        bagEntity.setPayloadOxum(payloadOxum);
        return this;
    }

    public BagEntityBuilder digitalObject(DigitalObject digitalObject) {
        bagEntity.setDigitalObject(digitalObject);
        return this;
    }

    public BagEntityBuilder bagVersion(String bagVersion) {
        bagEntity.setBagVersion(bagVersion);
        return this;
    }

    public BagEntityBuilder tagFileCharacterEncoding(String tagFileCharacterEncoding) {
        bagEntity.setTagFileCharacterEncoding(tagFileCharacterEncoding);
        return this;
    }

    public BagEntity build() {
        if(bagEntity.getDigitalObject() == null){
            String msg = String.format("Failed to build BagEntity: DigitalObject is null and must be set. BagEntity: %s", bagEntity);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return bagEntity;
    }
}
