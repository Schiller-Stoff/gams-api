package org.zim.gamsapi.enums;

/**
 * Enum for test project credentials.
 */
public enum TestProject {

    PROJECT_ABBR("test"),
    PROJECT_NAME("test-project");

    private final String value;

    TestProject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
