# Play Framework and Pekko Upgrade

## Summary

This repository has been upgraded from Play Framework 2.7.2 with Akka 2.5.22 to Play Framework 3.0.5 with Apache Pekko 1.0.2.

## Version Changes

### Before
- Play Framework: 2.7.2
- Akka: 2.5.22
- Scala: 2.11.12
- Java: 8 (target), 17 (runtime)
- Jackson: 2.9.10.4
- SLF4J: 1.6.1
- Logback: 1.0.7
- Netty: 4.1.44

### After
- Play Framework: 3.0.5
- Apache Pekko: 1.0.2
- Scala: 2.13.12
- Java: 11 (target), 17 (runtime)
- Jackson: 2.14.3
- SLF4J: 2.0.9
- Logback: 1.4.14
- Netty: 4.1.93

## Reason for Upgrade

1. License Compliance: Akka changed from Apache 2.0 to Business Source License 1.1 requiring commercial licenses. Apache Pekko maintains Apache 2.0 license.
2. Security: Play 2.7.2 and Akka 2.5.22 no longer receive security updates.
3. Modernization: Access to latest features and performance improvements.

## Changes Made

### Dependencies (4 POM files)
- Parent POM: Updated version properties
- all-actors POM: Replaced Akka with Pekko dependencies
- sb-es-utils POM: Updated Akka to Pekko
- service POM: Updated Play and Pekko dependencies

### Source Code (15 Java files)
- Replaced 24 Akka import statements with Pekko equivalents
- Package changes: akka.* to org.apache.pekko.*
- Files updated: BaseActor, CertificationActor, controllers, utilities, tests

### Configuration (1 file)
- application.conf: Changed akka namespace to pekko
- Updated all class references and logger configurations

### Play 3.0 API Updates
- ActorStartModule: Changed from AkkaGuiceSupport to PekkoGuiceSupport
- RequestHandler: Updated FutureConverters for Scala 2.13
- OnRequestHandler: Removed deprecated Http.Context, using Http.Request
- Fixed artifact names for Play 3.0 compatibility

## Build

Build all modules:
```
mvn clean install -DskipTests
```

Create distribution package:
```
cd service
mvn play2:dist
```

## Build Verification

All modules compile successfully:
- certification-service
- sb-utils
- Cassandra Utils
- sb-es-utils
- all-actors
- play-service

Dependency tree verified: No Akka dependencies, only Scala 2.13.12 present.

## Testing

Run tests:
```
mvn test
```

Note: Some PowerMock tests may require Java 17 compatibility adjustments (unrelated to this migration).

## Migration Impact

- Business Logic: No changes to business logic or functionality
- API Compatibility: Maintained, as Pekko is API-compatible with Akka 2.6
- Code Changes: Primarily package name updates from akka to pekko
- License: Now compliant with Apache 2.0 throughout the stack

## Known Issues

If you encounter NoClassDefFoundError for scala.collection.GenMap, verify dependency tree to ensure no Scala 2.12 artifacts are present:
```
mvn dependency:tree
```

Add exclusions for any scala-library or scala-reflect with version 2.12 if needed.
