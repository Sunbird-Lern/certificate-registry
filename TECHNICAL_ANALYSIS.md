# Technical Analysis: File-by-File Breakdown

## Overview
This document provides a detailed, file-by-file analysis of Akka usage in the certificate-registry application and specific migration requirements for each file.

---

## Module Structure

```
certificate-registry/
├── pom.xml (parent)
├── sb-utils/
│   └── pom.xml
├── cassandra-utils/
│   └── pom.xml
├── sb-es-utils/
│   ├── pom.xml
│   └── src/main/java/org/sunbird/common/
│       ├── ElasticSearchHelper.java (Akka usage)
│       └── ElasticSearchRestHighImpl.java (Akka usage)
├── all-actors/
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/sunbird/
│       │   ├── BaseActor.java (Critical - Akka usage)
│       │   ├── actor/CertificationActor.java (Critical - Akka usage)
│       │   ├── service/ICertService.java (Akka usage)
│       │   ├── serviceimpl/CertsServiceImpl.java (Akka usage)
│       │   └── utilities/CertificateUtil.java (Akka usage)
│       └── test/java/org/sunbird/actor/
│           └── CertificationActorTest.java (Akka usage)
└── service/
    ├── pom.xml
    └── app/
        ├── controllers/
        │   ├── BaseController.java (Akka usage)
        │   ├── CertificateController.java (Akka usage)
        │   └── RequestHandler.java (Critical - Akka usage)
        ├── utils/module/
        │   ├── ActorStartModule.java (Critical - Akka usage)
        │   └── SignalHandler.java (Critical - Akka usage)
        └── test/controllers/
            └── DummyActor.java (Akka usage)
```

---

## Critical Files Analysis

### 1. BaseActor.java (all-actors module)

**Location**: `all-actors/src/main/java/org/sunbird/BaseActor.java`

**Current Akka Usage**:
```java
import akka.actor.UntypedAbstractActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;

public abstract class BaseActor extends UntypedAbstractActor {
    protected DiagnosticLoggingAdapter logger = Logging.getLogger(this);
    protected Localizer localizer = Localizer.getInstance();
    
    @Override
    public void onReceive(Object message) throws Throwable {
        // Actor message handling
    }
    
    protected abstract void onReceive(Request request) throws Throwable;
}
```

**Migration Requirements**:
- **Complexity**: HIGH
- **Impact**: CRITICAL (base class for all actors)
- **Changes Required**:
  1. Replace `akka.actor.UntypedAbstractActor` → `org.apache.pekko.actor.UntypedAbstractActor`
  2. Replace `akka.event.DiagnosticLoggingAdapter` → `org.apache.pekko.event.DiagnosticLoggingAdapter`
  3. Replace `akka.event.Logging` → `org.apache.pekko.event.Logging`
  4. No logic changes required - API is identical

**Testing Priority**: CRITICAL
- Test actor lifecycle (creation, start, stop)
- Test message handling
- Test logging functionality
- Test error handling

---

### 2. CertificationActor.java (all-actors module)

**Location**: `all-actors/src/main/java/org/sunbird/actor/CertificationActor.java`

**Current Akka Usage**:
```java
import akka.actor.ActorRef;

public class CertificationActor extends BaseActor {
    @Inject
    @Named("certificate_background_actor")
    private ActorRef certBackgroundActorRef;
    
    @Override
    public void onReceive(Request request) throws BaseException {
        String operation = request.getOperation();
        switch (operation) {
            case "add":
                sender().tell(response, self());
                break;
            // ... other operations
        }
    }
}
```

**Migration Requirements**:
- **Complexity**: MEDIUM
- **Impact**: CRITICAL (main business logic actor)
- **Changes Required**:
  1. Replace `akka.actor.ActorRef` → `org.apache.pekko.actor.ActorRef`
  2. Dependency injection remains same
  3. `sender()` and `self()` methods work identically
  4. No logic changes required

**Testing Priority**: CRITICAL
- Test all operation handlers (add, validate, download, generate, verify, read, search)
- Test actor-to-actor communication
- Test response handling
- Test error scenarios

---

### 3. ActorStartModule.java (service module)

**Location**: `service/app/utils/module/ActorStartModule.java`

**Current Implementation**:
```java
import akka.routing.FromConfig;
import akka.routing.RouterConfig;
import play.libs.akka.AkkaGuiceSupport;

public class ActorStartModule extends AbstractModule implements AkkaGuiceSupport {
    @Override
    protected void configure() {
        final RouterConfig config = new FromConfig();
        for (ACTOR_NAMES actor : ACTOR_NAMES.values()) {
            bindActor(
                actor.getActorClass(),
                actor.getActorName(),
                (props) -> props.withRouter(config)
            );
        }
    }
}
```

**Migration Requirements**:
- **Complexity**: HIGH
- **Impact**: CRITICAL (DI integration)
- **Changes Required**:

#### For Play 2.9.x:
```java
import org.apache.pekko.routing.FromConfig;
import org.apache.pekko.routing.RouterConfig;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ActorStartModule extends AbstractModule {
    @Override
    protected void configure() {
        // Manual actor binding
    }
    
    @Provides
    public ActorSystem provideActorSystem() {
        return ActorSystem.create("application");
    }
    
    @Provides
    @Named("certification_actor")
    public ActorRef provideCertificationActor(ActorSystem system) {
        RouterConfig config = new FromConfig();
        Props props = Props.create(CertificationActor.class)
                          .withRouter(config);
        return system.actorOf(props, "certification_actor");
    }
}
```

#### For Play 3.0.x:
```java
import org.apache.pekko.routing.FromConfig;
import org.apache.pekko.routing.RouterConfig;
import play.libs.pekko.PekkoGuiceSupport;

public class ActorStartModule extends AbstractModule implements PekkoGuiceSupport {
    @Override
    protected void configure() {
        final RouterConfig config = new FromConfig();
        for (ACTOR_NAMES actor : ACTOR_NAMES.values()) {
            bindActor(
                actor.getActorClass(),
                actor.getActorName(),
                (props) -> props.withRouter(config)
            );
        }
    }
}
```

**Testing Priority**: CRITICAL
- Test actor creation through DI
- Test router configuration
- Test named actor injection
- Test actor lifecycle management

---

### 4. SignalHandler.java (service module)

**Location**: `service/app/utils/module/SignalHandler.java`

**Current Implementation**:
```java
import akka.actor.ActorSystem;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

@Singleton
public class SignalHandler {
    @Inject
    public SignalHandler(ActorSystem actorSystem, Provider<Application> applicationProvider) {
        STOP_DELAY = Duration.create(delay, TimeUnit.SECONDS);
        Signal.handle(
            new Signal("TERM"),
            signal -> {
                actorSystem.scheduler()
                    .scheduleOnce(
                        STOP_DELAY,
                        () -> Play.stop(applicationProvider.get()),
                        actorSystem.dispatcher()
                    );
            }
        );
    }
}
```

**Migration Requirements**:
- **Complexity**: MEDIUM
- **Impact**: HIGH (graceful shutdown)
- **Changes Required**:
  1. Replace `akka.actor.ActorSystem` → `org.apache.pekko.actor.ActorSystem`
  2. Scala Duration classes remain same (part of Scala stdlib)
  3. Scheduler API is identical in Pekko
  4. No logic changes required

**Testing Priority**: HIGH
- Test SIGTERM signal handling
- Test delayed shutdown
- Test graceful request completion
- Test ActorSystem shutdown

---

### 5. RequestHandler.java (service module)

**Location**: `service/app/controllers/RequestHandler.java`

**Current Implementation**:
```java
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

public class RequestHandler extends BaseController {
    public CompletionStage<Result> handleRequest(Request request, Object actorRef, 
                                                  String operation, Http.Request req) {
        Timeout t = new Timeout(Long.valueOf(request.getTimeout()), TimeUnit.SECONDS);
        Future<Object> future;
        
        if (actorRef instanceof ActorRef) {
            future = Patterns.ask((ActorRef) actorRef, request, t);
        } else {
            future = Patterns.ask((ActorSelection) actorRef, request, t);
        }
        
        return FutureConverters.toJava(future).thenApplyAsync(fn);
    }
}
```

**Migration Requirements**:
- **Complexity**: MEDIUM
- **Impact**: CRITICAL (all HTTP requests use this)
- **Changes Required**:
  1. Replace `akka.actor.ActorRef` → `org.apache.pekko.actor.ActorRef`
  2. Replace `akka.actor.ActorSelection` → `org.apache.pekko.actor.ActorSelection`
  3. Replace `akka.pattern.Patterns` → `org.apache.pekko.pattern.Patterns`
  4. Replace `akka.util.Timeout` → `org.apache.pekko.util.Timeout`
  5. `FutureConverters` remains same (Scala stdlib)
  6. No logic changes required

**Testing Priority**: CRITICAL
- Test ask pattern functionality
- Test timeout handling
- Test future conversion
- Test both ActorRef and ActorSelection paths
- Test error handling
- Test concurrent requests

---

### 6. CertificateController.java (service module)

**Location**: `service/app/controllers/CertificateController.java`

**Current Akka Usage**:
```java
import akka.actor.ActorRef;

public class CertificateController extends RequestHandler {
    @Inject
    @Named("certification_actor")
    private ActorRef certificationActor;
    
    public CompletionStage<Result> add(Http.Request request) throws Exception {
        return handleRequest(getRequest(request), certificationActor, "add", request);
    }
    // ... other endpoints
}
```

**Migration Requirements**:
- **Complexity**: LOW
- **Impact**: MEDIUM
- **Changes Required**:
  1. Replace `akka.actor.ActorRef` → `org.apache.pekko.actor.ActorRef`
  2. Named injection remains same
  3. No logic changes required

**Testing Priority**: HIGH
- Test all API endpoints
- Test actor communication
- Test error responses
- Test request/response mapping

---

### 7. ElasticSearchHelper.java (sb-es-utils module)

**Location**: `sb-es-utils/src/main/java/org/sunbird/common/ElasticSearchHelper.java`

**Current Akka Usage**:
```java
import akka.util.Timeout;

public class ElasticSearchHelper {
    private static Timeout timeout = Timeout.apply(Duration.apply(10, TimeUnit.SECONDS));
}
```

**Migration Requirements**:
- **Complexity**: LOW
- **Impact**: LOW
- **Changes Required**:
  1. Replace `akka.util.Timeout` → `org.apache.pekko.util.Timeout`
  2. API is identical
  3. No logic changes required

**Testing Priority**: MEDIUM
- Test timeout functionality
- Test ES operations with timeout

---

### 8. ElasticSearchRestHighImpl.java (sb-es-utils module)

**Location**: `sb-es-utils/src/main/java/org/sunbird/common/ElasticSearchRestHighImpl.java`

**Current Akka Usage**:
```java
import akka.dispatch.Futures;

public class ElasticSearchRestHighImpl implements ElasticSearchService {
    // Uses Futures for async operations
}
```

**Migration Requirements**:
- **Complexity**: MEDIUM
- **Impact**: MEDIUM
- **Changes Required**:
  1. Replace `akka.dispatch.Futures` → `org.apache.pekko.dispatch.Futures`
  2. API is identical
  3. No logic changes required

**Testing Priority**: HIGH
- Test async ES operations
- Test future handling
- Test error scenarios

---

### 9. CertificateUtil.java (all-actors module)

**Location**: `all-actors/src/main/java/org/sunbird/utilities/CertificateUtil.java`

**Current Akka Usage**:
```java
import akka.actor.ActorRef;

public class CertificateUtil {
    public static Response insertRecord(Map<String,Object> certAddReqMap, 
                                       ActorRef certBackgroundActorRef) {
        Request req = new Request();
        req.setOperation(ActorOperations.ADD_CERT_ES.getOperation());
        certBackgroundActorRef.tell(req, ActorRef.noSender());
        return response;
    }
}
```

**Migration Requirements**:
- **Complexity**: LOW
- **Impact**: MEDIUM
- **Changes Required**:
  1. Replace `akka.actor.ActorRef` → `org.apache.pekko.actor.ActorRef`
  2. `tell()` and `noSender()` methods identical
  3. No logic changes required

**Testing Priority**: MEDIUM
- Test fire-and-forget messaging
- Test background actor communication

---

### 10. Test Files

#### CertificationActorTest.java
**Location**: `all-actors/src/test/java/org/sunbird/actor/CertificationActorTest.java`

**Current Akka Usage**:
```java
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;

@RunWith(PowerMockRunner.class)
public class CertificationActorTest {
    private static ActorSystem system;
    
    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }
    
    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
    }
}
```

**Migration Requirements**:
- **Complexity**: LOW
- **Impact**: LOW
- **Changes Required**:
  1. Replace all `akka.*` imports → `org.apache.pekko.*`
  2. TestKit API is identical
  3. No logic changes required

#### DummyActor.java
**Location**: `service/test/controllers/DummyActor.java`

**Migration Requirements**:
- **Complexity**: LOW
- **Impact**: LOW
- Similar to BaseActor changes

---

## Configuration Files Analysis

### application.conf

**Location**: `service/conf/application.conf`

**Current Configuration** (Lines 18-107):
```hocon
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  loglevel = "INFO"
  stdout-loglevel = "DEBUG"
  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
  
  actor {
    provider = "akka.actor.LocalActorRefProvider"
    serializers {
      java = "akka.serialization.JavaSerializer"
    }
    serialization-bindings {
      "org.sunbird.request.Request" = java
      "org.sunbird.response.Response" = java
    }
    
    default-dispatcher {
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 32.0
        parallelism-max = 64
        task-peeking-mode = "FIFO"
      }
    }
    
    router-dispatcher {
      type = "Dispatcher"
      executor = "fork-join-executor"
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 32.0
        parallelism-max = 64
      }
      throughput = 1
    }
    
    cert-dispatcher {
      type = "Dispatcher"
      executor = "fork-join-executor"
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 32.0
        parallelism-max = 64
      }
      throughput = 1
    }
    
    deployment {
      /certification_actor {
        router = smallest-mailbox-pool
        nr-of-instances = 5
        dispatcher = cert-dispatcher
      }
      /certificate_background_actor {
        router = smallest-mailbox-pool
        nr-of-instances = 5
        dispatcher = cert-dispatcher
      }
    }
  }
  
  remote {
    maximum-payload-bytes = 30000000 bytes
    netty.tcp {
      port = 8088
      message-frame-size = 30000000b
      send-buffer-size = 30000000b
      receive-buffer-size = 30000000b
      maximum-frame-size = 30000000b
    }
  }
}
```

**Required Changes**:
```hocon
pekko {
  loggers = ["org.apache.pekko.event.slf4j.Slf4jLogger"]
  loglevel = "INFO"
  stdout-loglevel = "DEBUG"
  logging-filter = "org.apache.pekko.event.slf4j.Slf4jLoggingFilter"
  
  actor {
    provider = "org.apache.pekko.actor.LocalActorRefProvider"
    serializers {
      java = "org.apache.pekko.serialization.JavaSerializer"
    }
    serialization-bindings {
      "org.sunbird.request.Request" = java
      "org.sunbird.response.Response" = java
    }
    
    # All dispatcher and deployment configs remain structurally same
    # Just change namespace from 'akka' to 'pekko'
  }
  
  remote {
    # Configuration structure remains same
    # Just change namespace from 'akka' to 'pekko'
  }
}
```

**Migration Complexity**: LOW
- Simple namespace replacement: `akka.` → `org.apache.pekko.`
- All configuration structure remains identical
- Can use automated sed/awk scripts

---

## POM Files Analysis

### Parent POM (pom.xml)

**Current Dependencies**:
```xml
<properties>
    <akka.x.version>2.5.22</akka.x.version>
    <play2.version>2.7.2</play2.version>
    <scala.version>2.11.12</scala.version>
    <scala.major.version>2.11</scala.major.version>
</properties>
```

**Required Changes**:
```xml
<properties>
    <pekko.version>1.0.3</pekko.version>
    <play2.version>2.9.5</play2.version>
    <scala.version>2.13.12</scala.version>
    <scala.major.version>2.13</scala.major.version>
</properties>
```

### all-actors/pom.xml

**Current Dependencies**:
```xml
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-actor_${scala.major.version}</artifactId>
    <version>${akka.x.version}</version>
</dependency>
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-testkit_${scala.major.version}</artifactId>
    <version>2.5.22</version>
    <scope>test</scope>
</dependency>
```

**Required Changes**:
```xml
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-actor_${scala.major.version}</artifactId>
    <version>${pekko.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-testkit_${scala.major.version}</artifactId>
    <version>${pekko.version}</version>
    <scope>test</scope>
</dependency>
```

### service/pom.xml

**Current Akka Dependencies**:
```xml
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-remote_${scala.major.version}</artifactId>
    <version>${akka.x.version}</version>
</dependency>
```

Plus transitive dependencies from Play:
- akka-actor
- akka-stream
- akka-slf4j
- akka-http-core
- akka-parsing

**Required Changes**:
```xml
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-remote_${scala.major.version}</artifactId>
    <version>${pekko.version}</version>
</dependency>
```

For Play 2.9.x, may need explicit Pekko dependencies:
```xml
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-actor_${scala.major.version}</artifactId>
    <version>${pekko.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-stream_${scala.major.version}</artifactId>
    <version>${pekko.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-slf4j_${scala.major.version}</artifactId>
    <version>${pekko.version}</version>
</dependency>
```

For Play 3.0.x, Pekko is the default and comes transitively.

---

## Migration Script

### Automated Import Replacement

```bash
#!/bin/bash
# migrate-akka-to-pekko.sh

echo "Starting Akka to Pekko migration..."

# Backup first
echo "Creating backup..."
tar -czf pre-pekko-migration-backup.tar.gz .

# Replace Java imports
echo "Replacing Java imports..."
find . -name "*.java" -type f -exec sed -i 's/import akka\./import org.apache.pekko./g' {} +

# Replace Scala imports (if any)
echo "Replacing Scala imports..."
find . -name "*.scala" -type f -exec sed -i 's/import akka\./import org.apache.pekko./g' {} +

# Replace configuration
echo "Updating configuration files..."
find . -name "*.conf" -type f -exec sed -i 's/^akka\./pekko./g' {} +
find . -name "*.conf" -type f -exec sed -i 's/"akka\./"org.apache.pekko./g' {} +
find . -name "*.conf" -type f -exec sed -i 's/\[akka\./[org.apache.pekko./g' {} +

echo "Migration complete. Please review changes and test thoroughly."
```

### Manual Verification Steps

After running automated script:

1. **Search for remaining 'akka' references**:
```bash
grep -r "akka" --include="*.java" --include="*.conf" . | grep -v "pekko"
```

2. **Verify package structure**:
```bash
# Should return empty
grep -r "import akka\." --include="*.java" .
```

3. **Check POM files** (manual update required):
```bash
grep -r "com.typesafe.akka" --include="*.xml" .
```

---

## Testing Strategy

### Unit Testing

**Priority 1: Actor Tests**
- [ ] BaseActor creation and lifecycle
- [ ] Message handling in BaseActor
- [ ] CertificationActor all operations
- [ ] Actor supervision and error handling
- [ ] Logging functionality

**Priority 2: Integration Tests**
- [ ] ActorSystem initialization
- [ ] Dependency injection of actors
- [ ] Router configuration
- [ ] Dispatcher assignment
- [ ] Remote actor communication

**Priority 3: Controller Tests**
- [ ] RequestHandler ask pattern
- [ ] Timeout handling
- [ ] Future conversion
- [ ] Error responses
- [ ] All API endpoints

### Performance Testing

**Metrics to Verify**:
- [ ] Message throughput (should be equal or better)
- [ ] Latency (95th percentile should be comparable)
- [ ] Memory usage (should be similar)
- [ ] CPU usage (should be similar)
- [ ] Actor creation time
- [ ] Message processing time

**Load Testing Scenarios**:
- [ ] Concurrent certificate additions
- [ ] Parallel search operations
- [ ] Sustained load over time
- [ ] Burst traffic handling
- [ ] Actor pool saturation

### Compatibility Testing

**Binary Compatibility**:
- [ ] Serialization/deserialization of messages
- [ ] Remote actor protocol (if used)
- [ ] Persistent actor recovery (if used)
- [ ] Cluster communication (if used)

**API Compatibility**:
- [ ] All HTTP endpoints functional
- [ ] Request/response formats unchanged
- [ ] Error codes consistent
- [ ] Logging format preserved

---

## Rollback Plan

### Pre-Migration Checklist
- [ ] Full database backup
- [ ] Git branch created for migration
- [ ] Current production version tagged
- [ ] Test environment available
- [ ] Monitoring baseline captured

### Migration Phases
1. Development → Test environment
2. Staging environment
3. Canary deployment (5% traffic)
4. Rolling deployment (50% traffic)
5. Full production deployment

### Rollback Triggers
- Critical bugs affecting functionality
- Performance degradation >20%
- Memory leaks detected
- Actor system instability
- Test failures in production

### Rollback Procedure
1. Revert to previous Docker image
2. Restart services with old configuration
3. Verify functionality
4. Analyze failure cause
5. Plan remediation

---

## Summary

### Complexity Rating by File

| File | Complexity | Impact | Priority |
|------|-----------|--------|----------|
| BaseActor.java | HIGH | CRITICAL | 1 |
| CertificationActor.java | MEDIUM | CRITICAL | 1 |
| ActorStartModule.java | HIGH | CRITICAL | 1 |
| RequestHandler.java | MEDIUM | CRITICAL | 1 |
| SignalHandler.java | MEDIUM | HIGH | 2 |
| CertificateController.java | LOW | MEDIUM | 2 |
| ElasticSearchHelper.java | LOW | LOW | 3 |
| ElasticSearchRestHighImpl.java | MEDIUM | MEDIUM | 3 |
| CertificateUtil.java | LOW | MEDIUM | 3 |
| Test files | LOW | LOW | 4 |

### Estimated Effort

| Phase | Effort (days) | Risk |
|-------|---------------|------|
| Code changes | 3-5 | Low |
| Configuration updates | 1-2 | Low |
| POM updates | 2-3 | Medium |
| Unit testing | 5-7 | Medium |
| Integration testing | 3-5 | Medium |
| Performance testing | 2-3 | High |
| Documentation | 2-3 | Low |
| **Total** | **18-28 days** | **Medium** |

### Success Criteria

✅ All automated tests passing  
✅ Performance metrics within 10% of baseline  
✅ Zero production incidents for 2 weeks  
✅ Successful gradual rollout  
✅ Team training completed  
✅ Documentation updated  

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-07  
**Status**: Analysis Complete
