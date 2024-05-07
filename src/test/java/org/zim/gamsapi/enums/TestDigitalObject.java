package org.zim.gamsapi.enums;

/**
 * Enum for test digital object.
 */
public enum TestDigitalObject {

    DIGITAL_OBJECT_ID("test"),
    DIGITAL_OBJECT_NAME("test-digital-object");

    private final String value;

    TestDigitalObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
