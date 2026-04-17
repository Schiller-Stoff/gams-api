package org.ddh.gamsapi.application.WebDeployment.dto;

import java.time.Instant;

/**
 * Deployment metadata returned by the web deployment endpoints.
 */
public record WebDeploymentInfo(
    String projectAbbr,
    Instant deployedAt,
    String deployedBy,
    int fileCount,
    long totalSize
) {}