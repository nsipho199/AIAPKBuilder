# Phase 4 Implementation Guide 📖

## Build System & Infrastructure Implementation

**Phase**: 4 of 6  
**Duration**: 2 weeks  
**Complexity**: HIGH  
**Status**: ✅ COMPLETE

---

## Architecture Overview

### Build Pipeline Flow

```
Generated Code
      ↓
ProjectRepository.generateAndCreateProject()
      ↓
BuildExecutor.executeBuild()
      ├─ BuildProviderFactory.selectBestProvider()
      ├─ provider.startBuild()
      ├─ LogAggregator.aggregateLogs()
      └─ monitor build status → completion
      ↓
ArtifactManager.registerArtifact()
      ├─ ArtifactCache.storeArtifact()
      ├─ validate SHA256
      └─ update database
      ↓
APK/AAB Ready for Download
```

### Component Interactions

```
                    ┌─────────────────────┐
                    │   BuildExecutor     │
                    │   (Orchestrator)    │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┼─────────────┐
                 │             │             │
         ┌───────▼───────┐ ┌──▼──────────┐ │
         │ LogAggregator │ │ArtifactMgr  │ │
         └───────────────┘ └─────────────┘ │
                 │                         │
      ┌──────────┴──────────┐              │
      │                     │              │
  ┌───▼──┐ ┌────────┐ ┌────▼──┐          │
  │GitHub│ │Codemagic│ │Docker │          │
  │Actions│ └────────┘ └───────┘          │
  └──────┘                                 │
                                           │
                                   ┌───────▼────────┐
                                   │ ProjectRepository
                                   │ (Data Persistence)
                                   └────────────────┘
```

---

## Core Components

### 1. IBuildProvider Interface

**Location**: `data/service/build/IBuildProvider.kt`

```kotlin
interface IBuildProvider {
    val provider: BuildProvider
    suspend fun isHealthy(): Boolean
    suspend fun getCostEstimate(): Double?
    suspend fun startBuild(...): Result<BuildJob>
    suspend fun getBuildStatus(jobId: String): Result<BuildJob>
    fun subscribeToLogs(jobId: String): Flow<String>
    suspend fun cancelBuild(jobId: String): Result<Boolean>
    suspend fun downloadArtifact(jobId: String): Result<String>
    fun getEstimatedBuildTime(): Int?
}
```

**Purpose**: Defines unified interface for all build providers

### 2. BuildProviderFactory

**Location**: `data/service/build/BuildProviderFactory.kt`

```kotlin
class BuildProviderFactory {
    fun getProvider(provider: BuildProvider): IBuildProvider
    fun getAvailableProviders(): List<IBuildProvider>
    suspend fun getHealthyProviders(): List<IBuildProvider>
    suspend fun selectBestProvider(): IBuildProvider?
}
```

**Purpose**: Manages provider instantiation and selection

### 3. BuildExecutor

**Location**: `data/service/build/BuildExecutor.kt`

**Key Methods**:
- `executeBuild()`: Main entry point for builds
- `monitorBuildProgress()`: Tracks build status
- `cancelBuild()`: Stops running builds
- `retryBuild()`: Retries failed builds

**Features**:
- Automatic provider selection
- Progress callbacks
- Error recovery with retry
- Fallback provider support

### 4. Provider Implementations

#### GitHubActionsProvider
- **File**: `provider/GitHubActionsProvider.kt`
- **API**: GitHub REST API v3
- **Features**: Workflow dispatch, artifact download, log streaming

#### CodemagicProvider
- **File**: `provider/CodemagicProvider.kt`
- **API**: Codemagic REST API
- **Features**: Build triggering, status polling, artifact retrieval

#### DockerProvider
- **File**: `provider/DockerProvider.kt`
- **Service**: `docker/DockerService.kt`
- **Features**: Local containerized builds, process isolation

### 5. LogAggregator

**Location**: `data/service/build/LogAggregator.kt`

**Methods**:
- `aggregateLogs()`: Collects logs from provider stream
- `getLogs()`: Returns current logs
- `getLogFlow()`: Streams logs as Flow<String>
- `searchLogs()`: Find logs by pattern
- `getLogStats()`: Extract build statistics

### 6. ArtifactManager

**Location**: `data/service/build/ArtifactManager.kt`

**Methods**:
- `registerArtifact()`: Store artifact from build
- `getDownloadUrl()`: Get secure download URL
- `downloadArtifact()`: Download to temp location
- `validateArtifact()`: Verify SHA256 hash
- `cleanupOldArtifacts()`: Maintain storage space

### 7. ArtifactCache

**Location**: `data/service/build/ArtifactCache.kt`

**Features**:
- LRU-based caching strategy
- Configurable max size (5GB default)
- Automatic cleanup of old files
- File validation support

---

## Data Models

### BuildJob Entity
```kotlin
@Entity(tableName = "build_jobs")
data class BuildJob(
    @PrimaryKey val jobId: String,
    val projectId: String,
    val provider: BuildProvider,
    val status: BuildStatus,
    val startedAt: Long,
    val completedAt: Long? = null,
    val logOutput: String = "",
    val artifactUrl: String? = null,
    val errorMessage: String? = null,
    val progressPercent: Int = 0
)
```

### BuildArtifact Entity
```kotlin
@Entity(tableName = "artifacts")
data class BuildArtifact(
    @PrimaryKey val id: String,
    val buildJobId: String,
    val projectId: String,
    val artifactType: String, // "apk", "aab", "source"
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val fileName: String,
    val fileSizeBytes: Long,
    val sha256Hash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### BuildConfig Entity
```kotlin
@Entity(tableName = "build_configs")
data class BuildConfig(
    @PrimaryKey val id: String,
    val projectId: String,
    val provider: BuildProvider,
    val configJson: String, // JSON-serialized config
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## Usage Examples

### Starting a Build

```kotlin
// In GenerateViewModel
val buildResult = buildExecutor.executeBuild(
    projectId = projectId,
    preferredProvider = BuildProvider.GITHUB_ACTIONS,
    onProgress = { percent, message ->
        _uiState.update { it.copy(
            buildProgress = percent / 100f,
            buildMessage = message
        )}
    }
)

if (buildResult.isSuccess) {
    val job = buildResult.getOrNull()!!
    // Build completed
} else {
    val error = buildResult.exceptionOrNull()
    // Handle error
}
```

### Monitoring Build Progress

```kotlin
// In ProjectDetailViewModel
fun startBuild() {
    val project = _uiState.value.project ?: return
    viewModelScope.launch {
        val buildResult = buildExecutor.executeBuild(
            projectId = project.id,
            onProgress = { percent, message ->
                _uiState.update { it.copy(
                    buildProgress = percent / 100f,
                    buildMessage = message
                )}
            }
        )
        
        if (buildResult.isSuccess) {
            val job = buildResult.getOrNull()!!
            loadBuildLogs(job.jobId)
        }
    }
}
```

### Accessing Build Logs

```kotlin
// Get current logs
val logs = logAggregator.getLogs(jobId)

// Stream logs as they arrive
logAggregator.getLogFlow(jobId).collect { line ->
    println(line)
}

// Search for specific content
val errors = logAggregator.searchLogs(jobId, "error")

// Get build statistics
val stats = logAggregator.getLogStats(jobId)
```

### Managing Artifacts

```kotlin
// Download artifact
val downloadResult = artifactManager.downloadArtifact(artifactId)
if (downloadResult.isSuccess) {
    val filePath = downloadResult.getOrNull()!!
    // Use downloaded file
}

// Validate artifact integrity
val isValid = artifactManager.validateArtifact(artifactId)

// Get cache statistics
val stats = artifactCache.getCacheStats()
// {totalSizeBytes: 1024000, fileCount: 5, usagePercent: 20}
```

---

## Provider Configuration

### GitHub Actions
```kotlin
val config = mapOf(
    "repoOwner" to "aiapkbuilder",
    "repoName" to "builds",
    "workflowId" to "android-build.yml"
)
```

### Codemagic
```kotlin
val config = mapOf(
    "apiKey" to "your_api_key",
    "appId" to "your_app_id",
    "branch" to "main"
)
```

### Docker
```kotlin
val config = mapOf(
    "dockerImage" to "aiapkbuilder/android-build:latest",
    "workspaceDir" to "/workspace"
)
```

---

## Performance Considerations

### Build Time Optimization
- Provider selection based on current load
- Parallel builds across projects
- Artifact caching for faster access

### Memory Management
- Streaming logs to prevent buffering
- Incremental artifact processing
- Container cleanup after builds

### Cost Optimization
- Free tier preference (Docker)
- Cost estimation per provider
- Automatic provider fallback

---

## Error Handling

### Build Failures
```kotlin
BuildExecutor.executeBuild()
├─ Provider not healthy → select fallback
├─ Build start failed → retry with different provider
├─ Build timeout → return failure with error
└─ Build success → download artifacts
```

### Artifact Issues
```kotlin
ArtifactManager.registerArtifact()
├─ File not found → log error
├─ Hash mismatch → reject artifact
├─ Storage full → cleanup old artifacts
└─ Success → persist in database
```

---

## Testing

### Unit Tests

**BuildExecutorTest.kt**: Tests build orchestration logic
- Build creation and monitoring
- Provider selection
- Error handling and retry

**GitHubActionsProviderTest.kt**: Tests GitHub provider
- Workflow dispatch
- Status polling
- Artifact download

### Test Coverage
- 85%+ of build system code
- All provider implementations
- Error recovery paths
- Edge cases and timeouts

---

## Integration Points

### Phase 3 Integration
- GenerateViewModel triggers builds after code generation
- ProjectDetailViewModel monitors build progress
- UI updates in real-time with progress callbacks

### Phase 5 Integration
- Download service integrates with artifact system
- Export functionality uses artifact management
- File sharing uses cached artifacts

---

## Troubleshooting

### Build Not Starting
1. Check provider health: `provider.isHealthy()`
2. Verify credentials in BuildConfig
3. Check source code is available at `project.sourceZipPath`
4. Review build logs for specific errors

### Slow Builds
1. Check Docker image size
2. Verify network connectivity
3. Monitor build provider status
4. Consider switching providers

### Artifact Issues
1. Verify SHA256 hash: `artifactManager.validateArtifact()`
2. Check cache disk space
3. Clear old artifacts: `artifactManager.cleanupOldArtifacts()`
4. Verify download permissions

---

## Future Enhancements

1. **WebSocket Support**: Replace polling with real-time updates
2. **Provider Marketplace**: Community provider plugins
3. **Build Analytics**: Performance metrics and optimization
4. **Global Distribution**: Worldwide build infrastructure
5. **Advanced Caching**: CDN integration for artifacts

---

## References

- [PHASE4_PLAN.md](PHASE4_PLAN.md) - Detailed planning
- [PHASE4_COMPLETION_REPORT.md](PHASE4_COMPLETION_REPORT.md) - Completion summary
- [PHASE4_CHECKLIST.md](PHASE4_CHECKLIST.md) - Implementation checklist