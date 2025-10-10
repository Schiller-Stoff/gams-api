package org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.zim.gamsapi.Datastream.Datastream;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a file in the bagit archive along with metadata needed to create a datastream.
 */
@Builder
@Getter
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

    /**
    * Creates a BagFile from a Datastream object.
    * @param datastream the datastream to convert
    * @return the created BagFile
    */
  public static BagFile from(Datastream datastream){
      return BagFile.builder()
              .size(datastream.getSize())
              .bagpath(datastream.getBagPath())
              .dsid(datastream.getDsid())
              .mimetype(datastream.getMimeType())
              .title(datastream.getBaseMetadata().getTitle())
              .description(datastream.getBaseMetadata().getDescription())
              .creator(datastream.getBaseMetadata().getCreator())
              .rights(datastream.getBaseMetadata().getRights())
              .tags(datastream.getTags())
              .lang(datastream.getLang())
              .md5Checksum(datastream.getBaseMetadata().getMd5Checksum())
              .sha512Checksum(datastream.getBaseMetadata().getSha512Checksum())
              .build();
  }

}
