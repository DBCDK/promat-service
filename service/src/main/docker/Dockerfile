FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:latest

ADD target/*.war target/docker/promat-service.json deployments/

ENV OPENAPI_UI_CONTEXTROOT="http:" \
    MP_OPENAPI_SERVERS="http:/"