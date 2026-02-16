package org.ddh.gamsapi.domain.Datastream.DatastreamContent;

import org.ddh.gamsapi.domain.Datastream.DatastreamId;

/**
 * Result of writing datastream content to the filesystem.
 * Contains the computed checksums from the server-side write operation.
 */
public record WriteResult(
    DatastreamId datastreamId,
    String md5Checksum,
    String sha512Checksum
) {}
