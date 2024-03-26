package org.zim.gamsapi.System.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;


public class DigitalObjectBuilderIT extends IntegrationTest  {

    @Autowired
    IDigitalObjectRepository digitalObjectRepository;

    @Autowired
    IProjectRepository projectRepository;

    @Test
    public void cascadePersistMergeProjectWhenSavingADigitalObject() {

        final String TEST_PID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
        final String TEST_PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();

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



}
