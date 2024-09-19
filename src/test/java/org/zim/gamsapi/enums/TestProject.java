package org.zim.gamsapi.enums;

import org.zim.gamsapi.Project.Project;

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


    public static Project generate(){
        return Project.builder()
            .projectAbbr(PROJECT_ABBR.getValue())
            .build();
    }

}
