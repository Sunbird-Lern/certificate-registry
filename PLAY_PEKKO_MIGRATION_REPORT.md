# Play Framework Upgrade & Akka to Pekko Migration Report

## Executive Summary

This report analyzes the certificate-registry application for upgrading Play Framework and migrating from Akka to Apache Pekko. The application currently uses **Play Framework 2.7.2** and **Akka 2.5.22**, both of which are outdated and require modernization.

---

## Current State Analysis

### 1. Current Versions
- **Play Framework**: 2.7.2 (Released: April 2019)
- **Akka**: 2.5.22 (Released: May 2019)
- **Scala**: 2.11.12
- **Java**: 11 (target), 17 (runtime)
- **Build Tool**: Maven with play2-maven-plugin 1.0.0-rc5

### 2. Akka Usage in Codebase

The application makes extensive use of Akka for actor-based concurrency. Analysis reveals **14 Java files** using Akka across multiple modules:

#### Core Actor Files:
1. **BaseActor.java** (`all-actors/src/main/java/org/sunbird/BaseActor.java`)
   - Extends `akka.actor.UntypedAbstractActor`
   - Base class for all actors in the application
   - Uses `akka.event.DiagnosticLoggingAdapter` and `akka.event.Logging`

2. **CertificationActor.java** (`all-actors/src/main/java/org/sunbird/actor/CertificationActor.java`)
   - Main business logic actor
   - Uses `akka.actor.ActorRef` for actor references
   - Handles certificate operations (add, validate, download, generate, verify, read, search)

3. **ActorStartModule.java** (`service/app/utils/module/ActorStartModule.java`)
   - Extends `play.libs.akka.AkkaGuiceSupport`
   - Uses `akka.routing.FromConfig` for router configuration
   - Integrates Akka with Play's dependency injection

4. **SignalHandler.java** (`service/app/utils/module/SignalHandler.java`)
   - Uses `akka.actor.ActorSystem`
   - Manages graceful shutdown with SIGTERM handling
   - Uses Akka scheduler for delayed shutdown

#### Controller and Service Files:
5. **RequestHandler.java** (`service/app/controllers/RequestHandler.java`)
   - Uses `akka.pattern.Patterns` for ask pattern
   - Uses `akka.util.Timeout` for timeout management
   - Uses `akka.actor.ActorRef` and `akka.actor.ActorSelection`
   - Converts Scala futures to Java CompletionStage

6. **BaseController.java** (`service/app/controllers/BaseController.java`)
   - Uses `akka.actor.ActorRef`

7. **CertificateController.java** (`service/app/controllers/CertificateController.java`)
   - Uses `akka.actor.ActorRef` for actor communication

8. **CertificateUtil.java** (`all-actors/src/main/java/org/sunbird/utilities/CertificateUtil.java`)
   - Uses `akka.actor.ActorRef` for background processing

#### Test Files:
9. **CertificationActorTest.java** - Uses Akka TestKit
10. **DummyActor.java** - Test actor extending `UntypedAbstractActor`

#### Utility Files:
11. **ElasticSearchHelper.java** - Uses `akka.util.Timeout`
12. **ElasticSearchRestHighImpl.java** - Uses `akka.dispatch.Futures`

### 3. Akka Configuration

The `application.conf` file contains extensive Akka configuration:

```hocon
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  loglevel = "INFO"
  
  actor {
    provider = "akka.actor.LocalActorRefProvider"
    serializers {
      java = "akka.serialization.JavaSerializer"
    }
    serialization-bindings {
      "org.sunbird.request.Request" = java
      "org.sunbird.response.Response" = java
    }
    
    # Dispatcher configurations
    default-dispatcher { ... }
    router-dispatcher { ... }
    cert-dispatcher { ... }
    
    # Actor deployment with routing
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

### 4. Play Framework Integration

The application uses several Play Framework features:
- **Dependency Injection**: Guice-based DI with `play-guice`
- **HTTP Server**: Both Netty and Akka HTTP server support
- **Routing**: Static routes generation
- **Akka Integration**: `play.libs.akka.AkkaGuiceSupport` for actor DI
- **Filters**: CORS, CSRF, security headers
- **Configuration**: HOCON-based configuration

---

## Upgrade Path Analysis

### Option 1: Upgrade Play Framework (Stay with Akka)

#### Recommended Target Version: Play 2.9.x
- **Current**: Play 2.7.2 (April 2019)
- **Target**: Play 2.9.5 (Latest stable as of 2024)
- **Intermediate**: Play 2.8.x (for smoother transition)

#### Breaking Changes from 2.7 to 2.9:

1. **Scala Version Requirements**
   - Play 2.9 requires Scala 2.13 minimum
   - Current: Scala 2.11.12 → Target: Scala 2.13.x
   - **Impact**: Major - All Scala dependencies need updating

2. **Java Version Requirements**
   - Play 2.9 requires Java 11+ (currently targeting Java 11, runtime Java 17)
   - **Impact**: Low - Already compatible

3. **Akka Version**
   - Play 2.9 uses Akka 2.6.x or 2.7.x (still under old Apache license)
   - **Impact**: Medium - Requires Akka upgrade from 2.5.22 to 2.6.x

4. **Guice Update**
   - Requires update to newer Guice version
   - **Impact**: Low - Mostly compatible

5. **HTTP Client Changes**
   - WS client API changes
   - **Impact**: Medium - May require code updates

6. **Deprecated APIs Removed**
   - Various deprecated APIs from 2.7 removed
   - **Impact**: Medium - Requires code review

#### Advantages of Staying with Akka:
- ✅ Smaller migration effort initially
- ✅ Existing Akka knowledge applicable
- ✅ More gradual upgrade path
- ✅ Extensive documentation and community support

#### Disadvantages of Staying with Akka:
- ❌ **LICENSE RISK**: Akka 2.7+ uses Business Source License (BSL) 1.1
- ❌ Commercial licensing required for production use after Sept 2023
- ❌ Akka 2.6 (last Apache-licensed) reached EOL
- ❌ No long-term sustainability without commercial support
- ❌ Play Framework itself is considering Pekko migration

---

### Option 2: Upgrade Play Framework AND Migrate to Pekko (RECOMMENDED)

#### Recommended Target Versions:
- **Play Framework**: 3.0.x (Pekko-based) or 2.9.x with manual Pekko migration
- **Pekko**: 1.0.x or 1.1.x
- **Scala**: 2.13.x or 3.x
- **Java**: 11 or 17

#### Migration Path:

##### Phase 1: Upgrade to Play 2.9.x with Akka 2.6.x
- Upgrade Scala to 2.13.x
- Update all Scala-based dependencies
- Fix compilation errors
- Update deprecated API usage
- Test thoroughly

##### Phase 2: Migrate Akka to Pekko
- Replace Akka dependencies with Pekko equivalents
- Update import statements (akka.* → org.apache.pekko.*)
- Update configuration (akka.* → pekko.*)
- Update ActorSystem initialization
- Test thoroughly

##### Phase 3: Upgrade to Play 3.0.x (Optional)
- Play 3.0 natively supports Pekko
- Further modernization of APIs
- Better Java 17+ support

---

## Akka to Pekko Migration Details

### 1. What is Apache Pekko?

Apache Pekko is a fork of Akka 2.6.x maintained by the Apache Software Foundation:
- **License**: Apache License 2.0 (open source)
- **Compatibility**: Binary compatible with Akka 2.6.x
- **Versioning**: Pekko 1.0.x = Akka 2.6.x equivalent
- **Community**: Growing Apache community support
- **Stability**: Production-ready, used by major projects

### 2. Package Name Changes

All package names change from `akka.*` to `org.apache.pekko.*`:

```
akka.actor.*              → org.apache.pekko.actor.*
akka.event.*              → org.apache.pekko.event.*
akka.pattern.*            → org.apache.pekko.pattern.*
akka.util.*               → org.apache.pekko.util.*
akka.routing.*            → org.apache.pekko.routing.*
akka.dispatch.*           → org.apache.pekko.dispatch.*
akka.serialization.*      → org.apache.pekko.serialization.*
akka.testkit.*            → org.apache.pekko.testkit.*
```

### 3. Dependency Changes

#### Maven Dependencies:

**Current (Akka):**
```xml
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-actor_2.11</artifactId>
    <version>2.5.22</version>
</dependency>
```

**Target (Pekko):**
```xml
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-actor_2.13</artifactId>
    <version>1.0.3</version>
</dependency>
```

#### Required Pekko Dependencies:
```xml
<!-- Core Pekko -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-actor_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Pekko Stream -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-stream_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Pekko Remote -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-remote_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Pekko SLF4J -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-slf4j_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Pekko TestKit -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-testkit_2.13</artifactId>
    <version>1.0.3</version>
    <scope>test</scope>
</dependency>

<!-- Pekko HTTP (if needed) -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-http_2.13</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- Pekko HTTP Core (if needed) -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-http-core_2.13</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 4. Configuration Changes

**Current (application.conf):**
```hocon
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  actor {
    provider = "akka.actor.LocalActorRefProvider"
    serializers {
      java = "akka.serialization.JavaSerializer"
    }
  }
}
```

**Target (application.conf):**
```hocon
pekko {
  loggers = ["org.apache.pekko.event.slf4j.Slf4jLogger"]
  actor {
    provider = "org.apache.pekko.actor.LocalActorRefProvider"
    serializers {
      java = "org.apache.pekko.serialization.JavaSerializer"
    }
  }
}
```

### 5. Code Changes Required

#### File: BaseActor.java
```java
// Before (Akka)
import akka.actor.UntypedAbstractActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;

public abstract class BaseActor extends UntypedAbstractActor {
    protected DiagnosticLoggingAdapter logger = Logging.getLogger(this);
    // ...
}

// After (Pekko)
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.event.DiagnosticLoggingAdapter;
import org.apache.pekko.event.Logging;

public abstract class BaseActor extends UntypedAbstractActor {
    protected DiagnosticLoggingAdapter logger = Logging.getLogger(this);
    // ...
}
```

#### File: RequestHandler.java
```java
// Before (Akka)
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;

// After (Pekko)
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
```

#### File: ActorStartModule.java
```java
// Before (Akka)
import akka.routing.FromConfig;
import akka.routing.RouterConfig;
import play.libs.akka.AkkaGuiceSupport;

// After (Pekko)
import org.apache.pekko.routing.FromConfig;
import org.apache.pekko.routing.RouterConfig;
import play.libs.pekko.PekkoGuiceSupport; // Play 3.0+
// OR manual DI configuration for Play 2.9
```

#### File: SignalHandler.java
```java
// Before (Akka)
import akka.actor.ActorSystem;

@Inject
public SignalHandler(ActorSystem actorSystem, Provider<Application> applicationProvider) {
    // ...
}

// After (Pekko)
import org.apache.pekko.actor.ActorSystem;

@Inject
public SignalHandler(ActorSystem actorSystem, Provider<Application> applicationProvider) {
    // ...
}
```

### 6. Play Framework Integration Changes

#### For Play 2.9.x with Pekko:
Play 2.9 doesn't natively support Pekko, so manual configuration is needed:

1. Remove `AkkaGuiceSupport` dependency
2. Manually configure Pekko ActorSystem in Guice module
3. Create custom actor injection mechanism

#### For Play 3.0.x with Pekko:
Play 3.0 has native Pekko support:

1. Use `play.libs.pekko.PekkoGuiceSupport`
2. Direct replacement of Akka-based APIs
3. Updated configuration structure

---

## Impact Analysis

### 1. Files Requiring Changes

#### Import Changes Only (Low Impact):
- All 14 Java files using Akka imports
- Test files using Akka TestKit
- Configuration files (application.conf)

#### Code Logic Changes (Medium Impact):
- ActorStartModule.java (Guice integration)
- SignalHandler.java (ActorSystem injection)
- RequestHandler.java (Future conversion)

#### Configuration Changes (Medium Impact):
- application.conf (akka → pekko namespace)
- pom.xml files (all 5 modules)
- Actor deployment configurations

### 2. Testing Requirements

**Critical Test Areas:**
1. ✅ Actor creation and lifecycle
2. ✅ Message passing and pattern matching
3. ✅ Router configurations (smallest-mailbox-pool)
4. ✅ Dispatcher configurations
5. ✅ Remote actor communication
6. ✅ Serialization/deserialization
7. ✅ Graceful shutdown with SignalHandler
8. ✅ Integration with Play controllers
9. ✅ Timeout handling
10. ✅ Error handling and supervision

### 3. Build System Changes

**Maven Changes Required:**
- Update parent POM properties
- Update all 5 module POMs
- Update Scala version to 2.13.x
- Update play2-maven-plugin
- Update all Scala-suffixed dependencies (_2.11 → _2.13)

**Potential Issues:**
- play2-maven-plugin may have limited Play 3.0 support
- Consider migration to SBT for better Play support
- Scala 2.13 binary incompatibility with 2.11

---

## Risk Assessment

### HIGH RISK Items:

1. **Scala Version Upgrade (2.11 → 2.13)**
   - Binary incompatibility
   - All Scala dependencies must be updated
   - Potential API changes in Scala standard library
   - **Mitigation**: Thorough testing, staged rollout

2. **Play Framework Major Version Jump**
   - Breaking API changes across 2.7 → 2.8 → 2.9 → 3.0
   - Deprecated features removed
   - Configuration changes
   - **Mitigation**: Incremental upgrades (2.7→2.8→2.9)

3. **Actor System Initialization**
   - Different DI patterns in Pekko
   - Play-Pekko integration may differ
   - **Mitigation**: Extensive integration testing

### MEDIUM RISK Items:

1. **Serialization Changes**
   - Custom serializers may need updates
   - Binary compatibility concerns
   - **Mitigation**: Test with actual message types

2. **Remote Actor Communication**
   - Netty configuration differences
   - Protocol compatibility
   - **Mitigation**: Test remote communication thoroughly

3. **Dispatcher Configuration**
   - Configuration syntax may differ slightly
   - Performance characteristics
   - **Mitigation**: Load testing with production-like scenarios

### LOW RISK Items:

1. **Import Statement Changes**
   - Mechanical replacement
   - Can be automated with scripts
   - **Mitigation**: Use IDE refactoring or sed/awk scripts

2. **Logger Configuration**
   - Simple namespace change
   - **Mitigation**: Minimal testing required

---

## Benefits of Migration

### Business Benefits:

1. **✅ License Compliance**
   - Apache 2.0 license is fully open source
   - No commercial licensing costs
   - No legal risks in production

2. **✅ Long-term Sustainability**
   - Apache Foundation backing
   - Community-driven development
   - Active maintenance and security updates

3. **✅ Cost Savings**
   - No Akka commercial license fees
   - No per-node licensing costs
   - Reduced vendor lock-in

### Technical Benefits:

1. **✅ Binary Compatibility**
   - Pekko 1.0.x is binary compatible with Akka 2.6.x
   - Smooth migration path
   - Can coexist during migration

2. **✅ Modern Java Support**
   - Better Java 11+ support
   - Future Java 17/21 LTS support
   - Modern API improvements

3. **✅ Community Support**
   - Growing Apache community
   - Play Framework moving to Pekko
   - Industry trend toward Pekko

4. **✅ Security Updates**
   - Regular security patches
   - Transparent security process
   - No commercial barrier to updates

5. **✅ Future-Proofing**
   - Aligned with Play Framework roadmap
   - Compatible with modern tooling
   - Continued innovation

---

## Drawbacks and Challenges

### Migration Challenges:

1. **⚠️ Time and Effort**
   - Estimated effort: 2-4 weeks for full migration
   - Requires thorough testing
   - Team training on new ecosystem

2. **⚠️ Scala Version Upgrade**
   - Breaking changes in Scala 2.11 → 2.13
   - All dependencies need updating
   - Potential compilation errors

3. **⚠️ Play Framework Upgrade**
   - Multiple version jumps required
   - API changes and deprecations
   - Configuration updates

4. **⚠️ Maven vs SBT**
   - play2-maven-plugin has limited support
   - SBT is preferred for Play
   - Potential build system migration

5. **⚠️ Testing Coverage**
   - Comprehensive testing required
   - Actor behavior verification
   - Performance testing needed

6. **⚠️ Documentation Gap**
   - Less Pekko documentation than Akka
   - Fewer Stack Overflow answers
   - Smaller community (currently)

### Technical Challenges:

1. **⚠️ Binary Dependencies**
   - Third-party libraries may still use Akka
   - Potential conflicts during transition
   - May need to fork or replace dependencies

2. **⚠️ Configuration Complexity**
   - All config paths need updating
   - Environment-specific configurations
   - Different behavior in edge cases

3. **⚠️ Remote Communication**
   - Wire protocol compatibility
   - Rolling update challenges
   - Monitoring and observability changes

---

## Recommended Approach

### Phased Migration Strategy:

#### Phase 1: Preparation (Week 1)
- ✅ Set up migration branch
- ✅ Inventory all Akka usage
- ✅ Update development environment
- ✅ Create automated tests for current behavior
- ✅ Set up CI/CD for new configuration

#### Phase 2: Scala & Play Upgrade (Week 2-3)
- ✅ Upgrade Scala 2.11 → 2.13
- ✅ Upgrade Play 2.7 → 2.8
- ✅ Fix compilation errors
- ✅ Update deprecated API usage
- ✅ Run full test suite
- ✅ Upgrade Play 2.8 → 2.9
- ✅ Repeat testing

#### Phase 3: Akka to Pekko Migration (Week 4-5)
- ✅ Replace Akka dependencies with Pekko
- ✅ Update all import statements (automated)
- ✅ Update configuration files
- ✅ Update ActorSystem initialization
- ✅ Update Guice modules
- ✅ Run full test suite
- ✅ Integration testing

#### Phase 4: Testing & Validation (Week 6)
- ✅ Unit testing
- ✅ Integration testing
- ✅ Performance testing
- ✅ Load testing
- ✅ Security testing
- ✅ Documentation updates

#### Phase 5: Deployment (Week 7-8)
- ✅ Deploy to staging environment
- ✅ Smoke testing
- ✅ Monitoring and observability
- ✅ Gradual production rollout
- ✅ Rollback plan ready

### Alternative: Stay on Play 2.9 + Akka 2.6

If timeline or resources are constrained:
- Upgrade to Play 2.9.x
- Stay on Akka 2.6.x (last Apache licensed)
- **Warning**: Akka 2.6 reached EOL, security risk
- Plan Pekko migration for next quarter

---

## Cost-Benefit Analysis

### Migration Costs:
- **Developer Time**: 6-8 weeks (1-2 developers)
- **Testing Time**: 2 weeks
- **Risk of Bugs**: Medium (with thorough testing)
- **Downtime**: Minimal (with blue-green deployment)

### Benefits:
- **License Cost Savings**: $0-$50K+ annually (depending on scale)
- **Legal Risk Reduction**: Eliminated
- **Long-term Sustainability**: High
- **Security Updates**: Guaranteed
- **Community Support**: Growing

### ROI Calculation:
- **One-time Cost**: ~$20-40K (developer time)
- **Annual Savings**: $10-50K+ (license + legal + future costs)
- **Payback Period**: 6-12 months
- **5-Year NPV**: Positive

---

## Recommendations

### Immediate Actions (This Quarter):

1. **✅ PROCEED with Migration**
   - Benefits outweigh costs
   - License compliance is critical
   - Future-proofs the application

2. **✅ Use Phased Approach**
   - Minimize risk
   - Allow for testing at each stage
   - Enable rollback points

3. **✅ Upgrade Path: Play 2.7 → 2.8 → 2.9 → Pekko**
   - Staged approach reduces risk
   - Each step is testable
   - Aligns with best practices

4. **✅ Consider Play 3.0 (Optional)**
   - Only if resources permit
   - Native Pekko support
   - Better long-term option

### Medium-term Actions (Next 6 Months):

1. ✅ Evaluate SBT migration
2. ✅ Upgrade to Java 17 LTS
3. ✅ Modernize build pipeline
4. ✅ Improve monitoring and observability

### Long-term Strategy:

1. ✅ Stay aligned with Play Framework roadmap
2. ✅ Follow Pekko community developments
3. ✅ Regular dependency updates
4. ✅ Continuous modernization

---

## Technical Specifications

### Target Architecture:

```
┌─────────────────────────────────────────┐
│         Play Framework 3.0.x            │
│      (or 2.9.x with Pekko compat)       │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│         Apache Pekko 1.0.x              │
│    (Actor System, Streams, Remote)      │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│              JDK 11/17                  │
│            Scala 2.13.x                 │
└─────────────────────────────────────────┘
```

### Dependency Matrix:

| Component | Current | Target | Compatibility |
|-----------|---------|--------|---------------|
| Play Framework | 2.7.2 | 2.9.5 or 3.0.x | Breaking changes |
| Akka/Pekko | Akka 2.5.22 | Pekko 1.0.3 | Binary compatible (with Akka 2.6) |
| Scala | 2.11.12 | 2.13.12 | Binary incompatible |
| Java | 11 (target) | 11 or 17 | Compatible |
| Maven Plugin | 1.0.0-rc5 | Latest | Check compatibility |

---

## Conclusion

### Summary:

The migration from Akka to Pekko is **HIGHLY RECOMMENDED** due to:
1. ✅ License compliance requirements (Apache 2.0)
2. ✅ Cost savings (no commercial licensing)
3. ✅ Long-term sustainability (Apache Foundation)
4. ✅ Alignment with Play Framework roadmap
5. ✅ Active community and support

### Risks:

The migration carries **MEDIUM RISK** primarily due to:
1. ⚠️ Scala version upgrade (2.11 → 2.13)
2. ⚠️ Play Framework version jumps
3. ⚠️ Testing requirements

### Recommendation:

**PROCEED with phased migration:**
- Start Q1: Scala + Play upgrade
- Complete Q1: Pekko migration
- Test thoroughly at each phase
- Maintain rollback capability

### Success Criteria:

1. ✅ All tests passing
2. ✅ Performance metrics maintained
3. ✅ Zero license compliance issues
4. ✅ Successful production deployment
5. ✅ Team trained on new stack

---

## Appendices

### A. Useful Resources

**Apache Pekko:**
- Official Site: https://pekko.apache.org/
- Documentation: https://pekko.apache.org/docs/pekko/current/
- Migration Guide: https://pekko.apache.org/docs/pekko/current/project/migration-guides.html
- GitHub: https://github.com/apache/incubator-pekko

**Play Framework:**
- Official Site: https://www.playframework.com/
- Migration Guides: https://www.playframework.com/documentation/latest/Migration
- Pekko Support: https://www.playframework.com/documentation/3.0.x/ScalaPekko

**Scala:**
- Scala 2.13 Migration: https://docs.scala-lang.org/overviews/core/collections-migration-213.html

### B. Automated Migration Tools

**Import Statement Replacement:**
```bash
# Find and replace imports (Linux/Mac)
find . -name "*.java" -type f -exec sed -i 's/import akka\./import org.apache.pekko./g' {} +
find . -name "*.scala" -type f -exec sed -i 's/import akka\./import org.apache.pekko./g' {} +
```

**Configuration Update:**
```bash
# Update application.conf
sed -i 's/^akka\./pekko./g' application.conf
sed -i 's/"akka\./"org.apache.pekko./g' application.conf
```

### C. Testing Checklist

- [ ] All actors start successfully
- [ ] Message routing works correctly
- [ ] Router pools function as expected
- [ ] Dispatchers configured properly
- [ ] Remote actors communicate
- [ ] Serialization works correctly
- [ ] Graceful shutdown operates
- [ ] Performance benchmarks met
- [ ] No memory leaks
- [ ] Logging functions properly
- [ ] Exception handling works
- [ ] Integration with Play controllers
- [ ] API endpoints respond correctly
- [ ] Load testing passed

---

**Report Generated**: 2025-10-07  
**Application**: certificate-registry  
**Status**: Analysis Complete - No Code Changes Made  
**Next Steps**: Await approval to proceed with migration
