# API ENDPOINTS

| Endpunkt `/api/v1` | Beschreibung | Methoden |
|---|---|---|
| `/projects` | Liste der Projekte u. Metadaten dazu (Titel, Beschreibung, URI, Kürzel) | `GET` |
| `/projects/projectAbbr` | Einzelnes Projekt u. Metadaten zum Projekt (Kürzel, Titel, Beschreibung, Creator, Fördergeber, Laufzeit (von, bis)) | `GET`, `POST`, `PATCH`, `DELETE` |
| `/projects/projectAbbr/objects` | Liste aller Objekte, die zu einem Projekt gehören mit Metadaten zum digitalen Objekt | `GET` |
| `/projects/projectAbbr/objects/id` | Einzelnes Objekt u. Metadaten | `GET`, `PUT`, `DELETE` |
| `/projects/projectAbbr/objects/id/datastreams?` | Listet Metadaten zu allen Datenströmen, die zu einem Objekt gehören. Filtern via Parameter. | `GET` |
| `/projects/projectAbbr/objects/id/datastream?` | Metadaten des Hauptdatenstrom des Objekts (ohne Parameter) | `GET` |
| `/projects/projectAbbr/objects/id/datastreams/dsid` | Metadaten Datenstrom | `GET` |
| `/projects/projectAbbr/objects/id/datastreams/dsid/content` | "Inhalt" des Datenstroms = z.b. Bild. | `GET` |
| `(/collections)` | Sammlung von Objekten | `GET`, `PUT`, `DELETE`, `PATCH` |
| `/search` | Findet Objekte, die nach Metadaten in der DB gefiltert werden | `GET`, `(POST)` |
| `/userinfo` | Nutzerinfos, Projektzuordnungen, Rolle | `GET` |