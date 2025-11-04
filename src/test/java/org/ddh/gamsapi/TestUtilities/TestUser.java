package org.ddh.gamsapi.TestUtilities;

/**
 * Enum for test user credentials.
 * Only used for testing purposes.
 */
public enum TestUser {

    USERNAME("test"),
    PASSWORD("test");


    private final String value;

    TestUser(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
