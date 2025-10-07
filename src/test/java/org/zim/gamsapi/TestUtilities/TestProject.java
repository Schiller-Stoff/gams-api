package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;

/**
 * Enum for test project credentials.
 * Allows to generate project entities for testing purposes.
 */
public enum TestProject {

    PROJECT_ABBR("test"),
    PROJECT_TITLE("test-project"),
    PROJECT_DESCRIPTION("test-description");

    private final String value;

    TestProject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }


    /**
     * Generates a test project using the values defined in the enum.
     * @return The generated project.
     */
    public static Project generate(){
        return generate(PROJECT_ABBR.getValue());
    }

    /**
     * Generates a test project using the values defined in the enum. Allows to set the project abbreviation.
     * @param projectAbbr The abbreviation of the project.
     * @return The generated project.
     */
    public static Project generate(String projectAbbr){
        return ProjectBuilder.builder()
            .projectAbbr(projectAbbr)
            .description(PROJECT_DESCRIPTION.getValue())
            .title(PROJECT_TITLE.getValue())
            // following fields are supplied by the database / spring security worflows
            //.createdBy(TestUser.USERNAME.getValue())
            //.modifiedBy(TestUser.USERNAME.getValue())
            //.created(new Date())
            //.modified(new Date())
            //.published(new Date())
            .build();
    }

}
