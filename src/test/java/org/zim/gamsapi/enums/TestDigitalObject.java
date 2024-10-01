package org.zim.gamsapi.enums;

import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.Project.Project;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Enum for test digital object.
 */
public enum TestDigitalObject {

    DIGITAL_OBJECT_ID("test.test"),
    DIGITAL_OBJECT_NAME("test-digital-object"),

    DIGITAL_OBJECT_TYPE("test-digital-object-type"),

    DIGITAL_OBJECT_PUBLISHER("test-publisher");

    private final String value;

    TestDigitalObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DigitalObject generate(String projectAbbr){
        DigitalObject digitalObject = generate();
        //setting given project
        Project project = TestProject.generate(projectAbbr);
        digitalObject.setProject(project);
        digitalObject.setId(projectAbbr + "." + digitalObject.getId());
        if(!digitalObject.getId().contains(projectAbbr + ".")){
            String msg = "DigitalObject ID does not contain project abbreviation with appended dot: " + digitalObject + " project: " + project;
            throw new IllegalStateException(msg);
        }
        return digitalObject;
    }

    public static DigitalObject generate(){
        return new DigitalObjectBuilder()
            .id(DIGITAL_OBJECT_ID.getValue())
            .project(TestProject.PROJECT_ABBR.getValue())
            .publisher(DIGITAL_OBJECT_PUBLISHER.getValue())
            .objectType(DIGITAL_OBJECT_TYPE.getValue())
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();
    }

    public static DigitalObject generate(String projectAbbr, String id){
        DigitalObject digitalObject =  generate(projectAbbr);
        digitalObject.setId(projectAbbr + "." + id);
        if(!digitalObject.getId().contains(projectAbbr + ".")){
            String msg = "DigitalObject ID does not contain project abbreviation with appended dot: " + digitalObject + " project: " + projectAbbr;
            throw new IllegalStateException(msg);
        }
        return digitalObject;
    }

}
