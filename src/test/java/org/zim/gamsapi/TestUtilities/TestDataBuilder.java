package org.zim.gamsapi.TestUtilities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentRepository;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.domain.DigitalObjectCollection.IDigitalObjectCollectionRepository;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.zim.gamsapi.domain.Project.Project;
import org.zim.gamsapi.domain.Project.interfaces.IProjectRepository;

/**
 * TestDataBuilder is a component that builds test data sets for testing.
 */
@Component
@Slf4j
public class TestDataBuilder {

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;

  @Autowired
  private IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Autowired
  private DatastreamContentRepository datastreamContentRepository;

  @Autowired
  private ISubmissionRecordRepository bagEntityRepository;

  @Autowired
  private IDigitalObjectCollectionRepository gamsCollectionRepository;

  @Transactional
  public void removeAllExceptProjects(TestDataSet testDataSet) {
    datastreamContentRepository.delete(testDataSet.mainDatastream().deriveDatastreamId());
    dublinCoreEntryRepository.delete(testDataSet.dublinCoreEntry());
    datastreamRepository.delete(testDataSet.mainDatastream());
    bagEntityRepository.delete(testDataSet.submissionRecord());
    digitalObjectRepository.delete(testDataSet.digitalObject());
  }

  /**
   * Adds a random project to the already existing data in the database.
   * @param testDataSet the test data set to which the project will be added
   * @return the saved project
   */
  @Transactional
  public Project addRandomProject(TestDataSet testDataSet) {

    // create a random project
    // id must not be longer than 10 characters
    var randomProjectId = TestProject.PROJECT_ABBR.getValue() + System.currentTimeMillis();
    if (randomProjectId.length() > 10) {
      randomProjectId = randomProjectId.substring(0, 10);
    }

    // check if the id already exists
    if (projectRepository.existsById(randomProjectId)) {
      String msg = String.format("Trying to save random project but calculated a duplicated id: %s", randomProjectId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    var projectToBeSaved = TestProject.generate(randomProjectId);

    return projectRepository.save(projectToBeSaved);
  }

  /**
   * Adds a random datastream to the already existing data in the database.
   * @param testDataSet the test data set to which the datastream will be added
   * @return the saved datastream
   */
  @Transactional
  public Datastream addRandomDatastream(TestDataSet testDataSet) {

    // create a random datastream id
    var randomDatastreamId = System.currentTimeMillis() + TestDatastream.DSID.getValue();

    // check if the id already exists
    var existingIds = datastreamRepository.findAllByDigitalObjectId(testDataSet.digitalObject().getId());
    if (existingIds.stream().anyMatch(datastream -> datastream.getDsid().equals(randomDatastreamId))) {
      String msg = String.format("Trying to save random datastream but calculated duplicated ids: %s", randomDatastreamId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    var datastreamToBeSaved = TestDatastream.generate(
            testDataSet.digitalObject(),randomDatastreamId);

    return datastreamRepository.save(datastreamToBeSaved);
  }

  /**
   * Adds a random digital object to the already existing data in the database
   *
   * @param testDataSet the test data set to which the digital object will be added
   * @return the saved digital object
   * @throws IllegalStateException if a digital object with the same id already exists
   */
  @Transactional
  public DigitalObject addRandomObject(TestDataSet testDataSet) {

    // create a random digital object id
    var randomDigitalObjectId = TestDigitalObject.DIGITAL_OBJECT_ID.getValue() + System.currentTimeMillis();

    // check if the id already exists
    var existingIds = digitalObjectRepository.findAllByProject_ProjectAbbr(testDataSet.project().getProjectAbbr());
    if (existingIds.stream().anyMatch(digitalObject -> digitalObject.getId().equals(randomDigitalObjectId))) {
      String msg = String.format("Trying to save random digital object but calculated duplicated ids: %s", randomDigitalObjectId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    var digitalObjectToBeSaved = TestDigitalObject.generate(
            testDataSet.project().getProjectAbbr(), randomDigitalObjectId
    );

    bagEntityRepository.save(TestSubmissionRecord.generate(digitalObjectToBeSaved));

    return digitalObjectRepository.save(digitalObjectToBeSaved);
  }

  @Transactional
  public TestDataSet buildTestDataSet() {

    var projectToBeSaved = TestProject.generate();

    var persistedProject = projectRepository.save(projectToBeSaved);

    var digitalObjectToBeSaved =
            TestDigitalObject.generate(persistedProject.getProjectAbbr());

    var persistedDigitalObject = digitalObjectRepository.save(digitalObjectToBeSaved);

    var bagEntityToBeSaved = TestSubmissionRecord.generate(persistedDigitalObject);

    var persistedBagEntity = bagEntityRepository.save(bagEntityToBeSaved);

    var datastreamToBeSaved = TestDatastream.generate(persistedDigitalObject);

    var persistedDatastream = datastreamRepository.save(datastreamToBeSaved);

    // save the content of the datastream
    datastreamContentRepository.save(
        TestDatastreamContent.CONTENT.getValue().getBytes()
        , persistedDatastream.deriveDatastreamId()
    );

    var dublinCoreEntryToBeSaved = TestDublinCoreEntry.generate(persistedDigitalObject);

    var persistedDublinCoreEntry = dublinCoreEntryRepository.save(dublinCoreEntryToBeSaved);

    return new TestDataSet(
        persistedProject,
        persistedDigitalObject,
        persistedBagEntity,
        persistedDatastream,
        persistedDublinCoreEntry
    );
  }


}
