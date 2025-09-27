package org.zim.gamsapi.Ingest.utils.Bagit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagitSipJson;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Abstraction around incoming GAMS5-bags during ingest.
 * Represents bags on the local file-system.
 * TODO test
 *
 */
@Slf4j
@Getter
public class Bag {

  /**
   * Metadata obtained from bag-info file.
   */
  private BagInfo bagInfo;

  /**
   * Encapsules data obtained from sip.json + checksum read from
   * checksum files.
   * (carries the checksum of the sip.json itself)
   */
  private BagData bagData;

  /**
   * Path to the bag directory (where bag-info.txt etc. are located)
   * on the local filesystem
   */
  final private Path BAG_DIR_PATH;

  public Bag(Path BAG_DIR_PATH) throws IOException {
    this.BAG_DIR_PATH = BAG_DIR_PATH;
    readBag();
  }


  /**
   * Reads bag from the local bag directory path defined via constructor and
   * instantiates representing objects accordingly.
   */
  private void readBag() throws IOException {

    // read and validate bagit structure
    this.bagInfo = BagItDirectoryReader.extractBagItInfo(this.BAG_DIR_PATH);

    // read in expected checksum files from bag (e.g. manifest-sha512.txt)
    var bagPathSha512Map = BagItDirectoryReader.extractBagPathSha512Map(this.BAG_DIR_PATH);
    var bagPathMd5Map = BagItDirectoryReader.extractBagPathMd5Map(this.BAG_DIR_PATH);

    // handle sip json
    BagitSipJson bagitSipJson = BagItDirectoryReader.extractAndValidateSipJson(this.BAG_DIR_PATH);

    String sipJsonMd5 = bagPathMd5Map.get(BagItFilePaths.BAG_SIP_JSON.name);
    String sipJsonSHA512 = bagPathSha512Map.get(BagItFilePaths.BAG_SIP_JSON.name);

    BagData bagData = BagData.builder()
        .id(bagitSipJson.getId())
        .project(bagitSipJson.getProject())
        .title(bagitSipJson.getTitle())
        .objectType(bagitSipJson.getObjectType())
        .description(bagitSipJson.getDescription())
        .creator(bagitSipJson.getCreator())
        .rights(bagitSipJson.getRights())
        .publisher(bagitSipJson.getPublisher())
        .funder(bagitSipJson.getFunder())
        .mainResource(bagitSipJson.getMainResource())
        .contentFiles(new HashSet<>())  // this is being populated below
        .types(bagitSipJson.getTypes())
        .md5Checksum(sipJsonMd5)
        .sha512Checksum(sipJsonSHA512)
        .build();

    // sipjson content files
    for(var contentFile : bagitSipJson.getContentFiles()){
      var dsid = contentFile.getDsid();

      // extract checksums for this content file from the maps read from the manifests
      String md5 = bagPathMd5Map.get(contentFile.getBagpath());
      String sha512 = bagPathSha512Map.get(contentFile.getBagpath());

      // TODO test IOException?
      if(md5.length() != 32){
        String msg = String.format("MD5 checksum for file %s is unexpectedly not valid: %s", contentFile.getBagpath(), md5);
        log.error(msg);
        throw new IOException(msg);
      }

      // TODO test IOException?
      if(sha512.length() != 128){
        String msg = String.format("SHA512 checksum for file %s is unexpectedly not valid: %s", contentFile.getBagpath(), sha512);
        log.error(msg);
        throw new IOException(msg);
      }

      BagFile bagFile = BagFile.builder()
          .bagpath(contentFile.getBagpath())
          .creator(contentFile.getCreator())
          .description(contentFile.getDescription())
          .dsid(dsid)
          .mimetype(contentFile.getMimetype())
          .size(contentFile.getSize())
          .title(contentFile.getTitle())
          .rights(contentFile.getRights())
          .md5Checksum(md5)
          .sha512Checksum(sha512)
          .tags(contentFile.getTags())
          .lang(contentFile.getLang())
          .build();

      bagData.getContentFiles().add(bagFile);
    }

    this.bagData = bagData;
  }

  /**
   * Allows to quickly access all content files in the bag.
   * @return
   */
  public Set<BagFile> getContentFiles(){
    return this.bagData.getContentFiles();
  }

  /**
   * Find a content file by its dsid.
   * @param dsid the dsid to search for
   * @return the BagFile with the given dsid
   */
  public BagFile findContentFileByDsid(String dsid){
    for(BagFile bagFile : this.bagData.getContentFiles()){
      if(bagFile.getDsid().equals(dsid)){
        return bagFile;
      }
    }
    String msg = String.format("No content file with dsid %s found in bag %s", dsid, this.bagData.getId());
    log.error(msg);
    throw new NoSuchElementException(msg);
  }


  public void loopContentFiles(){

  }



}
