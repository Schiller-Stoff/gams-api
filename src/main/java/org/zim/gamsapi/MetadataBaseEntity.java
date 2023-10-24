package org.zim.gamsapi;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Handles shared metadata fields between DigitalObject and Datastream
 *
 * https://www.dublincore.org/
 */
@Embeddable
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataBaseEntity {

  /**
   * Title of digital object / datastream
   */
  @NotEmpty
  private String title;

  /**
   * Rights statement of digital object / datastream
   */
  @NotEmpty
  private String rights;

  /**
   * Publisher of the digital object or datastream
   */
  @NotEmpty
  private String publisher;

  /**
   * Creator of digital object or datastream
   */
  @NotEmpty
  private String creator;

  /**
   * Description if the digital object or datastream
   */
  @Size(min = 5, max = 2000)
  @NotEmpty
  private String description;


  /**
   * Arbitrarily associated format with the digital object / datastream
   * e.g. datastream --> book-page
   * e.g. digital object --> book
   */
  //private String format;


  /**
   * Arbitrarily associated subjects with the digital object / datastreams
   */
  //private Set<String> subject;

  /**
   * Arbitrarily associated contributor
   */
  //private String contributor;

  /**
   * Arbitrarily associated date with the digital object / datastream.
   */
  //private String date;

  //private String type;

  //private String source;

  // system controlled - always english?
  //private String language;

  //private String relation;

  //private String coverage;

}
