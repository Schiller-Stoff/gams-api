package org.ddh.gamsapi.domain.ArchivalRecord.utils;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleAlreadyExistsException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleNotRegisteredException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleServerException;

import java.net.URI;
import java.util.Optional;

/**
 * Port to the handle server (Handle.Net v9 HTTP JSON REST API, manual chapter 14).
 * <p>
 * All methods accept PIDs in the form stored on {@code ArchivalRecord}, i.e. {@code hdl:11471/518.10.1.4714}.
 * Stripping the {@code hdl:} scheme for the wire protocol is an implementation detail.
 * <p>
 * Remote calls are blocking HTTP round trips. Do not call them while holding a database
 * transaction open longer than necessary (see two-phase pattern used for ingest).
 */
public interface IHandleClient {

  /**
   * Generates a new, syntactically valid PID under the configured prefix.
   * Purely local - does NOT contact the handle server and does NOT guarantee that the
   * handle is still free there. Use {@link #register(String, URI)} to actually claim it.
   *
   * @return e.g. {@code hdl:11471/518.10.1.4714}
   */
  String generate();

  /**
   * Creates the handle on the handle server with an HS_ADMIN value and a URL value pointing to {@code target}.
   * <p>
   * Never overwrites: if the handle already exists and already points to the same target the call is
   * treated as a successful (idempotent) retry. If it points elsewhere, the call fails.
   *
   * @throws HandleAlreadyExistsException if the handle exists with a different target
   * @throws HandleServerException        on connectivity, authentication or unexpected server errors
   */
  void register(String pid, URI target);

  /**
   * Replaces the URL value of an existing handle (e.g. re-targeting on publication).
   *
   * @throws HandleNotRegisteredException if the handle does not exist on the handle server
   * @throws HandleServerException        on connectivity, authentication or unexpected server errors
   */
  void retarget(String pid, URI target);

  /**
   * Resolves the URL value of a handle (unauthenticated, public values only).
   *
   * @return the target, or empty if the handle does not exist or has no URL value
   * @throws HandleServerException on connectivity or unexpected server errors
   */
  Optional<URI> resolveTarget(String pid);

  /**
   * Checks whether the handle exists on the handle server (unauthenticated).
   *
   * @throws HandleServerException on connectivity or unexpected server errors
   */
  boolean exists(String pid);

  /**
   * Deletes the handle. Idempotent: deleting a non-existing handle is a no-op.
   * <p>
   * Only meant for handles that were never public (e.g. abandoned reservations).
   * Deleting a published PID breaks the persistence promise - prefer re-targeting to a tombstone page.
   *
   * @throws HandleServerException on connectivity, authentication or unexpected server errors
   */
  void delete(String pid);

}