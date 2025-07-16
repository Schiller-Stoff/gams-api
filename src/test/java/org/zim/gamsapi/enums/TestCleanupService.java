package org.zim.gamsapi.enums;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamContent.DatastreamContentDeletionFailure;
import org.zim.gamsapi.Datastream.DatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.GAMSCollection.GAMSCollection;
import org.zim.gamsapi.Project.Project;

@Service
@Transactional
@Slf4j
public class TestCleanupService {

  private final DatastreamContentRepository datastreamContentRepository;
  @PersistenceContext
  private EntityManager entityManager;

  public TestCleanupService(DatastreamContentRepository datastreamContentRepository) {
    this.datastreamContentRepository = datastreamContentRepository;
  }

  /**
   * Clean up using EntityManager native queries - no custom repository methods needed
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanupAllTestData() {

    // delete all data from the filesystem
    datastreamContentRepository.deleteAll();

    try {
      log.debug("Starting test data cleanup using EntityManager");

      // TODO refactor whole method (use deleteAllQuery method)

      // Disable foreign key checks for PostgreSQL
      entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();

      // Delete in any order since FK checks are disabled
      executeCleanupQuery("DELETE FROM datastream_content_deletion_failure");
      executeCleanupQuery("DELETE FROM dublin_core_entry");
      executeCleanupQuery("DELETE FROM collection_digital_object");
      executeCleanupQuery("DELETE FROM datastream");
      executeCleanupQuery("DELETE FROM collection");
      executeCleanupQuery("DELETE FROM digital_object");
      executeCleanupQuery("DELETE FROM project");

      // Re-enable foreign key checks
      entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();

      entityManager.flush();
      entityManager.clear();

      log.debug("Test data cleanup completed successfully");

    } catch (Exception e) {
      log.error("Error during test data cleanup", e);
      // Always try to re-enable FK checks
      try {
        entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
      } catch (Exception ignored) {}
      throw e;
    }
  }

  /**
   * TODO jdoc
   * Alternative cleanup respecting foreign keys (slower but safer)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanupAllTestDataSafe() {

    // delete all data from the filesystem
    datastreamContentRepository.deleteAll();

    try {
      // TODO redo log statement
      log.debug("Starting safe test data cleanup");

      executeDeleteAllQuery(DatastreamContentDeletionFailure.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(DublinCoreEntry.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(GAMSCollection.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(Datastream.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(DigitalObject.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(Project.ORDERED_MANAGED_TABLES);

      entityManager.flush();
      entityManager.clear();

    } catch (Exception e) {
      log.error("Error during safe cleanup", e);
      throw e;
    }
  }

  /**
   * Execute a cleanup query with error handling
   */
  private void executeCleanupQuery(String sql) {
    try {
      int deletedRows = entityManager.createNativeQuery(sql).executeUpdate();
      if (deletedRows > 0) {
        log.trace("Deleted {} rows with query: {}", deletedRows, sql);
      }
    } catch (Exception e) {
      log.warn("Failed to execute cleanup query: {} - Error: {}", sql, e.getMessage());
      // Don't throw - continue with other cleanup operations
    }
  }

  /**
   * Execute delete all queries for all managed tables in the given order
   * @param orderedTableNames Array of table names in the order they should be deleted
   */
  private void executeDeleteAllQuery(String[] orderedTableNames) {
    for (String tableName : orderedTableNames) {
      executeDeleteAllQuery(tableName);
    }
  }

  /**
   * Execute a delete all query for a specific table
   * @param tableName Name of the table to delete all rows from
   */
  private void executeDeleteAllQuery(String tableName) {

    final String DELETE_ALL_QUERY = String.format("DELETE FROM %s", tableName);

    try {
      int deletedRows = entityManager.createNativeQuery(DELETE_ALL_QUERY).executeUpdate();
      if (deletedRows > 0) {
        log.trace("Deleted {} rows from table: {}", deletedRows, tableName);
      }
    } catch (Exception e) {
      log.warn("Failed to delete from table: {} - Error: {}", tableName, e.getMessage());
      throw e;
    }
  }

  /**
   * Verify cleanup was successful
   */
  @Transactional(readOnly = true)
  public boolean verifyCleanupSuccess() {
    try {
      // Check if any data remains
      Long totalCount = (Long) entityManager.createNativeQuery(
          "SELECT " +
              "(SELECT COUNT(*) FROM project) + " +
              "(SELECT COUNT(*) FROM digital_object) + " +
              "(SELECT COUNT(*) FROM datastream) + " +
              "(SELECT COUNT(*) FROM gams_collection) + " +
              "(SELECT COUNT(*) FROM dublin_core_entry)"
      ).getSingleResult();

      return totalCount == 0;

    } catch (Exception e) {
      log.error("Error verifying cleanup", e);
      return false;
    }
  }
}


