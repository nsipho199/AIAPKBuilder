# Phase 4 Plan: Build System & Infrastructure 🏗️

## Project: AI APK Builder - Build System Implementation

**Phase**: 4 of 6  
**Status**: 📋 PLANNED  
**Estimated Duration**: 2 weeks  
**Complexity**: HIGH  
**Priority**: CRITICAL (MVP blocker)

---

## Executive Summary

Phase 4 implements the cloud build infrastructure that transforms generated source code into deliverable APK/AAB files. This phase bridges the gap between AI-generated code (Phase 2-3) and user-ready applications (Phase 5).

```
Generated Code → Build Provider → APK/AAB → User
      ↑                                        ↓
   Phase 3                                 Phase 4
```

---

## Phase 4 Objectives

### Primary Goals
1. ✅ **Multi-provider Build Support** - GitHub Actions, Codemagic, Docker, Self-hosted
2. ✅ **Real-Time Build Monitoring** - WebSocket streaming, progress updates, live logs
3. ✅ **Artifact Management** - Generation, caching, delivery of APK/AAB files
4. ✅ **Error Recovery** - Automatic retries, fallback builds, error diagnostics
5. ✅ **Cost Optimization** - Provider selection based on current costs, queue management

### Success Criteria
- [ ] Generated Android projects build successfully in <5 minutes
- [ ] Real-time build logs streamed to UI with <2 second latency
- [ ] Fallback between providers works seamlessly on failure
- [ ] APK/AAB artifacts downloadable from app within 10 seconds of build completion
- [ ] Build history persisted with success/failure tracking
- [ ] Cost estimation accurate to within 10%
- [ ] Offline Docker builds work without external dependencies

---

## Architecture Overview

### Build Pipeline Flow

```
┌────────────────────────────────────────────────────────────┐
│ Generated Project (from Phase 3)                           │
│ - Source code (.kt files)                                  │
│ - build.gradle.kts                                         │
│ - AndroidManifest.xml                                      │
│ - Resources (strings.xml, colors.xml)                      │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│ BuildExecutor (Orchestrator)                               │
│ - Select build provider                                    │
│ - Initialize build job                                     │
│ - Monitor progress                                         │
│ - Handle failures                                          │
└────────────────┬───────────────────────────────────────────┘
                 │
        ┌────────┼────────┐
        │        │        │
        ▼        ▼        ▼
    ┌─────┐ ┌─────────┐ ┌──────┐
    │ GHA │ │Codemagic│ │Docker│
    └────┴─┴─────────┴─┴──────┘
        │         │        │
        └────────┬┴────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│ LogAggregator + ArtifactManager                            │
│ - Collect logs from all providers                          │
│ - Stream to WebSocket clients                              │
│ - Store artifacts in cache                                 │
│ - Generate download links                                  │
└────────────────┬───────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│ BuildJob (Database Storage)                                │
│ - Status (QUEUED → RUNNING → SUCCESS/FAILED)               │
│ - Logs (persisted for history)                             │
│ - Artifacts (APK, AAB, mappings)                           │
│ - Timing (started, completed, duration)                    │
│ - Costs (provider-specific tracking)                       │
└────────────────────────────────────────────────────────────┘
```

### Component Breakdown

```
BuildSystem/
├── Providers/
│   ├── GitHubActionsProvider.kt      (REST API for workflows)
│   ├── CodemagicProvider.kt          (REST API for builds)
│   ├── DockerProvider.kt             (Docker daemon communication)
│   └── SelfHostedProvider.kt         (Custom endpoint)
├── Orchestration/
│   ├── BuildExecutor.kt              (Orchestrator & state machine)
│   ├── BuildProviderFactory.kt       (Provider instantiation)
│   └── BuildScheduler.kt             (WorkManager integration)
├── Monitoring/
│   ├── LogAggregator.kt              (Log collection & streaming)
│   ├── BuildProgressTracker.kt       (Status updates)
│   └── HealthChecker.kt              (Provider availability)
├── Artifacts/
│   ├── ArtifactManager.kt            (APK/AAB management)
│   ├── ArtifactCache.kt              (Storage & retrieval)
│   └── DownloadService.kt            (Client delivery)
└── Models/
    ├── BuildRequest.kt               (Input specification)
    ├── BuildResponse.kt              (Output specification)
    ├── BuildLog.kt                   (Log entry)
    └── Artifact.kt                   (Generated files)
```

---

## Detailed Implementation Plan

### Week 1: Core Build Infrastructure

#### Task 1.1: Build Models & Data Layer (2 days)
**Deliverables**:
- [ ] `BuildRequest` data class (project source, provider config, options)
- [ ] `BuildResponse` data class (build status, APK path, timing)
- [ ] `BuildLog` entity for database persistence
- [ ] `Artifact` entity for APK/AAB storage tracking
- [ ] Room database updates with new DAOs
- [ ] Domain models for provider abstraction

**Files to Create**:
```
data/
├── model/
│   ├── BuildRequest.kt
│   ├── BuildResponse.kt
│   ├── BuildLog.kt
│   └── Artifact.kt
└── local/
    ├── dao/
    │   ├── BuildLogDao.kt
    │   └── ArtifactDao.kt
```

**Example BuildRequest**:
```kotlin
data class BuildRequest(
    val projectId: String,
    val projectName: String,
    val sourceCode: GeneratedProjectFiles,
    val buildProvider: BuildProvider,        // GITHUB_ACTIONS, CODEMAGIC, DOCKER
    val buildType: BuildType,                 // DEBUG, RELEASE
    val signingConfig: SigningConfig?,        // For RELEASE builds
    val options: Map<String, String> = emptyMap()
)

data class BuildResponse(
    val buildId: String,
    val status: BuildStatus,                  // QUEUED, RUNNING, SUCCESS, FAILED
    val apkPath: String?,
    val aabPath: String?,
    val logsUrl: String?,
    val startTime: Long,
    val endTime: Long?,
    val duration: Long?,
    val errorMessage: String?,
    val artifacts: List<Artifact> = emptyList()
)
```

#### Task 1.2: Build Provider Interfaces (1 day)
**Deliverables**:
- [ ] `IBuildProvider` interface with standard operations
- [ ] `BuildProviderFactory` for instance creation
- [ ] Provider configuration management
- [ ] Provider availability checker

**Files to Create**:
```
service/
├── build/
│   ├── BuildProvider.kt               (interface)
│   ├── BuildProviderFactory.kt        (factory)
│   ├── BuildProviderConfig.kt         (configuration)
│   └── ProviderHealthChecker.kt       (availability)
```

**Example IBuildProvider Interface**:
```kotlin
interface IBuildProvider {
    suspend fun initiateBuild(request: BuildRequest): BuildResponse
    suspend fun getBuildStatus(buildId: String): BuildResponse
    suspend fun cancelBuild(buildId: String): Boolean
    suspend fun downloadArtifact(artifactId: String): File
    suspend fun getProviderHealth(): ProviderHealth
}

enum class BuildStatus {
    QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED
}

data class ProviderHealth(
    val isAvailable: Boolean,
    val queueLength: Int,
    val estimatedWaitTime: Long,
    val costPerBuild: Float
)
```

#### Task 1.3: Build Executor (2 days)
**Deliverables**:
- [ ] `BuildExecutor` orchestrator class
- [ ] State machine for build lifecycle
- [ ] Provider selection logic (cost-based, availability-based)
- [ ] Error handling and retry mechanism
- [ ] Build cancellation support

**Files to Create**:
```
service/
├── build/
│   └── BuildExecutor.kt               (orchestrator)
```

**Key Features**:
```kotlin
class BuildExecutor(
    private val providers: Map<BuildProvider, IBuildProvider>,
    private val logAggregator: LogAggregator,
    private val artifactManager: ArtifactManager,
    private val buildRepository: BuildJobRepository
) {
    // State machine for build lifecycle
    suspend fun executeBuild(request: BuildRequest): BuildResponse {
        // 1. Validate input
        // 2. Select provider (cheapest/fastest)
        // 3. Initiate build on provider
        // 4. Monitor progress (WebSocket streaming)
        // 5. Handle errors with fallback
        // 6. Download & cache artifacts
        // 7. Persist in database
        // 8. Emit completion event
    }
    
    private suspend fun selectProvider(): IBuildProvider {
        // Choose based on: cost, current load, availability, user preference
    }
    
    private suspend fun executeWithFallback(request: BuildRequest): BuildResponse {
        // Try primary provider
        // If fails, try secondary provider
        // If fails, try tertiary provider
        // If all fail, return error
    }
}
```

---

### Week 1-2: Provider Implementations

#### Task 2.1: GitHub Actions Provider (3 days)
**Deliverables**:
- [ ] GitHub Actions workflow dispatch support
- [ ] Run status polling mechanism
- [ ] Artifact download from run
- [ ] Log streaming from run logs
- [ ] Cost calculation (based on free/paid tiers)

**Files to Create**:
```
service/
├── build/
│   ├── provider/
│   │   └── GitHubActionsProvider.kt
│   └── api/
│       └── GitHubActionsApiService.kt (Retrofit)
```

**Key Implementation Points**:
```kotlin
class GitHubActionsProvider(
    private val apiService: GitHubActionsApiService,
    private val logAggregator: LogAggregator
) : IBuildProvider {
    
    override suspend fun initiateBuild(request: BuildRequest): BuildResponse {
        // 1. Upload source code to temp branch
        // 2. Create workflow file if not exists
        // 3. Trigger workflow dispatch
        // 4. Return run ID
    }
    
    suspend fun getBuildStatus(buildId: String): BuildResponse {
        // Poll GitHub API for run status
        // QUEUED → RUNNING → SUCCESS/FAILURE
    }
    
    suspend fun streamLogs(buildId: String) {
        // Download run logs and stream via WebSocket
    }
    
    suspend fun downloadArtifact(artifactId: String): File {
        // Download artifact from GitHub Actions run
    }
}
```

**Workflow Template** (will be created in repo):
```yaml
name: Build APK
on:
  workflow_dispatch:
    inputs:
      project_source: { required: true }
      build_type: { required: true }

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v3
```

#### Task 2.2: Codemagic Provider (2 days)
**Deliverables**:
- [ ] Codemagic REST API integration
- [ ] Build triggering and status polling
- [ ] Real-time artifact availability
- [ ] Cost tracking (per-build charges)
- [ ] Error handling (specific to Codemagic)

**Files to Create**:
```
service/
├── build/
│   ├── provider/
│   │   └── CodemagicProvider.kt
│   └── api/
│       └── CodemagicApiService.kt    (Retrofit)
```

**Implementation Focus**:
```kotlin
class CodemagicProvider(
    private val apiService: CodemagicApiService,
    private val config: CodemagicConfig
) : IBuildProvider {
    
    override suspend fun initiateBuild(request: BuildRequest): BuildResponse {
        // 1. Push source code to Codemagic
        // 2. Trigger build using API
        // 3. Return build ID
    }
    
    // Similar to GitHub Actions but using Codemagic's REST API
}
```

**API Endpoints**:
- `POST /builds/` - Start build
- `GET /builds/{id}/` - Get build status
- `GET /builds/{id}/logs/` - Get build logs
- `GET /builds/{id}/artifacts/` - List artifacts

#### Task 2.3: Docker Provider (2 days)
**Deliverables**:
- [ ] Docker daemon communication
- [ ] Android emulator/build environment setup
- [ ] Build execution in container
- [ ] Local file extraction
- [ ] Performance optimization

**Files to Create**:
```
service/
├── build/
│   ├── provider/
│   │   └── DockerProvider.kt
│   └── docker/
│       └── DockerService.kt
```

**Key Features**:
```kotlin
class DockerProvider(
    private val dockerService: DockerService,
    private val config: DockerConfig
) : IBuildProvider {
    
    override suspend fun initiateBuild(request: BuildRequest): BuildResponse {
        // 1. Create Docker container with Android SDK
        // 2. Copy source code to container
        // 3. Execute ./gradlew assembleDebug
        // 4. Extract APK from container
        // 5. Clean up container
    }
    
    private suspend fun createBuildContainer(): String {
        // Create Ubuntu container with Android SDK pre-installed
    }
    
    private suspend fun executeBuild(containerId: String): BuildResult {
        // Run gradlew build inside container
    }
}
```

**Docker Image Specs**:
```dockerfile
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y openjdk-17-jdk
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-*.zip
# ... Android SDK setup
WORKDIR /build
```

---

### Week 2: Monitoring & Artifact Management

#### Task 3.1: Build Executor & Orchestration (2 days)
**Deliverables**:
- [ ] Complete BuildExecutor implementation
- [ ] Provider selection algorithm
- [ ] Automatic fallback on failure
- [ ] Build history tracking
- [ ] Cost-based provider preference

#### Task 3.2: Log Aggregator & WebSocket (2 days)
**Deliverables**:
- [ ] Real-time log streaming via WebSocket
- [ ] Log buffering and compression
- [ ] Client connection management
- [ ] Log persistence (database)

**Files to Create**:
```
service/
├── build/
│   └── LogAggregator.kt
```

**Implementation**:
```kotlin
class LogAggregator(
    private val buildRepository: BuildJobRepository,
    private val websocketManager: WebSocketManager
) {
    
    fun subscribeToBuildLogs(buildId: String, subscriber: (LogEntry) -> Unit) {
        // Open WebSocket connection
        // Stream logs as they arrive
        // Buffer and persist to database
    }
    
    suspend fun getBuildLogs(buildId: String): List<BuildLog> {
        // Retrieve from database
    }
}
```

#### Task 3.3: Artifact Manager (1 day)
**Deliverables**:
- [ ] APK/AAB caching strategy
- [ ] Download link generation
- [ ] Artifact cleanup (old builds)
- [ ] Storage quota management

**Files to Create**:
```
service/
├── build/
│   ├── ArtifactManager.kt
│   └── ArtifactCache.kt
```

---

## Data Model Enhancements

### Room Database Updates

```kotlin
// New Entity for BuildJob (enhanced from Phase 1)
@Entity(tableName = "build_jobs")
data class BuildJob(
    @PrimaryKey val buildId: String,
    val projectId: String,
    val buildProvider: String,              // GITHUB_ACTIONS, CODEMAGIC, DOCKER
    val status: BuildStatus,                // QUEUED, RUNNING, SUCCESS, FAILED
    val startTime: Long,
    val endTime: Long?,
    val durationMillis: Long?,
    val apkPath: String?,
    val aabPath: String?,
    val logsPath: String?,
    val errorMessage: String?,
    val costEstimate: Float,
    val costActual: Float?,
    val retryCount: Int = 0,
    val fallbackProvider: String?           // Provider used after retry
)

@Entity(tableName = "build_logs")
data class BuildLogEntity(
    @PrimaryKey val logId: String,
    val buildId: String,
    val timestamp: Long,
    val level: String,                      // INFO, WARNING, ERROR
    val message: String,
    val component: String                   // "gradle", "aapt", "compiler", etc.
)

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey val artifactId: String,
    val buildId: String,
    val type: String,                       // APK, AAB, MAPPING, LOG
    val filePath: String,
    val fileSize: Long,
    val checksum: String,
    val createdAt: Long,
    val expiresAt: Long
)
```

---

## Integration Points

### With Phase 3 (UI)
- [ ] Update `GenerateViewModel` to trigger builds
- [ ] Add `BuildProgressScreen` for real-time monitoring
- [ ] Add download button to `ProjectDetailScreen`
- [ ] WebSocket connection management in ViewModel

### With Phase 2 (AI Code Generation)
- [ ] Ensure generated projects are buildable
- [ ] Validation of generated code structure
- [ ] Error reporting back to code generator

### With Phase 1 (Data Layer)
- [ ] BuildJob entity and DAO
- [ ] BuildLog persistence
- [ ] Artifact tracking

---

## Testing Strategy

### Unit Tests
- [ ] Provider selection algorithm
- [ ] Fallback logic on failures
- [ ] Cost calculation
- [ ] Log parsing
- [ ] Artifact management

### Integration Tests
- [ ] End-to-end build flow
- [ ] Provider availability checks
- [ ] WebSocket streaming
- [ ] Database persistence

### E2E Tests
- [ ] Generate project → Build → Download APK
- [ ] Multiple concurrent builds
- [ ] Provider fallback scenarios
- [ ] Network failure recovery

---

## Deliverables Checklist

### Week 1
- [ ] Build models and data layer (Task 1.1)
- [ ] Provider interfaces (Task 1.2)
- [ ] Build executor skeleton (Task 1.3)
- [ ] GitHub Actions provider (Task 2.1)

### Week 2
- [ ] Codemagic provider (Task 2.2)
- [ ] Docker provider (Task 2.3)
- [ ] Log aggregator (Task 3.2)
- [ ] Artifact manager (Task 3.3)
- [ ] Complete orchestration (Task 3.1)
- [ ] UI integration
- [ ] Testing suite

---

## Success Metrics

### Performance
- [ ] Build initiation: <5 seconds
- [ ] Build completion: <5 minutes (most cases)
- [ ] Log streaming latency: <2 seconds
- [ ] Artifact download: <10 seconds

### Reliability
- [ ] Provider availability: >99.5%
- [ ] Build success rate: >95% (for valid projects)
- [ ] Fallback success rate: >99%
- [ ] Log completeness: 100%

### Quality
- [ ] Code coverage: >80%
- [ ] Error handling: All paths covered
- [ ] Documentation: 100% complete
- [ ] Type safety: 100%

---

## Known Challenges & Solutions

### Challenge 1: Multi-Provider Synchronization
**Problem**: Different providers have different status update frequencies
**Solution**: Abstract via polling interface with configurable intervals

### Challenge 2: Long-Running Builds
**Problem**: Network interruptions during 5-minute builds
**Solution**: Checkpoint-based progress tracking with resume capability

### Challenge 3: Cost Tracking Accuracy
**Problem**: Each provider bills differently
**Solution**: Provider-specific cost calculators with audit trail

### Challenge 4: Large Artifact Downloads
**Problem**: APK files can be 50+ MB
**Solution**: Chunked transfer, pause/resume, compression options

### Challenge 5: Concurrent Build Limits
**Problem**: Providers limit concurrent builds
**Solution**: Priority queue with intelligent scheduling

---

## Dependencies & Prerequisites

### Required APIs
- GitHub: Personal access token with repo + workflow scopes
- Codemagic: API key
- Docker: Local daemon or remote endpoint

### Required Libraries (already in project)
- Retrofit (HTTP client)
- Room (database)
- WorkManager (background tasks)
- Coroutines (async operations)

### New Dependencies to Add
- OkHttp WebSocket (real-time logs)
- Docker SDK (docker-java)
- Protocol Buffers (optional, for log compression)

---

## Next Steps (Phase 5)

Phase 5 will focus on:
- APK/AAB export functionality
- Source code export as ZIP
- Project management (CRUD, favorites)
- Backup & restore
- GitHub repository integration

---

## References

- GitHub Actions API: https://docs.github.com/en/rest/actions
- Codemagic API: https://docs.codemagic.io/rest-api/
- Docker Engine API: https://docs.docker.com/engine/api/
- Android Build System: https://developer.android.com/build

---

**Phase 4 is ready to begin!** 🚀

Continue to `PHASE4_CHECKLIST.md` for detailed task tracking.
