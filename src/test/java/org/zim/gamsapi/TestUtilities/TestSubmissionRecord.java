package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;

import java.time.Instant;
import java.util.Date;

public class TestSubmissionRecord {

    public static final String ID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
    public static final String CREATED_BY = TestBag.TestBagSipJson.CREATED_BY;
    public static final String SCHEMA = TestBag.TestBagSipJson.SCHEMA;
    public static final String SOURCE = TestBag.TestBagSipJson.SOURCE;
    public static final String EXTERNAL_DESCRIPTION = TestBag.TestBagInfo.EXTERNAL_DESCRIPTION;
    public static final String PAYLOAD_OXUM = TestBag.TestBagInfo.PAYLOAD_OXUM;
    public static final String BAG_VERSION = TestBag.BagitTxt.BAGIT_VERSION;
    public static final String TAG_FILE_CHARACTER_ENCODING = TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING;
    public static final String BAGGING_DATE = TestBag.TestBagInfo.BAGGING_DATE;
    public static final String CONTACT_EMAIL = TestBag.TestBagInfo.CONTACT_EMAIL;

    /**
     * Generates a BagEntity for testing purposes.
     * @param digitalObject The DigitalObject to associate with the BagEntity.
     * @return A BagEntity instance populated with test data.
     */
    public static SubmissionRecord generate(DigitalObject digitalObject){
        return SubmissionRecord.builder()
                .digitalObject(digitalObject)
                .createdBy(TestSubmissionRecord.CREATED_BY)
                .schema(TestSubmissionRecord.SCHEMA)
                .source(TestSubmissionRecord.SOURCE)
                .externalDescription(TestSubmissionRecord.EXTERNAL_DESCRIPTION)
                .baggingDate(TestSubmissionRecord.BAGGING_DATE)
                .contactMail(TestSubmissionRecord.CONTACT_EMAIL)
                .payloadOxum(TestSubmissionRecord.PAYLOAD_OXUM)
                .bagVersion(TestSubmissionRecord.BAG_VERSION)
                .tagFileCharacterEncoding(TestSubmissionRecord.TAG_FILE_CHARACTER_ENCODING)
                .build();
    }

}
