# AI APK Builder - Getting Started Guide 🚀

## Quick Start (5 minutes)

### 1. Clone the Repository
```bash
git clone https://github.com/aiapkbuilder/aiapkbuilder.git
cd AIAPKBuilder
```

### 2. Set Up Environment
```bash
# Android Studio: Open project and let Gradle sync
# Or use Gradle directly:
./gradlew clean build
```

### 3. Get API Keys (Optional)
Visit these sites to get free API keys:
- **OpenAI**: https://platform.openai.com/api-keys (free $5 credit)
- **OpenRouter**: https://openrouter.ai (free tier)
- **Groq**: https://console.groq.com (free tier)

### 4. Configure App
```
1. Open AIAPKBuilder app
2. Go to Settings
3. Paste your AI API key
4. Choose build provider (GitHub Actions recommended)
5. Save settings
```

### 5. Create Your First App
```
1. Tap "Generate New App"
2. Type: "Create a weather app"
3. Select features
4. Tap "Generate"
5. Watch the magic happen!
```

---

## Project Structure

```
AIAPKBuilder/
├── build.gradle.kts              # Root build config
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml         # Dependency versions
│
├── app/
│   ├── build.gradle.kts           # App-level build
│   ├── proguard-rules.pro         # ProGuard config
│   │
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/aiapkbuilder/app/
│       │   │   ├── AIAPKBuilderApp.kt           # App initialization
│       │   │   ├── MainActivity.kt              # Main activity
│       │   │   │
│       │   │   ├── data/
│       │   │   │   ├── api/                     # API clients
│       │   │   │   │   ├── AIApiService.kt      # OpenAI, GitHub, Codemagic
│       │   │   │   │   └── CodemagicApiService.kt
│       │   │   │   ├── local/                   # Room database
│       │   │   │   │   └── AppDatabase.kt
│       │   │   │   ├── model/                   # Data models
│       │   │   │   │   ├── Models.kt            # Main entities
│       │   │   │   │   └── TemplateData.kt      # Templates
│       │   │   │   ├── repository/              # Repositories
│       │   │   │   │   ├── ProjectRepository.kt
│       │   │   │   │   ├── SettingsRepository.kt
│       │   │   │   │   └── TemplateRepository.kt
│       │   │   │   └── service/                 # Business logic
│       │   │   │       ├── CodeGenerationService.kt
│       │   │   │       ├── BuildProviderService.kt
│       │   │   │       └── BuildWorker.kt
│       │   │   │
│       │   │   ├── di/                          # Dependency injection
│       │   │   │   └── AppModule.kt
│       │   │   │
│       │   │   ├── ui/
│       │   │   │   ├── navigation/
│       │   │   │   │   └── AppNavigation.kt
│       │   │   │   ├── screens/                 # Screen composables
│       │   │   │   │   ├── HomeScreen.kt
│       │   │   │   │   ├── GenerateScreen.kt    # AI generation
│       │   │   │   │   ├── ProjectsScreen.kt
│       │   │   │   │   ├── ProjectDetailScreen.kt
│       │   │   │   │   ├── BuildLogScreen.kt
│       │   │   │   │   ├── SettingsScreen.kt
│       │   │   │   │   └── TemplatesScreen.kt
│       │   │   │   └── theme/
│       │   │   │       ├── Theme.kt
│       │   │   │       └── Type.kt
│       │   │   │
│       │   │   ├── viewmodel/                   # MVVM ViewModels
│       │   │   │   ├── HomeViewModel.kt
│       │   │   │   ├── GenerateViewModel.kt
│       │   │   │   ├── ProjectsViewModel.kt
│       │   │   │   ├── ProjectDetailViewModel.kt
│       │   │   │   ├── BuildLogViewModel.kt
│       │   │   │   └── SettingsViewModel.kt
│       │   │   │
│       │   │   └── util/                        # Utilities
│       │   │       ├── AICodeGenerator.kt       # Code generation
│       │   │       ├── ErrorHandling.kt
│       │   │       ├── ProjectGenerationUtils.kt
│       │   │       ├── AppLogger.kt
│       │   │       └── Extensions.kt
│       │   │
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           ├── data_extraction_rules.xml
│       │           └── file_paths.xml
│       │
│       ├── test/                                # Unit tests
│       │   └── java/.../GenerateViewModelTest.kt
│       │
│       └── androidTest/                         # Instrumentation tests
│           └── java/.../MainActivityTest.kt
│
├── Documentation/
│   ├── PHASE1_IMPLEMENTATION.md                 # Phase 1 details
│   ├── PHASE2_PLAN.md                          # Phase 2 roadmap
│   ├── COMPLETE_ROADMAP.md                     # Full 6-phase roadmap
│   ├── ARCHITECTURE.md                         # Architecture overview
│   ├── CONTRIBUTING.md                         # Contribution guidelines
│   └── API_DOCUMENTATION.md                    # API reference
│
├── README.md                                    # Project overview
├── LICENSE                                      # MIT License
└── .gitignore
```

---

## Key Files to Know

### Core Data Layer
- **Models.kt** - All data entities (10 classes)
- **AppDatabase.kt** - Room database with 6 DAOs
- **ProjectRepository.kt** - Main data access layer

### Services & Business Logic
- **CodeGenerationService.kt** - AI code generation interface (Phase 2)
- **BuildProviderService.kt** - Build system interface (Phase 4)
- **BuildWorker.kt** - Background build execution

### UI & Navigation
- **AppNavigation.kt** - Compose navigation setup
- **GenerateScreen.kt** - Main AI generation interface
- **SettingsScreen.kt** - App configuration

### Utilities
- **ErrorHandling.kt** - Error classification
- **ProjectGenerationUtils.kt** - Helper functions
- **AppLogger.kt** - Logging infrastructure
- **Extensions.kt** - Kotlin extension functions

---

## Development Workflow

### 1. Setting Up for Development
```bash
# Clone repository
git clone https://github.com/aiapkbuilder/aiapkbuilder.git

# Open in Android Studio
# File → Open → AIAPKBuilder

# Sync Gradle (automatic)
./gradlew clean build

# Run on emulator or device
./gradlew installDebug
```

### 2. Building the Debug APK
```bash
./gradlew assembleDebug
# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### 3. Building the Release APK
```bash
# Create release keystore (first time only)
keytool -genkey -v -keystore ~/.android/release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 -alias release

# Build signed release APK
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release.apk
```

### 4. Running Tests
```bash
# Unit tests
./gradlew test

# Instrumentation tests (requires emulator/device)
./gradlew connectedAndroidTest

# With coverage
./gradlew testDebugUnitTestCoverage
```

### 5. Code Analysis
```bash
# Detekt (static analysis)
./gradlew detekt

# Lint
./gradlew lint

# Build with analysis
./gradlew build --scan
```

---

## Architecture Overview

### MVVM Pattern
```
UI (Composable Screens)
    ↓
ViewModel (StateFlow<UiState>)
    ↓
Repository (Data Access)
    ↓
Data Layer (Room, Retrofit, DataStore)
```

### Dependency Injection (Hilt)
```
@HiltAndroidApp (Application)
    ↓
@HiltAndroidActivity / @AndroidEntryPoint
    ↓
@Inject lateinit var repository: ProjectRepository
```

### Data Flow Example
```
User enters prompt
    ↓
GenerateScreen calls GenerateViewModel.generateProject()
    ↓
ViewModel calls ProjectRepository.generateAndCreateProject()
    ↓
Repository calls CodeGenerationService (Phase 2)
    ↓
AI generates project plan
    ↓
Code generators create files (Phase 2)
    ↓
Project saved to Room database
    ↓
UI updates with progress
    ↓
Build initiated via BuildWorker
```

---

## Important Classes Reference

### Models
```kotlin
AppProject          // Main project entity
BuildJob            // Build execution record
ProjectTemplate     // Pre-built app template
GeneratedProjectPlan // AI-generated project spec
BuildStatus         // enum: PENDING, GENERATING, BUILDING, SUCCESS, FAILED
AppType             // enum: 18 app types
BuildProvider       // enum: 5 providers (GitHub, Codemagic, Docker, etc.)
AIProvider          // enum: 5 AI providers (OpenAI, Groq, Ollama, etc.)
```

### Repositories
```kotlin
ProjectRepository    // Projects CRUD + generation
SettingsRepository   // User settings & config
TemplateRepository   // Templates management
```

### Key Enums
```kotlin
enum class BuildStatus {
    PENDING, GENERATING, BUILDING, SUCCESS, FAILED, CANCELLED
}

enum class AppType (18 types) {
    CALCULATOR, NOTES, CHAT, ECOMMERCE, DELIVERY, TAXI, SCHOOL,
    AI_ASSISTANT, FINANCE, PRODUCTIVITY, PORTFOLIO, BUSINESS,
    STREAMING, DASHBOARD, WEATHER, FITNESS, SOCIAL, CUSTOM
}

enum class BuildProvider {
    LOCAL, GITHUB_ACTIONS, CODEMAGIC, DOCKER, SELF_HOSTED, COMMUNITY
}

enum class AIProvider {
    OPENAI, OPENROUTER, OLLAMA, GROQ, CUSTOM
}
```

---

## Common Tasks

### Adding a New Screen
1. Create `YourNewScreen.kt` in `ui/screens/`
2. Create `YourNewViewModel.kt` in `viewmodel/`
3. Add route to `AppNavigation.kt`
4. Add ViewModel dependency in `AppModule.kt`

### Adding a New Database Entity
1. Create entity class in `data/model/Models.kt`
2. Create DAO interface in `data/local/AppDatabase.kt`
3. Add abstract function to AppDatabase
4. Create auto-migration if needed
5. Create repository if needed

### Adding a New API Service
1. Create API service interface in `data/api/`
2. Create data classes for request/response
3. Add Retrofit service to `AppModule.kt`
4. Create repository wrapper if needed

### Accessing Data
```kotlin
// In ViewModel
private val repository: ProjectRepository by inject()

// Observe projects
viewModelScope.launch {
    repository.getAllProjects().collect { projects ->
        _uiState.value = _uiState.value.copy(projects = projects)
    }
}

// Insert project
viewModelScope.launch {
    repository.projectDao.insertProject(project)
}
```

---

## Debugging Tips

### Enable Verbose Logging
```kotlin
// In AppLogger.kt
AppLogger.d("Debug message", "TAG")
AppLogger.i("Info message", "TAG")
AppLogger.e("Error message", throwable = exception, tag = "TAG")
```

### View Database Contents
```
Android Studio → Device File Explorer
→ /data/data/com.aiapkbuilder.app/databases/
→ aiapkbuilder.db (use SQLite viewer)
```

### View Build Logs
```
Check app/build/logs/ for detailed build output
```

### Inspect Network Requests
```
Android Studio → Logcat with filter "retrofit"
Or enable OkHttp logging interceptor in AppModule
```

---

## Common Issues & Solutions

### Issue: Gradle Sync Fails
**Solution:**
```bash
# Clear gradle cache
rm -rf ~/.gradle

# Re-sync
./gradlew clean build --refresh-dependencies
```

### Issue: Database Migration Error
**Solution:**
```
1. Clear app data: adb shell pm clear com.aiapkbuilder.app
2. Re-run app (creates fresh database)
3. Check database version in AppDatabase.kt
```

### Issue: Hilt DependencyResolve Error
**Solution:**
```
1. Ensure all @Inject constructors have parameters with providers
2. Check AppModule.kt has all necessary @Provides
3. Run: ./gradlew --refresh-dependencies clean build
```

### Issue: API Key Not Working
**Solution:**
```
1. Verify key is correct in Settings
2. Check internet connection
3. Verify API quota/limits on provider dashboard
4. Try fallback to different provider
```

---

## Testing Guide

### Unit Testing Example
```kotlin
@Test
fun testProjectGeneration() {
    val request = GenerationRequest("Build a calculator")
    val result = repository.generateAndCreateProject(request, BuildProvider.LOCAL)
    
    assertNotNull(result)
    assertTrue(result.isNotEmpty())
}
```

### Instrumentation Testing Example
```kotlin
@Test
fun testGenerateScreenUI() {
    composeRule.setContent {
        GenerateScreen()
    }
    
    composeRule
        .onNodeWithTag("prompt_input")
        .performTextInput("Build a weather app")
    
    composeRule
        .onNodeWithText("Generate")
        .performClick()
}
```

---

## Performance Tips

1. **Use DataStore for settings** - Not SharedPreferences
2. **Cache API responses** - 1 hour TTL
3. **Lazy load templates** - Load on-demand
4. **Use Flow for reactive updates** - Not LiveData
5. **Batch database operations** - Use @Transaction
6. **Compress code generation** - Cache results

---

## Security Best Practices

1. **Never hardcode API keys** - Use DataStore
2. **Validate all user input** - Use ErrorHandling
3. **Sanitize file paths** - Use FileProvider
4. **Use HTTPS only** - Retrofit enforces this
5. **Encrypt sensitive data** - Consider EncryptedSharedPreferences
6. **Request permissions properly** - Use Manifest + Runtime

---

## Contributing to the Project

### 1. Fork & Clone
```bash
git clone https://github.com/YOUR_USERNAME/aiapkbuilder.git
cd aiapkbuilder
git remote add upstream https://github.com/aiapkbuilder/aiapkbuilder.git
```

### 2. Create Feature Branch
```bash
git checkout -b feature/my-feature
```

### 3. Make Changes & Commit
```bash
# Follow commit message format:
# [FEATURE/BUG/DOCS] Description
git commit -m "[FEATURE] Add support for Ollama AI provider"
```

### 4. Push & Create PR
```bash
git push origin feature/my-feature
# Create Pull Request on GitHub
```

### 5. Code Review
- Respond to review comments
- Update code if needed
- Maintainers will merge when approved

---

## Resources & Links

- **GitHub Repository**: https://github.com/aiapkbuilder/aiapkbuilder
- **Documentation**: See `/Documentation` folder
- **Issue Tracker**: https://github.com/aiapkbuilder/aiapkbuilder/issues
- **Discussions**: https://github.com/aiapkbuilder/aiapkbuilder/discussions
- **Android Docs**: https://developer.android.com/docs
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Hilt Docs**: https://dagger.dev/hilt
- **Room Database**: https://developer.android.com/training/data-storage/room

---

## FAQ

**Q: Do I need an API key to use the app?**
A: Optional. You can try with free tier or use Ollama for completely local/offline usage.

**Q: Can I build this offline?**
A: Yes, if using Ollama. All other providers require internet.

**Q: What's the minimum Android version?**
A: API 26 (Android 8.0)

**Q: How long does generation take?**
A: Usually 10-30 seconds depending on project complexity.

**Q: Can I modify generated code?**
A: Yes! Download source ZIP and edit in Android Studio.

**Q: Is this app open source?**
A: Yes! MIT License - completely free and open.

**Q: How can I contribute?**
A: See Contributing section above. PRs welcome!

---

**Happy Building! 🚀**

For questions or support, open an issue on GitHub or join our Discord community.
