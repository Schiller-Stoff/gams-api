package org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit;

public class BagExportException extends RuntimeException {
    public BagExportException(String message) {
        super(message);
    }

    public BagExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
