## Build with test

```
mvn clean verify
```

Integration tests are not run againts a real Artifactory instance, but against a WireMock server.

This could be improved in a future with testcontainers.

## Interactive tests

### Start artifactory instance

```
docker-compose up -d artifactory
```

### Start the plugin

```
mvn hpi:run
```
