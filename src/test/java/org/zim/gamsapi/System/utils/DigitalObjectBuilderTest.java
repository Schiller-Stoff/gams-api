package org.zim.gamsapi.System.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestProject;

import java.util.Set;

public class DigitalObjectBuilderTest extends UnitTest {

    @Test
    public void testObjectHasExpectedProperties(){

        final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
        final String TEST_DSID = TestDatastream.DSID.getValue();

        DigitalObject digitalObject = new DigitalObjectBuilder("test")
                .addDatastream(TEST_DSID)
                    .withData(new byte[]{1,2,3})
                    .withMimeType("application/pdf")
                    .withFileName("test.pdf")
                    .withType("test")
                    .withSize(100L)
                    .withMetadataBaseEnity(MetadataBaseEntity.builder().build())
                    .add()
                .withObjectType("test")
                .addProject(TEST_PROJECT_ABBR)
                    .withDescription("description")
                    .add()
                .addBaseMetadata()
                    .withTitle("title")
                    .add()
                .withTypes(Set.of("test"))
                .build();

        Assertions.assertThat(digitalObject.getProject().getProjectAbbr())
                .isEqualTo(TEST_PROJECT_ABBR);
//        Assertions.assertThat(digitalObject.getDatastreams().iterator().next().getDsid())
//                .isEqualTo(TEST_DSID);

        org.junit.jupiter.api.Assertions.fail("Test needs to be updated to reflect the new implementation");

    }

    @Test
    public void bidirectionalRelationshipsBetweenObjectsDatastreamsAndProjectAreWorking(){

        final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
        final String TEST_DSID = TestDatastream.DSID.getValue();

        DigitalObject digitalObject = new DigitalObjectBuilder("test")
                .addDatastream(TEST_DSID)
                    .withData(new byte[]{1,2,3})
                    .withMimeType("application/pdf")
                    .withFileName("test.pdf")
                    .withType("test")
                    .withSize(100L)
                    .withMetadataBaseEnity(MetadataBaseEntity.builder().build())
                    .add()
                .withObjectType("test")
                .addProject(TEST_PROJECT_ABBR)
                    .withDescription("description")
                    .add()
                .addBaseMetadata()
                    .withTitle("title")
                    .add()
                .withTypes(Set.of("test"))
                .build();

        // Project reference inside digital object points to digital object itself
        //Assertions.assertThat(digitalObject.getProject().getDigitalObjects()).contains(digitalObject);

        // Datastream reference inside digital object points to digital object itself
        //Datastream datastream = digitalObject.getDatastreams().iterator().next();
        //Assertions.assertThat(digitalObject).isEqualTo(datastream.getDigitalObject());
        org.junit.jupiter.api.Assertions.fail("Test needs to be updated to reflect the new implementation");

    }





}
