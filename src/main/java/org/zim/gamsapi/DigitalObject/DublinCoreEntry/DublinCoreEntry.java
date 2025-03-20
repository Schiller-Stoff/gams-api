package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.zim.gamsapi.DigitalObject.DigitalObject;

/**
 * Entity class containing all dublin core elements assigned in the gams-api.
 * Pointing to the digital object it belongs to.
 */
@Entity
@Table( indexes = {
    @Index(name = "idx_dc_digital_object_id", columnList = "digital_object_id"),
    @Index(name = "idx_dc_name", columnList = "name"),
    @Index(name = "idx_dc_value", columnList = "value")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DublinCoreEntry {

  /**
   * Generated unique identifier for the dublin core element
   * (unique for the full gams-api).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  /**
   * Digital object the dublin core element belongs to.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "digital_object_id", nullable = false)
  @NotNull
  private DigitalObject digitalObject;

  /**
   * Dublin core element name.
   */
  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /**
   * Dublin core element value.
   */
  @Column(name = "value", nullable = false)
  private String value;


}
