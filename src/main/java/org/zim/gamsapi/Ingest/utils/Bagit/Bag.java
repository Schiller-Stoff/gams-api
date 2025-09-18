package org.zim.gamsapi.Ingest.utils.Bagit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO jdoc
 * TODO test
 *
 */
@Slf4j
@Getter
public class Bag {

  // read and validate
  private BagItInfo bagItInfo;

  // do i need this sipjson?
  private BagitSipJson bagitSipJson;

  /**
   * Path to the bagit directory (where bagit.txt etc. are located)
   */
  private Path bagitDirPath;

  /**
   * List of files in the bag (as represented in the sip.json)
   */
  private List<BagFile> bagFiles = new ArrayList<>();

  public Bag(Path bagitDirPath) throws IOException {
    this.bagitDirPath = bagitDirPath;
    readBag();
  }

  // TODO maybe own representation of files with all required data?
  // e.g. BagFile alongside checksums etc.


  /**
   * Read and validate the bagit structure including the sip.json file.
   */
  private void readBag() throws IOException {

    // read and validate bagit structure
    this.bagItInfo = BagItDirectoryReader.extractBagItInfo(this.bagitDirPath);

    // handle sip json
    BagitSipJson bagitSipJson = BagItDirectoryReader.extractAndValidateSipJson(this.bagitDirPath);
    this.bagitSipJson = bagitSipJson;

    var dsidSha512Map = BagItDirectoryReader.extractBagPathSha512Map(this.bagitDirPath);
    var dsidMd5Map = BagItDirectoryReader.extractBagPathMd5Map(this.bagitDirPath);
    // sipjson content files
    for(var contentFile : bagitSipJson.getContentFiles()){
      var dsid = contentFile.getDsid();

      // TODO how to handle the sip.json? (is not represented in the sip.json itself)
      // TODO should I ignore the sip.json?

      String md5 = dsidMd5Map.get(contentFile.getBagpath());
      String sha512 = dsidSha512Map.get(contentFile.getBagpath());

      if(md5.length() != 32){
        String msg = String.format("MD5 checksum for file %s is unexpectedly not valid: %s", contentFile.getBagpath(), md5);
        log.error(msg);
        throw new IOException(msg);
      }

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
          .md5Checksum(md5)
          .sha512Checksum(sha512)
          .build();


      this.bagFiles.add(bagFile);
    }

  }


  public void loopContentFiles(){

  }



}
