| Endpunkt | Beschreibung | Methoden |
|---|---|---|
| /projects | Liste der Projekte u. Metadaten dazu (Titel, Beschreibung, URI, Kürzel) | GET |
| /projects/proj_abbr | Einzelnes Projekt u. Metadaten zum Projekt (Kürzel, Titel, Beschreibung, Creator, Fördergeber, Laufzeit (von, bis)) | GET, POST, PATCH, DELETE |
| /projects/proj_abbr/objects | Liste aller Objekte, die zu einem Projekt gehören mit Metadaten zum digitalen Objekt | GET |
| /projects/proj_abbr/objects/object_id | Einzelnes Objekt u. Metadaten | GET, PUT, DELETE |
| /search | Findet Objekte, die nach Metadaten in der DB gefiltert werden | GET, (POST) |
| /projects/proj_abbr/objects/object_id/datastreams? | Listet Metadaten zu allen Datenströmen, die zu einem Objekt gehören. Filtern via Parameter. | GET |
| /projects/proj_abbr/objects/object_id/datastream? | Metadaten des Hauptdatenstrom des Objekts (ohne Parameter) | GET |
| /projects/proj_abbr/objects/object_id/datastreams/DS_ID | Metadaten Datenstrom | GET |
| /projects/proj_abbr/objects/object_id/datastreams/DS_ID/content | "Inhalt" des Datenstroms = z.b. Bild. | GET |
| /userinfo | Nutzerinfos, Projektzuordnungen, Rolle | GET |
| (/collections) | Sammlung von Objekten | GET, PUT, DELETE, PATCH |