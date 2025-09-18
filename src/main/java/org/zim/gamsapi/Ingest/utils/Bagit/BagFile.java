package org.zim.gamsapi.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO jdoc
 * TODO test
 * TODO rename also to something like BagContentFile?
 */
@Builder
public class BagFile {

  /**
   * Size of the file in bytes.
   */
  @NotNull
  private long size;

  /**
   * Path to the file in the bagit archive.
   */
  @NotEmpty
  private String bagpath;

  /**
   * Id of the datastream to be created
   */
  @NotEmpty
  private String dsid;


  /**
   * Mimetype of the file.
   */
  @NotEmpty
  private String mimetype;


  /**
   * Title of the datastream
   */
  @NotEmpty
  private String title;


  /**
   * Description of the datastream
   */
  @Size(max = 2000)
  private String description;


  /**
   * Creator of the datastream
   */
  @NotEmpty
  private String creator;


  /**
   * Rights statement of the datastream
   */
  @NotEmpty
  private String rights;

  /**
   * Tags of the datastream / content file mapped from the sip.json
   */
  @NotNull
  private Set<String> tags = new HashSet<>();

  /**
   * Language of the datastream / content file mapped from the sip.json
   */
  @NotNull
  private Set<String> lang = new HashSet<>();

  /**
   * Checksum of the file using md5 algorithm.
   */
  @NotEmpty
  private String md5Checksum;

  /**
   * Checksum of the file using sha256 algorithm.
   */
  @NotEmpty
  private String sha512Checksum;

  // TODO think about
  // method like this?
  public void toDatastream(){
    // TODO best added to BagitContentFile itself?
  }

  /**
   * TODO implement
   */
  public void toDatastreamContent(){

  }

}
