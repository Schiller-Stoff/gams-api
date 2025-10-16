package org.ddh.gamsapi.application.Integration.Common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ddh.gamsapi.application.Integration.Common.enums.IntegrationStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "integration_failure")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationFailure {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String serviceName; // "BaseSearch", "CustomIndex", etc.

  @Column(nullable = false)
  private String projectAbbr;

  @Column(nullable = false)
  private String digitalObjectId;

  @Column(nullable = false)
  private String operation; // "INDEX", "DELETE"

  @Lob
  @Column
  private String errorMessage;

  @Column
  private Integer retryCount = 0;

  @Column
  private Integer maxRetries = 5;

  @Column
  @CreationTimestamp
  private LocalDateTime failedAt;

  @Column
  private LocalDateTime nextRetryAt;

  @Column
  private String status = IntegrationStatus.PENDING; // PENDING, RETRYING, FAILED, SUCCEEDED
}
