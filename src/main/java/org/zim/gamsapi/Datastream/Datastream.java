package org.zim.gamsapi.Datastream;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DCBaseEntity;

/**
 * Domain class for datastream domain objects.
 */
@Entity
@Table(
        name = "datastream",
        // this will guarantee that the dsid field is unique per referenced digital object!
        uniqueConstraints = {@UniqueConstraint(name = "DatastreamNameUniquePerObject", columnNames = { "digital_object_pid", "dsid" })})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Datastream {

  /**
   * Global id of datastream - each datastream has an unique identifier
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne
  @ToString.Exclude
  @JsonBackReference // manages bidirectional reference in json https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion
  private DigitalObject digitalObject;

  /**
   * Digital object specific identifier for the datastream.
   * Like TEI_SOURCE - MUST be unique per Digital object.
   */
  @Column(name = "dsid")
  private String dsid;

  /**
   * Actual contained binary data
   */
  @Column(name = "data")
  @ToString.Exclude
  @JsonIgnore
  private byte[] data;

  /**
   * Mimetype of the contained data.
   */
  @Column(name = "mime_type")
  private String mimeType;

  @Column(name = "file_name")
  private String fileName;



  private String metaAddress = "DEFAULT_VALUE";

  @Embedded
  // increases allowed length of description based on the EmbeddedEntity
  @AttributeOverride(name = "description", column = @Column(length = 2000))
  private DCBaseEntity dublinCore;

}
