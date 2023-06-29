package org.zim.gamsapi.DigitalObject;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.MetadataBaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain object representing a digital object in sense of OAIS.
 */
@Entity
@Table(name = "digital_object")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalObject {

  /**
   * PID of the digital object
   */
  @Id
  // @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "pid")
  @NotBlank
  private String pid;

  @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "digitalObject")
  @Builder.Default
  @JsonManagedReference // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  private List<Datastream> datastreams = new ArrayList<>();

  /**
   * Content Model representation
   */
  @Column(name = "object_type")
  private String objectType;


  /**
   * Project this digital object belongs to
   */
  @Column(name = "projectAbbr", nullable = false)
  private String projectAbbr;


  @Embedded
  // increases allowed length of description based on the EmbeddedEntity
  @AttributeOverride(name = "description", column = @Column(length = 2000))
  @Valid  // Add Bean validation to embedded tables
  private MetadataBaseEntity baseMetadata;

}
