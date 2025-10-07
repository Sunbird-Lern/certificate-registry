# Quick Reference Guide: Akka to Pekko Migration

## One-Page Summary

### Current State
- **Play Framework**: 2.7.2 (2019)
- **Akka**: 2.5.22 (2019, Apache License)
- **Scala**: 2.11.12
- **Status**: Both outdated, Akka license changed to BSL 1.1

### Target State (Recommended)
- **Play Framework**: 2.9.5 or 3.0.x
- **Pekko**: 1.0.3 (Apache License 2.0)
- **Scala**: 2.13.12
- **Status**: Modern, open source, sustainable

---

## Why Migrate?

### License Issue (CRITICAL)
❌ **Akka 2.7+**: Business Source License (BSL) 1.1 - requires commercial license  
✅ **Pekko**: Apache License 2.0 - fully open source

### Benefits
1. ✅ **Free**: No licensing costs
2. ✅ **Open Source**: Apache Foundation backed
3. ✅ **Compatible**: Binary compatible with Akka 2.6
4. ✅ **Sustainable**: Active development and support
5. ✅ **Future-proof**: Play Framework moving to Pekko

### Costs
- ⏱️ **Time**: 4-6 weeks effort
- 🧪 **Testing**: Extensive testing required
- 📚 **Learning**: Team training needed
- ⚠️ **Risk**: Medium (mitigated by phased approach)

---

## Quick Migration Checklist

### Phase 1: Pre-Migration (Week 1)
- [ ] Create migration branch
- [ ] Set up CI/CD for new config
- [ ] Baseline performance metrics
- [ ] Team review of migration plan

### Phase 2: Scala & Play Upgrade (Week 2-3)
- [ ] Update Scala 2.11 → 2.13 in all POMs
- [ ] Update Play 2.7 → 2.8 → 2.9
- [ ] Fix compilation errors
- [ ] Run full test suite
- [ ] Performance testing

### Phase 3: Pekko Migration (Week 4)
- [ ] Replace Akka dependencies with Pekko
- [ ] Run automated import replacement script
- [ ] Update configuration files (akka → pekko)
- [ ] Update ActorStartModule for DI
- [ ] Run full test suite

### Phase 4: Testing (Week 5)
- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance tests
- [ ] Load tests
- [ ] Security tests

### Phase 5: Deployment (Week 6)
- [ ] Deploy to staging
- [ ] Smoke tests
- [ ] Canary deployment (5%)
- [ ] Gradual rollout (100%)
- [ ] Monitor for 2 weeks

---

## Import Mappings

### Actor System
```java
// Before
import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;

// After
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
```

### Patterns & Utils
```java
// Before
import akka.pattern.Patterns;
import akka.util.Timeout;
import akka.routing.FromConfig;

// After
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.apache.pekko.routing.FromConfig;
```

### Events & Logging
```java
// Before
import akka.event.Logging;
import akka.event.DiagnosticLoggingAdapter;

// After
import org.apache.pekko.event.Logging;
import org.apache.pekko.event.DiagnosticLoggingAdapter;
```

### Testing
```java
// Before
import akka.testkit.javadsl.TestKit;

// After
import org.apache.pekko.testkit.javadsl.TestKit;
```

---

## Dependency Changes

### Maven POM Properties
```xml
<!-- Before -->
<properties>
    <akka.x.version>2.5.22</akka.x.version>
    <scala.major.version>2.11</scala.major.version>
    <play2.version>2.7.2</play2.version>
</properties>

<!-- After -->
<properties>
    <pekko.version>1.0.3</pekko.version>
    <scala.major.version>2.13</scala.major.version>
    <play2.version>2.9.5</play2.version>
</properties>
```

### Actor Dependencies
```xml
<!-- Before -->
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-actor_2.11</artifactId>
    <version>2.5.22</version>
</dependency>

<!-- After -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-actor_2.13</artifactId>
    <version>1.0.3</version>
</dependency>
```

### Complete Dependency List
```xml
<!-- Core -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-actor_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Stream -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-stream_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Remote -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-remote_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- SLF4J Logging -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-slf4j_2.13</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- TestKit -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-testkit_2.13</artifactId>
    <version>1.0.3</version>
    <scope>test</scope>
</dependency>

<!-- HTTP (if needed) -->
<dependency>
    <groupId>org.apache.pekko</groupId>
    <artifactId>pekko-http_2.13</artifactId>
    <version>1.0.1</version>
</dependency>
```

---

## Configuration Changes

### application.conf
```hocon
# Before
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  actor {
    provider = "akka.actor.LocalActorRefProvider"
    serializers {
      java = "akka.serialization.JavaSerializer"
    }
  }
}

# After
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

---

## Automated Migration Script

```bash
#!/bin/bash
# Quick migration script

# 1. Backup
tar -czf backup-$(date +%Y%m%d).tar.gz .

# 2. Replace Java imports
find . -name "*.java" -type f -exec sed -i 's/import akka\./import org.apache.pekko./g' {} +

# 3. Replace configuration
find . -name "*.conf" -type f -exec sed -i 's/^akka\./pekko./g' {} +
find . -name "*.conf" -type f -exec sed -i 's/"akka\./"org.apache.pekko./g' {} +

# 4. Verify (should return empty)
echo "Remaining akka imports:"
grep -r "import akka\." --include="*.java" . || echo "None found - Good!"

echo "Migration script complete. Now update POMs manually."
```

---

## Files to Update

### Critical (Must Update)
1. ✅ `pom.xml` (parent + all 5 modules)
2. ✅ `BaseActor.java` - Base class for actors
3. ✅ `ActorStartModule.java` - DI configuration
4. ✅ `RequestHandler.java` - Ask pattern
5. ✅ `SignalHandler.java` - Graceful shutdown
6. ✅ `application.conf` - Actor configuration

### Medium Priority
7. ✅ `CertificationActor.java` - Main business logic
8. ✅ `CertificateController.java` - HTTP endpoints
9. ✅ `CertificateUtil.java` - Utility methods
10. ✅ `ElasticSearchRestHighImpl.java` - ES integration

### Low Priority
11. ✅ Test files (all)
12. ✅ Other utility files

---

## Testing Commands

```bash
# Clean build
mvn clean install

# Run tests
mvn test

# Run specific test
mvn test -Dtest=CertificationActorTest

# Build service
cd service
mvn play2:dist

# Run service (dev mode)
mvn play2:run

# Check for Akka references
grep -r "akka" --include="*.java" --include="*.conf" . | grep -v "pekko"
```

---

## Rollback Plan

### If Migration Fails
1. **Stop deployment** immediately
2. **Revert** to previous Docker image/artifact
3. **Restore** old configuration
4. **Analyze** root cause
5. **Re-plan** migration approach

### Rollback Command
```bash
# Restore from backup
tar -xzf backup-YYYYMMDD.tar.gz

# Or git revert
git revert <migration-commit>
git push
```

---

## Common Issues & Solutions

### Issue 1: Compilation Errors
**Problem**: Cannot find Pekko classes  
**Solution**: Check Maven dependency versions, run `mvn clean install`

### Issue 2: Actor Not Starting
**Problem**: Actor injection fails  
**Solution**: Verify ActorStartModule configuration, check actor names

### Issue 3: Tests Failing
**Problem**: TestKit issues  
**Solution**: Update test imports, verify ActorSystem creation

### Issue 4: Performance Degradation
**Problem**: Slower than Akka  
**Solution**: Check dispatcher configuration, adjust pool sizes

### Issue 5: Configuration Not Loading
**Problem**: Pekko config not recognized  
**Solution**: Verify namespace changes (akka → pekko), check HOCON syntax

---

## Resources

### Official Documentation
- **Pekko**: https://pekko.apache.org/docs/pekko/current/
- **Play Framework**: https://www.playframework.com/documentation/
- **Migration Guide**: https://pekko.apache.org/docs/pekko/current/project/migration-guides.html

### Community Support
- **GitHub**: https://github.com/apache/incubator-pekko
- **Mailing List**: dev@pekko.apache.org
- **Stack Overflow**: Tag [apache-pekko]

### Tools
- **Maven**: https://maven.apache.org/
- **SBT** (alternative): https://www.scala-sbt.org/

---

## Success Metrics

### Must Have
- ✅ All tests passing
- ✅ No runtime errors
- ✅ Performance within 10% of baseline
- ✅ Zero production incidents

### Nice to Have
- ✅ Improved build times
- ✅ Better memory usage
- ✅ Enhanced monitoring
- ✅ Updated documentation

---

## Next Steps

1. **Review** this guide and full reports
2. **Get approval** from stakeholders
3. **Schedule** migration window
4. **Execute** phased migration plan
5. **Monitor** and validate
6. **Document** lessons learned

---

**Quick Start**: Read main report → Update POMs → Run migration script → Test → Deploy

**Estimated Time**: 4-6 weeks full-time

**Risk Level**: Medium (with proper testing)

**Recommendation**: ✅ **PROCEED** - Benefits outweigh costs
