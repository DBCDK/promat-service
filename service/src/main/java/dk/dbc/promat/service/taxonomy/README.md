# TAXONOMY SERVICE
Service for fetching all "subjects" records (agencyId 190004) records, and then to build a "taxonomy tree" used by
the frontend to enable the reviewer to select words and sentences that describe the material.

Subject records in 190004 records are structured this way (from praxis):
https://praxis.dbc.dk/andre-formater/px-aut0012.html/#pxx09kfe


## Fetching records
For existing old-world-setup (dm2) the approach is to use a complete dump of all 190004 records, using

```shell 
curl -H 'Content-type: application/json' -d '{"agencies":[190004],"outputFormat":"JSON"}' http://rawrepo-record-service.cisterne.svc.cloud.dbc.dk/api/v1/dump
```
As this is not that snappy, this takes place once every hour.

### Future dm3
For future dm3 setup the kafka subject used for prokat-search will be the basis for kafka-js worker to create a new
kafka subject, containing ONLY metakompas taxonomy relevant records. The initial first build will be slow, but thereafter
only incremental .

## Taxonomy tree
In current Metakompas solution the reviewer navigates menu structure like this:
```shell
{
  "ramme": {
    "handlingens tid udtrykt i ord": [],
    "handlingens tid udtrykt i tal": [],
    "geografisk sted": [],
    "fiktivt sted": [],
    "miljø": [],
    "genre": [],
    "univers": []
  },
  "handling": {
    "handler om": [],
    "navngivet hovedperson": [],
    "hovedperson(er) - beskrivelse": {
      "om hovedpersonen": [],
      "hovedpersonens karaktertræk": [],
      "hovedpersonens konflikt": []
    }
  },
  "fortælleteknik": {
    "skrivestil og struktur": [],
    "fortællerstemme": [],
    "tempo": []
  },
  "stemning": {
    "positiv": [],
    "humoristisk": [],
    "romantisk": [],
    "erotisk": [],
    "dramatisk": [],
    "trist": [],
    "uhyggelig": [],
    "fantasifuld": [],
    "tankevækkende": []
  }
}

```

Since this way of structuring are current hardcoded into 190004 records (praxis link above), we will stick to this approach for now.