package org.zim.gamsapi.System.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;


public class DigitalObjectBuilderIT extends IntegrationTest  {

    final String TEST_PID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
    final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();

    final String TEST_DSID = TestDatastream.DSID.getValue();

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    @Autowired
    IDatastreamRepository datastreamRepository;

    @Test
    public void cascadePersistMergeProjectWhenSavingADigitalObject() {

        DigitalObject digitalObject = new DigitalObjectBuilder(TEST_PID)
                // would need to be saved first via repository
                .addProject(TEST_PROJECT_ABBR)
                .add()
                .build();

        // Assert that the project does not exist in the database
        Assertions.assertThat(projectRepository.findById(TEST_PROJECT_ABBR)).isEmpty();

        // object and project exist now
        digitalObject = digitalObjectRepository.save(digitalObject);
        Assertions.assertThat(digitalObjectRepository.findById(TEST_PID)).isPresent();
        Assertions.assertThat(projectRepository.findById(TEST_PROJECT_ABBR)).isPresent();

        // clean up
        projectRepository.delete(digitalObject.getProject());
        Assertions.assertThat(projectRepository.findById(TEST_PROJECT_ABBR)).isEmpty();

    }


    @Test
    public void cascadePersistMergeDatastreamWhenSavingADigitalObject(){

        DigitalObject digitalObject = new DigitalObjectBuilder(TEST_PID)
                .addProject(TEST_PROJECT_ABBR)
                .add()
                .addDatastream(TEST_DSID)
                .add()
                .build();

        digitalObjectRepository.save(digitalObject);

        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject, TEST_DSID))
                .isPresent();

        //clean up - cascade delete from project to individual datastreams
        projectRepository.delete(digitalObject.getProject());
        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject, TEST_DSID)).isEmpty();

    }

    @Test
    public void mayAssignObjectToExistingProject(){

        DigitalObject digitalObject = new DigitalObjectBuilder(TEST_PID)
                .addProject(TEST_PROJECT_ABBR)
                .add()
                .addDatastream(TEST_DSID)
                .add()
                .build();

        digitalObject = digitalObjectRepository.save(digitalObject);

        DigitalObject digitalObject2 = new DigitalObjectBuilder("peterzwerg")
                .addProject(TEST_PROJECT_ABBR)
                .add()
                .addDatastream(TEST_DSID)
                .add()
                .build();

        digitalObjectRepository.save(digitalObject2);
        // check if the datastream is present of second object
        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject2, TEST_DSID))
                .isPresent();

        // cleanup
        projectRepository.delete(digitalObject.getProject());
        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject2, TEST_DSID))
                .isEmpty();
    }


    /**
     * Test that the builder can create a digital object with a datastream
     * and persist both to the database.
     */
    @Test
    public void cascadeCreationOfDatastreams(){

        DigitalObject digitalObject = new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
                .addProject(TestProject.PROJECT_ABBR.getValue())
                .add()
                .addDatastream(TestDatastream.DSID.getValue())
                .add()
                .build();

        digitalObjectRepository.save(digitalObject);

        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject, TestDatastream.DSID.getValue()))
                .isPresent();

        // cleanup
        projectRepository.delete(digitalObject.getProject());
        // ensure cleanup
        Assertions.assertThat(datastreamRepository.findByDigitalObjectAndDsid(digitalObject, TestDatastream.DSID.getValue()))
                .isEmpty();

    }

}
