package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.application.Ingest.IngestRecord;

import java.time.Instant;

public class TestIngestRecord {

    public static final String ID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
    public static final String CREATED_BY = TestBag.TestBagSipJson.CREATED_BY;
    public static final String SCHEMA = TestBag.TestBagSipJson.SCHEMA;
    public static final String SOURCE = TestBag.TestBagSipJson.SOURCE;
    public static final String EXTERNAL_DESCRIPTION = TestBag.TestBagInfo.EXTERNAL_DESCRIPTION;
    public static final Float PAYLOAD_OXUM = TestBag.TestBagInfo.PAYLOAD_OXUM;
    public static final String BAG_VERSION = TestBag.BagitTxt.BAGIT_VERSION;
    public static final String TAG_FILE_CHARACTER_ENCODING = TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING;
    public static final Instant BAGGING_TIMESTAMP = TestBag.TestBagInfo.BAGGING_TIMESTAMP;
    public static final String CONTACT_EMAIL = TestBag.TestBagInfo.CONTACT_EMAIL;

    /**
     * Generates a BagEntity for testing purposes.
     * @param digitalObject The DigitalObject to associate with the BagEntity.
     * @return A BagEntity instance populated with test data.
     */
    public static IngestRecord generate(DigitalObject digitalObject){
        return IngestRecord.builder()
                .digitalObject(digitalObject)
                .createdBy(TestIngestRecord.CREATED_BY)
                .schema(TestIngestRecord.SCHEMA)
                .source(TestIngestRecord.SOURCE)
                .externalDescription(TestIngestRecord.EXTERNAL_DESCRIPTION)
                .baggingTimeStamp(TestIngestRecord.BAGGING_TIMESTAMP)
                .contactMail(TestIngestRecord.CONTACT_EMAIL)
                .payloadOxum(TestIngestRecord.PAYLOAD_OXUM)
                .bagVersion(TestIngestRecord.BAG_VERSION)
                .tagFileCharacterEncoding(TestIngestRecord.TAG_FILE_CHARACTER_ENCODING)
                .build();
    }

}
