package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Ingest.BagEntity;

public class TestBagEntity {

    public static final String ID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();

    /**
     * Generates a BagEntity for testing purposes.
     * @param digitalObject The DigitalObject to associate with the BagEntity.
     * @return A BagEntity instance populated with test data.
     */
    public static BagEntity generate(DigitalObject digitalObject){
        return BagEntity.builder()
                .digitalObject(digitalObject)
                .createdBy(TestBag.TestBagSipJson.CREATED_BY)
                .schema(TestBag.TestBagSipJson.SCHEMA)
                .source(TestBag.TestBagSipJson.SOURCE)
                .externalDescription(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION)
                .baggingTimeStamp(TestBag.TestBagInfo.BAGGING_TIMESTAMP)
                .contactMail(TestBag.TestBagInfo.CONTACT_EMAIL)
                .payloadOxum(TestBag.TestBagInfo.PAYLOAD_OXUM)
                .build();
    }

}
