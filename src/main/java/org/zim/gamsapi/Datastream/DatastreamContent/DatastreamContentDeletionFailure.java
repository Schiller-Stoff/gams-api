package org.zim.gamsapi.Datastream.DatastreamContent;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Entity representing a failed deletion of a datastream's content file.
 */
@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatastreamContentDeletionFailure {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "datastream_dsid", nullable = false)
  private String datastreamDsid;

  @Column(name = "digital_object_id", nullable = false)
  private String digitalObjectId;

  @Column(name = "retry_count")
  private Integer retryCount = 0;

  @Column(name = "max_retries")
  private Integer maxRetries = 10000000;


}
