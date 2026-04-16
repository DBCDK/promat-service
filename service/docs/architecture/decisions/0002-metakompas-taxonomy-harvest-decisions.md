# 2. Metakompas taxonomy harvest decision
Date: 2026-04-15

## Status
Proposed

## Context
Metakompas taxonomy data must be made available for the promat frontend in order for the reviewer to select any number of 
appropriate keywords, to describe the material in question associated with the review case.

In the past, the former Metakompas suite (most notably the nextjs frontend app) has been used to build and serve 
taxonomy data.
As the "canonical truth" for subject records is stored in RawRepo, Subject records are harvested from here, using the "dump"
endpoint in record-service.

But the aim is to make the harvesting simpler.  

## Decision
1. In the future dm3 world, we will base the solution on kafka-js workers. A metakompas Subjects oriented topic will be created.
The topic will hold a filtered (valid metakompas records only!) transformation of the "prokat-search-all" topic.
As this is done using  a JavaScript-based kafka worker, we might as well make the outcome of the filter nice and directly
aimed at fitting into promat's taxonomy builder. The solution must be in place at the end of 2026 at the latest.

Subjects live in RawRepo with agency 190004.
But the 190004 records in dm2 lack the DBC-specific fields (such as X09). 
To make a 190004 topic we would have to be able to have access to the 190004 record as well as its
191919 counterpart, in a recordcollection. At present there is no such "base" topic. So we would need a
new java based rawrepo to kafka, for this purpose only. So for now we will stick to the v2 dump endpoint solution. 
2. Promat must be able to exist in both dm2 and dm3 environs, and use the dump endpoint in the dm2 
recordservice (as in the old metakompas nextjs solution), when running in "dm2" mode. The type of harvesting must be 
configurable by env vars.  To use the dm2 harvester, the env var RECORD_SERVICE must be present. Otherwise, dm3 
harvesting from kafka is assumed (future).

## Consequences
* As long as promat still lives in dm2, the old recordservice dump endpoint cannot be retired. 
* Additional new business logic concerning subjects will have to be implemented in the kafka-js worker (javascript), 
and (in java) in promat.
* In dm2 environments we will regularly (once every approx. 15 minutes) have to traverse the full body of the 190004 dump. 
Sifting the approx. 10.000 subjects out of a total of 100.000 is noticeably slow (25 s). And it will affect the startup time 
of promat. 
But once started, the harvesting will take place in a separate thread, and thus should not affect performance in 
everyday use.
* Integration tests will be somewhat more involved. Kafka-js workers tests will not be able to be executed when the 
docker container is started in dm2 mode. And vice versa with the dm2 tests, when the docker container is started in dm3 mode.
* When the transition to dm3 is complete, we will have to remove  the dm2-specific code from promat.

