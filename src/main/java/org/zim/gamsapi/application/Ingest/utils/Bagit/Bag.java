package org.zim.gamsapi.application.Ingest.utils.Bagit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.application.Ingest.exceptions.ExportProcessingException;
import org.zim.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.application.Ingest.utils.Bagit.mapping.BagSipJson;
import org.zim.gamsapi.domain.Datastream.DatastreamId;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Abstraction around incoming GAMS5-bags during ingest.
 * Represents bags on the local file-system.
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
     * Metadata obtained from bagit.txt file.
     * Bag version + encoding of tag files.
     */
  private BagMeta bagMeta;

  /**
   * Path to the bag directory (where bag-info.txt etc. are located)
   * on the local filesystem
   */
  final private Path BAG_DIR_PATH;

  /**
   * Constructor that takes the path to a local bag directory.
   * Reads the bag from the filesystem and instantiates representing objects accordingly.
   * @param BAG_DIR_PATH path to the local bag directory
   *                     (where bag-info.txt etc. are located)
   */
  public Bag(Path BAG_DIR_PATH) {
    this.BAG_DIR_PATH = BAG_DIR_PATH;
    readBag();
  }

  /**
   * Constructor that takes already instantiated bag components.
   * Mainly meant for testing purposes AND exporting.
   * @param bagInfo the bag info
   * @param bagMeta the bag meta
   * @param bagData the bag data
   */
  public Bag(BagInfo bagInfo, BagMeta bagMeta, BagData bagData) {
    this.BAG_DIR_PATH = null;
    this.bagInfo = bagInfo;
    this.bagMeta = bagMeta;
    this.bagData = bagData;
  }

  /**
   * Reads bag from the local bag directory path defined via constructor and
   * instantiates representing objects accordingly.
   * Throws IngestProcessingException in case of any problems (if checksums don't have the required length / are empty).
   */
  private void readBag() {

    this.bagInfo = BagDirectoryReader.readBagInfoFile(this.BAG_DIR_PATH);
    this.bagMeta = BagDirectoryReader.readBagItTxtFile(this.BAG_DIR_PATH);

    // read in expected checksum files from bag (e.g. manifest-sha512.txt)
    var bagPathSha512Map = BagDirectoryReader.readSha512ManifestFile(this.BAG_DIR_PATH);
    var bagPathMd5Map = BagDirectoryReader.readMd5ManifestFile(this.BAG_DIR_PATH);

    // handle sip json
    BagSipJson bagSipJson = BagDirectoryReader.readSipJson(this.BAG_DIR_PATH);

    String sipJsonMd5 = bagPathMd5Map.get(BagFilePaths.BAG_SIP_JSON.name);
    String sipJsonSHA512 = bagPathSha512Map.get(BagFilePaths.BAG_SIP_JSON.name);

    if(sipJsonMd5 == null || sipJsonMd5.length() != 32){
      String msg = String.format("MD5 checksum for sip.json is unexpectedly not valid - Got value %s for bag: %s", sipJsonMd5, bagSipJson);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    if(sipJsonSHA512 == null || sipJsonSHA512.length() != 128){
      String msg = String.format("SHA512 checksum for sip.json is unexpectedly not valid - Got value %s for bag: %s", sipJsonSHA512, bagSipJson);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    BagData bagData = BagData.builder()
        .id(bagSipJson.getRecid())
        .project(bagSipJson.getProject())
        .title(bagSipJson.getTitle())
        .objectType(bagSipJson.getObjectType())
        .description(bagSipJson.getDescription())
        .creator(bagSipJson.getCreator())
        .rights(bagSipJson.getRights())
        .publisher(bagSipJson.getPublisher())
        .funder(bagSipJson.getFunder())
        .mainResource(bagSipJson.getMainResource())
        .contentFiles(new HashSet<>())  // this is being populated below
        .md5Checksum(sipJsonMd5)
        .sha512Checksum(sipJsonSHA512)
        // bag related fields
        .schema(bagSipJson.getSchema())
        .createdBy(bagSipJson.getCreated_by())
        .source(bagSipJson.getSource())
        .build();

    // sipjson content files
    for(var contentFile : bagSipJson.getContentFiles()){
      var dsid = contentFile.getDsid();

      // extract checksums for this content file from the maps read from the manifests
      String md5 = bagPathMd5Map.get(contentFile.getBagpath());
      String sha512 = bagPathSha512Map.get(contentFile.getBagpath());

      if(md5.length() != 32){
        String msg = String.format("MD5 checksum for file %s is unexpectedly not valid: %s", contentFile.getBagpath(), md5);
        log.error(msg);
        throw new IngestProcessingException(msg);
      }

      if(sha512.length() != 128){
        String msg = String.format("SHA512 checksum for file %s is unexpectedly not valid: %s", contentFile.getBagpath(), sha512);
        log.error(msg);
        throw new IngestProcessingException(msg);
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
   * @return set of BagFiles representing the content files in the bag
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

  /**
   * Writes a bag as a zip to the given output stream.
   * Streams the datastream content from the given datastream content repository to the output stream.
   * @param outputStream the output stream to write the bag zip to
   * @param datastreamContentRepository needed to stream the datastream content to the output stream.
   */
  public void writeAsZipToStream(OutputStream outputStream, IDatastreamContentRepository datastreamContentRepository) {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

      // 01. write sip.json
      String sipJsonContent =  writeSipJson(zipOutputStream);
      // 02. write manifests (with checksums for sip.json)
      writeManifests(zipOutputStream, sipJsonContent);

      long payloadSize = 0;
      payloadSize += sipJsonContent.getBytes(StandardCharsets.UTF_8).length;
      // 03. write bag content
      payloadSize += writeBagContent(zipOutputStream, datastreamContentRepository);

      // 04. write bag-info.txt (with payload size)
      writeBagInfo(zipOutputStream, payloadSize);

      // write bagit.txt
      writeBagitTxt(zipOutputStream);


    } catch (IOException e) {
      String msg = String.format("Error writing bag %s to zip output stream. Original error: %s", bagData.getId(), e);
      log.error(msg);
      throw new ExportProcessingException(msg);
    }

  }
  /**
   * Writes bagit.txt to the given ZipOutputStream.
   * @param zipOut the zip output stream to write to
   * @throws IOException in case of any problems
   */
  private void writeBagitTxt(ZipOutputStream zipOut) throws IOException {
    String bagItTxtContent = bagMeta.toBagItTxtContent();
    writeTextEntry(zipOut, bagData.getId() + "/bagit.txt", bagItTxtContent);
  }

  /**
   * Writes bag-info.txt to the given ZipOutputStream.
   * Adds given payload oxum to the bag-info.txt.
   *
   * @param zipOut the zip output stream to write to
   * @param payloadOxum the payload oxum to write
   * @throws IOException in case of any problems
   */
  private void writeBagInfo(ZipOutputStream zipOut, float payloadOxum) throws IOException {
    var bagInfoAsTxt = bagInfo.toBagInfoContent(payloadOxum);
    writeTextEntry(zipOut, bagData.getId() + "/bag-info.txt", bagInfoAsTxt);
  }

  /**
   * Writes the datastream content to the given ZipOutputStream.
   * Streams the datastream content from the given datastream content repository to the output stream.
   * @param zipOutputStream the zip output stream to write to
   * @param datastreamContentRepository needed to stream the datastream content to the output stream.
   * @return the total number of bytes written
   */
  private long writeBagContent(ZipOutputStream zipOutputStream, IDatastreamContentRepository datastreamContentRepository) {

    long writtenBytesCount = 0;

    // 03b add datastream content to bag-zip
    // TODO hardcoded BUFFER_SIZE?
    int BUFFER_SIZE = 8192;
    byte[] buffer = new byte[BUFFER_SIZE];
    for (BagFile bagFile : bagData.getContentFiles()) {
      writtenBytesCount += bagFile.getSize();

      // TODO weird variable name
      String fullPath = bagData.getId() + "/" + bagFile.getBagpath();
      log.trace("Writing datastream content to bag path: {}", fullPath);

      ZipEntry entry = new ZipEntry(fullPath);
      entry.setSize(bagFile.getSize());
      try {
        zipOutputStream.putNextEntry(entry);
      } catch (IOException e) {
        String msg = String.format("Error creating zip entry for %s in bag %s. Original error: %s", fullPath, bagData.getId(), e);
        log.error(msg);
        throw new ExportProcessingException(msg);
      }

      DatastreamId datastreamId = DatastreamId.builder()
          .dsid(bagFile.getDsid())
          .digitalObject(bagData.getId())
          .build();

      // Stream content with checksum calculation
      try (InputStream contentStream = datastreamContentRepository.findById(datastreamId).getInputStream()) {
        int bytesRead;
        while ((bytesRead = contentStream.read(buffer)) != -1) {
          zipOutputStream.write(buffer, 0, bytesRead);
        }
        zipOutputStream.closeEntry();
        log.debug("Finished writing datastream content: {}", fullPath);
      } catch (Exception e) {
        String msg = String.format("Failed to stream datastream content for %s", datastreamId);
        log.error(msg, e);
        throw new ExportProcessingException(msg);
      }
    }

    return writtenBytesCount;

  }

  /**
   * Writes manifest files to the given ZipOutputStream.
   * Calculates checksums for given sip.json and adds them to the manifests.
   * @param zipOut the zip output stream to write to
   * @param  sipJson the content of the sip.json file (to calculate checksums for)
   * @throws IOException in case of any problems
   */
  private void writeManifests(ZipOutputStream zipOut, String sipJson) throws IOException {

    // Manifest builders - accumulate during streaming
    final StringBuilder md5Manifest = new StringBuilder();
    final StringBuilder sha512Manifest = new StringBuilder();

    String sha512Checksum;
    String md5Checksum;

    //calculacte sha-256 and md5 for sip.json
    try {
      var sipJsonBytes = sipJson.getBytes(StandardCharsets.UTF_8);
      // calc checksum for sha-512
      var sha512Digest = MessageDigest.getInstance("SHA-512");
      sha512Digest.update(sipJsonBytes);
      byte[] sha512ChecksumBytes = sha512Digest.digest();
      sha512Checksum = bytesToHex(sha512ChecksumBytes);
      // calc md5 checksum for sip.json
      var md5Digest = MessageDigest.getInstance("MD5");
      md5Digest.update(sipJsonBytes);
      byte[] md5ChecksumBytes = md5Digest.digest();
      md5Checksum = bytesToHex(md5ChecksumBytes);
    } catch (NoSuchAlgorithmException e) {
      String msg = String.format("Error calculating SHA-512 checksum for sip.json in bag %s. Original error: %s", bagData.getId(), e);
      log.error(msg);
      throw new ExportProcessingException(msg);
    }

    // TODO own method for writing manifest entries?

    // sip.json manifest entry
    sha512Manifest
        .append(sha512Checksum)
        .append(" ")
        .append(BagFilePaths.BAG_SIP_JSON.name)
        .append("\n");

    // add md5 manifest entry
    md5Manifest
        .append(md5Checksum)
        .append(" ")
        .append(BagFilePaths.BAG_SIP_JSON.name)
        .append("\n");

    // add manifest entries for content files
    bagData.getContentFiles().forEach(contentFile -> {
      md5Manifest.append(contentFile.getMd5Checksum())
          .append(" ")
          .append(contentFile.getBagpath())
          .append("\n");

      sha512Manifest.append(contentFile.getSha512Checksum())
          .append(" ")
          .append(contentFile.getBagpath())
          .append("\n");
    });

    writeTextEntry(zipOut, bagData.getId() + "/manifest-md5.txt", md5Manifest.toString());
    writeTextEntry(zipOut, bagData.getId() + "/manifest-sha512.txt", sha512Manifest.toString());

  }

  /**
   * Writes the sip.json file to the given ZipOutputStream.
   * @param zipOut the zip output stream to write to
   * @return sip.json content as string
   * @throws IOException in case of any problems
   */
  private String writeSipJson(ZipOutputStream zipOut) throws IOException {
    String sipJsonContent = bagData.toSipJsonContent();
    writeTextEntry(zipOut, bagData.getId() + "/" + BagFilePaths.BAG_SIP_JSON.name, sipJsonContent);
    return sipJsonContent;
  }

  /**
   * Writes file-content as string to a ZipOutputStream.
   * @param zipOut the zip to which the file should be written to
   * @param path the path (including filename) within the zip
   * @param content the content of the file (as string)
   * @throws IOException in case of any problems
   */
  private void writeTextEntry(ZipOutputStream zipOut, String path, String content) throws IOException {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    ZipEntry entry = new ZipEntry(path);
    entry.setSize(bytes.length);
    zipOut.putNextEntry(entry);
    zipOut.write(bytes);
    zipOut.closeEntry();
  }

  /**
   * Converts byte array to hex string.
   * @param bytes the byte array to convert
   * @return the hex string
   */
  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

}
