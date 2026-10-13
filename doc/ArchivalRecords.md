
# ArchivalRecords: The archiving process in GAMS5
 
## How it generally works

The gams-api provides ArchivalRecords that represent the system's view on the archiving state of individual digital objects.
Each ArchivalRecord represents a separate version. So each digital object might have no or multiple archival records assigned.

gams-api does not provide a long term storage solution itself, but provides the means to track the archiving state of individual digital objects
via the archival records api. E.g. gams-api tracks a pid, the location of the archived-material (via externalId), the publication date etc. 

Archival records are stateful: They are meant to hold data about the archiving process being done outside the system in steps.
E.g. the RESERVED state requires you to define a pid, DRAFT requires an external-id (pointer to the archived resource) and PUBLISHED
requires a publication date (=publication of the archived resource)

An archival record might use a completely arbitrary pid system (e.g. handle), only if GAMS5 specific custom handles are
used GAMS5 will sign in handles to our managed handle-server.

## What makes the archiving process inherently complex

* Distributed system at it's core:
    - Handle server, gams-api, invenioRDM and gams-archiver (cli-tool)
    - Asynchronous, error-prone communication that is hard to keep consistent. (transaction-safety) So there is
      an inherent need for manual / automatic correction in error scenarios.
* Required concordance-bookkeeping between: DigitalObject-Handle-InvenioId:
    - Handles must be completely random (e.g. cannot be computed from the object-id)
    - IDs of repository resources are completely random (InvenioRDM) (cannot be set via e.g. handle)
* Tight inherent coupling:
    - InvenioRDM needs the handle, handle-server needs the invenio-id, gams-api needs both ids, etc.
* Stateful archiving process:
    - State machine managed on server side and not locally
    - RESERVE/DRAFT/PUBLISH: To-be-archived resources may be revised or rejected manually. There might be long time-spans between drafting and publishing an archived resource.  
