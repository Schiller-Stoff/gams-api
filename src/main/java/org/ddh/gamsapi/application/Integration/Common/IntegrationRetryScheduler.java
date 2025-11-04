package org.ddh.gamsapi.application.Integration.Common;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.enums.IntegrationStatus;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationFailureRepository;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntegrationRetryScheduler {

  private final IIntegrationFailureRepository failureRepository;
  private final List<IIntegrationService> integrationServices;

  // Runs every 5 minutes
  @Scheduled(fixedDelay = 300000, initialDelay = 60000)
  @Transactional
  public void retryFailedIntegrations() {
    log.debug("Checking for failed integrations to retry");

    List<IntegrationFailure> failures = failureRepository
        .findByStatusAndNextRetryAtBefore("PENDING", LocalDateTime.now());

    for (IntegrationFailure failure : failures) {
      if (failure.getRetryCount() >= failure.getMaxRetries()) {
        log.warn("Max retries reached for {}. Marking as FAILED", failure.getId());
        failure.setStatus("FAILED");
        failureRepository.save(failure);
        continue;
      }

      try {
        // Find the right service
        IIntegrationService service = findService(failure.getServiceName());

        if (IntegrationStatus.INDEX.equals(failure.getOperation())) {
          service.indexObject(failure.getProjectAbbr(), failure.getDigitalObjectId());
        } else if (IntegrationStatus.DELETE.equals(failure.getOperation())) {
          service.deleteIndexedObject(failure.getProjectAbbr(), failure.getDigitalObjectId());
        }

        // Success! Delete the failure record
        failureRepository.delete(failure);
        log.info("Successfully retried integration {}", failure.getId());

      } catch (Exception e) {
        log.error("Retry failed for {}. Error: {}", failure.getId(), e.getMessage());

        // Update retry count with exponential backoff
        failure.setRetryCount(failure.getRetryCount() + 1);
        int backoffMinutes = (int) Math.pow(2, failure.getRetryCount()) * 5; // 5, 10, 20, 40, 80 minutes
        failure.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));
        failure.setErrorMessage(e.getMessage());
        failureRepository.save(failure);
      }
    }
  }

  private IIntegrationService findService(String serviceName) {
    return integrationServices.stream()
        .filter(s -> s.getClass().getSimpleName().contains(serviceName))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));
  }
}
