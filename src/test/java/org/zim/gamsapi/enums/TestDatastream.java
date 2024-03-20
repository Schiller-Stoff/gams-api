package org.zim.gamsapi.enums;

/**
 * Enum for test datastream.
 */
public enum TestDatastream {

    DSID("test"),
    DATASTREAM_NAME("test-datastream");

    private final String value;

    TestDatastream(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
