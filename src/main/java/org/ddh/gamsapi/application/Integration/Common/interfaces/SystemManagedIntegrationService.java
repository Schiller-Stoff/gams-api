package org.ddh.gamsapi.application.Integration.Common.interfaces;

/**
 * Marker interface for system-managed integration services.
 * Tightly coupled with system events
 * (will be handled via spring's event system and dead letter queue for retries)
 */
public interface SystemManagedIntegrationService extends IIntegrationService {
}
