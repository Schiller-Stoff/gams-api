package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Ingest.IngestRecord;

import java.time.Instant;

public class TestBagEntity {

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
                .createdBy(TestBagEntity.CREATED_BY)
                .schema(TestBagEntity.SCHEMA)
                .source(TestBagEntity.SOURCE)
                .externalDescription(TestBagEntity.EXTERNAL_DESCRIPTION)
                .baggingTimeStamp(TestBagEntity.BAGGING_TIMESTAMP)
                .contactMail(TestBagEntity.CONTACT_EMAIL)
                .payloadOxum(TestBagEntity.PAYLOAD_OXUM)
                .bagVersion(TestBagEntity.BAG_VERSION)
                .tagFileCharacterEncoding(TestBagEntity.TAG_FILE_CHARACTER_ENCODING)
                .build();
    }

}
