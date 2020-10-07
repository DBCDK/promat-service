Promat service
==============

### Configuration

**Environment variables**

* PROMAT_DB_URL database URL (USER:PASSWORD@HOST:PORT/DBNAME) of the underlying promat database.

### API

The service exposes a RESTful [API](https://raw.githubusercontent.com/DBCDK/promat-service/master/docs/openapi.yaml).

### Development

**Requirements**

To build this project JDK 11 and Apache Maven is required.

To start a local instance, docker is required.

**Scripts**
* clean - clears build artifacts
* build - builds artifacts and runs unit and integration tests
* validate - analyzes source code and javadoc
* start - starts localhost instance
* stop - stops localhost instance

```bash
./clean && ./build && ./validate && PROMAT_DB_URL="..." ./start
```

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
