package org.zim.gamsapi.application.Ingest.utils.Bagit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.DatastreamId;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.domain.Datastream.DatastreamContent.DatastreamContentRepository;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BagExportService {

    private final IDigitalObjectRepository digitalObjectRepository;
    private final IDatastreamRepository datastreamRepository;
    private final ISubmissionRecordRepository ingestRecordRepository;
    private final IDublinCoreEntryRepository dublinCoreEntryRepository;
    private final DatastreamContentRepository datastreamContentRepository;

    private static final int BUFFER_SIZE = 8192;
    private static final String BAG_VERSION = "1.0";
    private static final String TAG_FILE_ENCODING = "UTF-8";

    /**
     * Exports a digital object as a streaming BagIt archive.
     * This approach scales to any number of datastreams by streaming content directly to the output.
     *
     * @param digitalObjectId The ID of the digital object to export
     * @return OutputStream consumer that writes the bag to the provided stream
     * @throws BagExportException if export fails
     */
    @Transactional(readOnly = true)
    public BagExportStreamWriter exportDigitalObjectAsBag(String digitalObjectId) throws BagExportException {

        log.info("Starting bag export for digital object: {}", digitalObjectId);

        // 1. Load all required metadata in single transaction
        DigitalObject digitalObject = digitalObjectRepository.findById(digitalObjectId)
                .orElseThrow(() -> {
                    String msg = String.format("Digital object with id %s not found", digitalObjectId);
                    log.error(msg);
                    return new DigitalObjectNotFoundException(msg);
                });

        SubmissionRecord submissionRecord = ingestRecordRepository.findById(digitalObjectId)
                .orElseThrow(() -> {
                    String msg = String.format("Ingest record for digital object %s not found", digitalObjectId);
                    log.error(msg);
                    return new BagExportException(msg);
                });

        Set<Datastream> datastreams = datastreamRepository.findAllByDigitalObject(digitalObject);

        if (datastreams.isEmpty()) {
            String msg = String.format("No datastreams found for digital object %s. At least a dublin core datastream should be present.", digitalObjectId);
            log.warn(msg);
        }

        List<DublinCoreEntry> dublinCoreEntries = dublinCoreEntryRepository.findEntriesByDigitalObjectId(digitalObjectId);

        log.info("Loaded metadata for {} datastreams", datastreams.size());

        // 2. Return a writer that will perform streaming export
        return new BagExportStreamWriter(
                digitalObject,
            submissionRecord,
                datastreams,
                dublinCoreEntries,
                datastreamContentRepository
        );
    }

    /**
     * Inner class that handles the actual streaming export.
     * Separates metadata loading from streaming to keep transaction boundaries clean.
     */
    public static class BagExportStreamWriter {

        private final DigitalObject digitalObject;
        private final SubmissionRecord submissionRecord;
        private final Set<Datastream> datastreams;
        private final List<DublinCoreEntry> dublinCoreEntries;
        private final DatastreamContentRepository datastreamContentRepository;

        // Manifest builders - accumulate during streaming
        private final StringBuilder md5Manifest = new StringBuilder();
        private final StringBuilder sha512Manifest = new StringBuilder();

        public BagExportStreamWriter(
                DigitalObject digitalObject,
                SubmissionRecord submissionRecord,
                Set<Datastream> datastreams,
                List<DublinCoreEntry> dublinCoreEntries,
                DatastreamContentRepository datastreamContentRepository) {
            this.digitalObject = digitalObject;
            this.submissionRecord = submissionRecord;
            this.datastreams = datastreams;
            this.dublinCoreEntries = dublinCoreEntries;
            this.datastreamContentRepository = datastreamContentRepository;
        }

        /**
         * Writes the complete bag to the provided OutputStream.
         * Uses streaming to handle large datastreams efficiently.
         *
         * @param outputStream The stream to write the bag to
         * @throws IOException if writing fails
         */
        public void writeTo(OutputStream outputStream) throws IOException {

            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {

                String bagName = digitalObject.getId() + "-bag";

                // 1. Write bagit.txt
                writeBagitTxt(zipOut, bagName);

                // 2. Write bag-info.txt
                writeBagInfo(zipOut, bagName, submissionRecord);

                // 3. Write sip.json
                writeSipJson(zipOut, bagName);

                // 4. Stream all datastream content files + build manifests
                writeDatastreamContents(zipOut, bagName);

                // 5. Write DC.xml if DC entries exist
                if (!dublinCoreEntries.isEmpty()) {
                    writeDublinCoreXml(zipOut, bagName);
                }

                // 6. Write manifest files (built during content streaming)
                writeManifests(zipOut, bagName);

                zipOut.finish();

                log.info("Successfully exported bag for digital object: {}", digitalObject.getId());
            }
        }

        private void writeBagitTxt(ZipOutputStream zipOut, String bagName) throws IOException {
            String content = String.format("BagIt-Version: %s%nTag-File-Character-Encoding: %s%n",
                    BAG_VERSION, TAG_FILE_ENCODING);

            writeTextEntry(zipOut, bagName + "/bagit.txt", content);
        }

        private void writeBagInfo(ZipOutputStream zipOut, String bagName, SubmissionRecord record) throws IOException {
            Instant timestamp = record.getBaggingTimeStamp();
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
                    record.getBagContactMail(),
                    record.getBagExternalDescription(),
                    record.getBagPayloadOxum()
            );

            writeTextEntry(zipOut, bagName + "/bag-info.txt", content);
        }

        private void writeSipJson(ZipOutputStream zipOut, String bagName) throws IOException {
            // Build sip.json from digital object metadata
            Map<String, Object> sipJson = new LinkedHashMap<>();
            sipJson.put("recid", digitalObject.getId());
            sipJson.put("project", digitalObject.getProject().getProjectAbbr());
            sipJson.put("title", digitalObject.getBaseMetadata().getTitle());
            sipJson.put("objectType", digitalObject.getObjectType());
            sipJson.put("description", digitalObject.getBaseMetadata().getDescription());
            sipJson.put("creator", digitalObject.getBaseMetadata().getCreator());
            sipJson.put("rights", digitalObject.getBaseMetadata().getRights());
            sipJson.put("publisher", digitalObject.getPublisher());

            if (digitalObject.getFunder() != null) {
                sipJson.put("funder", digitalObject.getFunder());
            }

            if (digitalObject.getMainResource() != null) {
                sipJson.put("mainResource", digitalObject.getMainResource());
            }

            // Add content files metadata
            List<Map<String, Object>> contentFiles = datastreams.stream()
                    .map(ds -> {
                        Map<String, Object> fileMap = new LinkedHashMap<>();
                        fileMap.put("dsid", ds.getDsid());
                        fileMap.put("filename", ds.getBagPath());
                        fileMap.put("mimetype", ds.getMimeType());
                        fileMap.put("title", ds.getBaseMetadata().getTitle());
                        fileMap.put("description", ds.getBaseMetadata().getDescription());
                        fileMap.put("creator", ds.getBaseMetadata().getCreator());
                        fileMap.put("rights", ds.getBaseMetadata().getRights());
                        fileMap.put("size", ds.getSize());
                        fileMap.put("tags", new ArrayList<>(ds.getTags()));
                        fileMap.put("lang", new ArrayList<>(ds.getLang()));
                        return fileMap;
                    })
                    .collect(Collectors.toList());

            sipJson.put("contentFiles", contentFiles);
            sipJson.put("$schema", submissionRecord.getBagSchema());
            sipJson.put("created_by", submissionRecord.getBagCreatedBy());
            sipJson.put("source", submissionRecord.getBagSource());

            // Convert to JSON
            String jsonContent = toJson(sipJson);

            String sipPath = "data/meta/sip.json";
            writeTextEntry(zipOut, bagName + "/" + sipPath, jsonContent);

            // Calculate checksums for sip.json and add to manifests
            addToManifests(sipPath, jsonContent.getBytes(StandardCharsets.UTF_8));
        }

        private void writeDatastreamContents(ZipOutputStream zipOut, String bagName) throws IOException {

            byte[] buffer = new byte[BUFFER_SIZE];

            for (Datastream datastream : datastreams) {

                String relativePath = "data/content/" + datastream.getBagPath();
                String fullPath = bagName + "/" + relativePath;

                log.debug("Writing datastream content: {}", relativePath);

                ZipEntry entry = new ZipEntry(fullPath);
                entry.setSize(datastream.getSize());
                zipOut.putNextEntry(entry);

                DatastreamId datastreamId = datastream.deriveDatastreamId();

                // Stream content with checksum calculation
                try (InputStream contentStream = datastreamContentRepository.findById(datastreamId).getInputStream();
                     ChecksumInputStream checksumStream = new ChecksumInputStream(contentStream)) {

                    int bytesRead;
                    while ((bytesRead = checksumStream.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }

                    zipOut.closeEntry();

                    // Add checksums to manifests
                    md5Manifest.append(checksumStream.getMd5Checksum())
                            .append(" ")
                            .append(relativePath)
                            .append("\n");

                    sha512Manifest.append(checksumStream.getSha512Checksum())
                            .append(" ")
                            .append(relativePath)
                            .append("\n");

                } catch (Exception e) {
                    String msg = String.format("Failed to stream datastream content for %s", datastreamId);
                    log.error(msg, e);
                    throw new IOException(msg, e);
                }
            }
        }

        private void writeDublinCoreXml(ZipOutputStream zipOut, String bagName) throws IOException {

            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");

            for (DublinCoreEntry entry : dublinCoreEntries) {
                xml.append("  <dc:").append(entry.getName());
                if (entry.getLanguage() != null && !entry.getLanguage().isEmpty()) {
                    xml.append(" xml:lang=\"").append(entry.getLanguage()).append("\"");
                }
                xml.append(">")
                        .append(escapeXml(entry.getValue()))
                        .append("</dc:").append(entry.getName()).append(">\n");
            }

            xml.append("</metadata>\n");

            String dcPath = "data/content/DC.xml";
            String xmlContent = xml.toString();
            writeTextEntry(zipOut, bagName + "/" + dcPath, xmlContent);

            // Add DC.xml to manifests
            addToManifests(dcPath, xmlContent.getBytes(StandardCharsets.UTF_8));
        }

        private void writeManifests(ZipOutputStream zipOut, String bagName) throws IOException {
            writeTextEntry(zipOut, bagName + "/manifest-md5.txt", md5Manifest.toString());
            writeTextEntry(zipOut, bagName + "/manifest-sha512.txt", sha512Manifest.toString());
        }

        private void addToManifests(String relativePath, byte[] content) throws IOException {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                MessageDigest sha512 = MessageDigest.getInstance("SHA-512");

                md5.update(content);
                sha512.update(content);

                md5Manifest.append(bytesToHex(md5.digest()))
                        .append(" ")
                        .append(relativePath)
                        .append("\n");

                sha512Manifest.append(bytesToHex(sha512.digest()))
                        .append(" ")
                        .append(relativePath)
                        .append("\n");

            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Checksum algorithm not available", e);
            }
        }

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

        private String escapeXml(String str) {
            return str.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        }
    }

    /**
     * Wrapper stream that calculates checksums while reading.
     */
    private static class ChecksumInputStream extends FilterInputStream {

        private final MessageDigest md5;
        private final MessageDigest sha512;

        public ChecksumInputStream(InputStream in) throws IOException {
            super(in);
            try {
                this.md5 = MessageDigest.getInstance("MD5");
                this.sha512 = MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Checksum algorithms not available", e);
            }
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                md5.update((byte) b);
                sha512.update((byte) b);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            if (bytesRead != -1) {
                md5.update(b, off, bytesRead);
                sha512.update(b, off, bytesRead);
            }
            return bytesRead;
        }

        public String getMd5Checksum() {
            return bytesToHex(md5.digest());
        }

        public String getSha512Checksum() {
            return bytesToHex(sha512.digest());
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        }
    }
}