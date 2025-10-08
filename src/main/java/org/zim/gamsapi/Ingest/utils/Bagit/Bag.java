package org.zim.gamsapi.Ingest.utils.Bagit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.Ingest.IngestRecord;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
     * Metadata obtained from bagit.txt file.
     * Bag version + encoding of tag files.
     */
  private BagMeta bagMeta;

  /**
   * Path to the bag directory (where bag-info.txt etc. are located)
   * on the local filesystem
   */
  final private Path BAG_DIR_PATH;

  public Bag(Path BAG_DIR_PATH) {
    this.BAG_DIR_PATH = BAG_DIR_PATH;
    readBag();
  }

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
        .types(bagSipJson.getTypes())
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
   * TODO jdoc
   * TODO test?
   * @param zipOutputStream
   */
  public void writeToZip(ZipOutputStream zipOutputStream, IDatastreamContentRepository datastreamContentRepository) {
    try {
      //1. write bagit.txt
      writeBagitTxt(zipOutputStream);
      writeBagInfo(zipOutputStream);
      writeSipJson(zipOutputStream);
      writeManifests(zipOutputStream);

      // 03b add datastream content
      // TODO what is with the datastream content?
      // TODO maybe i can load - because i have the content files available in the bag! (method would only need the datastreamContentRepository as argument!)
      // loop through datastreams and then fetch content from filesystem repository
      int BUFFER_SIZE = 8192;
      byte[] buffer = new byte[BUFFER_SIZE];
      for(BagFile bagFile : bagData.getContentFiles()){

        // TODO werid variable name
        String fullPath = bagData.getId() + "/" + bagFile.getBagpath();

        // TODO think about log msg
        log.debug("Writing datastream content to bag path: {}", fullPath);

        ZipEntry entry = new ZipEntry(fullPath);
        entry.setSize(bagFile.getSize());
        zipOutputStream.putNextEntry(entry);

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
          // TODO rethink exception
          String msg = String.format("Failed to stream datastream content for %s", datastreamId);
          log.error(msg, e);
          throw new IOException(msg, e);
        }
      }


    } catch (IOException e) {
      // TODO better error message
      String msg = String.format("Error writing bag %s to zip output stream. Original error: %s", bagData.getId(), e);
      log.error(msg);
      // TODO different exception!
      throw new IngestProcessingException(msg);
    }


  }


  private void writeBagitTxt(ZipOutputStream zipOut) throws IOException {
    String BAG_VERSION = this.bagMeta.getBagItVersion();
    String TAG_FILE_ENCODING = this.bagMeta.getTagFileCharacterEncoding();
    String content = String.format("BagIt-Version: %s%nTag-File-Character-Encoding: %s%n",
        BAG_VERSION, TAG_FILE_ENCODING);

    writeTextEntry(zipOut, bagData.getId() + "/bagit.txt", content);
  }

  private void writeBagInfo(ZipOutputStream zipOut) throws IOException {
    Instant timestamp = bagInfo.getBaggingTimeStamp();
    String date = timestamp.atZone(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    String time = timestamp.atZone(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_TIME) + " UTC";

    String content = String.format(
        "Bagging-Date: %s%n" +
            "Bagging-Time: %s%n" +
            "Contact-Email: %s%n" +
            "External-Description: %s%n" +
            "Payload-Oxum: %s%n",
        date,
        time,
        bagInfo.getContactMail(),
        bagInfo.getExternalDescription(),
        bagInfo.getPayloadOxum()
    );

    writeTextEntry(zipOut, bagData.getId() + "/bag-info.txt", content);
  }

  /**
   * TODO implement - needs all checksums?
   * @param zipOut
   * @throws IOException
   */
  private void writeManifests(ZipOutputStream zipOut) throws IOException {

    // Manifest builders - accumulate during streaming
    final StringBuilder md5Manifest = new StringBuilder();
    final StringBuilder sha512Manifest = new StringBuilder();

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
   * TODO jdoc
   * @param zipOut
   * @throws IOException
   */
  private void writeSipJson(ZipOutputStream zipOut) throws IOException {
    // Build sip.json from digital object metadata
    Map<String, Object> sipJson = new LinkedHashMap<>();
    sipJson.put("recid", bagData.getId());
    sipJson.put("project", bagData.getProject());
    sipJson.put("title", bagData.getTitle());
    sipJson.put("objectType", bagData.getObjectType());
    sipJson.put("description", bagData.getDescription());
    sipJson.put("creator", bagData.getCreator());
    sipJson.put("rights", bagData.getRights());
    sipJson.put("publisher", bagData.getPublisher());

    if (bagData.getFunder() != null) {
      sipJson.put("funder", bagData.getFunder());
    }

    if (bagData.getMainResource() != null) {
      sipJson.put("mainResource", bagData.getMainResource());
    }

    List<Map<String, Object>> contentFiles = bagData.getContentFiles().stream().map(contentFile -> {
      Map<String, Object> fileMap = new LinkedHashMap<>();
      fileMap.put("dsid", contentFile.getDsid());
      fileMap.put("filename", contentFile.getBagpath());
      fileMap.put("mimetype", contentFile.getMimetype());
      fileMap.put("title", contentFile.getTitle());
      fileMap.put("description", contentFile.getDescription());
      fileMap.put("creator", contentFile.getCreator());
      fileMap.put("rights", contentFile.getRights());
      fileMap.put("size", contentFile.getSize());
      fileMap.put("tags", new ArrayList<>(contentFile.getTags()));
      fileMap.put("lang", new ArrayList<>(contentFile.getLang()));
      return fileMap;
    }).collect(Collectors.toList());


    sipJson.put("contentFiles", contentFiles);
    sipJson.put("$schema", bagData.getSchema());
    sipJson.put("created_by", bagData.getCreatedBy());
    sipJson.put("source", bagData.getSource());

    // Convert to JSON
    String jsonContent = toJson(sipJson);

    String sipPath = "data/meta/sip.json";
    writeTextEntry(zipOut, bagData.getId() + "/" + sipPath, jsonContent);

    // Calculate checksums for sip.json and add to manifests
    // TODO?
    // addToManifests(sipPath, jsonContent.getBytes(StandardCharsets.UTF_8));
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


  private String toJson(Map<String, Object> map) {
    // Simple JSON serialization - you should use Jackson in production
    StringBuilder json = new StringBuilder("{\n");
    Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Object> entry = iter.next();
      json.append("  \"").append(entry.getKey()).append("\": ");
      json.append(toJsonValue(entry.getValue()));
      if (iter.hasNext()) {
        json.append(",");
      }
      json.append("\n");
    }
    json.append("}");
    return json.toString();
  }

  @SuppressWarnings("unchecked")
  private String toJsonValue(Object value) {
    if (value == null) {
      return "null";
    } else if (value instanceof String) {
      return "\"" + escapeJson((String) value) + "\"";
    } else if (value instanceof Number) {
      return value.toString();
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      return "[" + list.stream()
          .map(this::toJsonValue)
          .collect(Collectors.joining(", ")) + "]";
    } else if (value instanceof Map) {
      return toJson((Map<String, Object>) value);
    }
    return "\"" + value.toString() + "\"";
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }


}
