# Phase 2: AI Integration & Code Generation Engine 🤖

## Overview
Phase 2 focuses on implementing the AI-powered code generation engine that transforms natural language prompts into complete Android project structures.

## Phase 2 Objectives

### 1. **AI Provider Integration**
Connect to multiple AI providers for prompt processing and code generation.

**Implementation Files:**
- `data/service/AIProviderManager.kt` - Provider abstraction layer
- `data/api/AIApiClient.kt` - Unified API client

**Features:**
- Automatic provider fallback (if primary fails, try secondary)
- Token usage tracking
- Rate limiting
- Response streaming support
- Cost estimation

**Providers:**
```
OpenAI (gpt-4o, gpt-4-turbo)
  ↓
OpenRouter (100+ models, cheaper)
  ↓
Groq (Fast inference: llama3, mixtral)
  ↓
Ollama (100% local/offline)
  ↓
Custom (Self-hosted LLMs)
```

---

### 2. **Prompt Analysis Engine**
Understand user intent and extract requirements from prompts.

**Implementation:**
- `util/PromptAnalyzer.kt` - NLP preprocessing
- `util/PromptTemplates.kt` - Prompt engineering

**Components:**
```kotlin
// Extract app requirements
data class ExtractedRequirements(
    val appName: String,
    val appType: AppType,
    val primaryFeatures: List<String>,
    val secondaryFeatures: List<String>,
    val targetAudience: String,
    val complexity: Complexity, // simple, moderate, complex
    val estimatedScreenCount: Int,
    val databaseNeed: Boolean,
    val apiIntegration: Boolean,
    val offlineSupport: Boolean
)
```

**Prompts:**
```
System: "You are an expert Android app architect..."
User: "Build a weather app"
→ Analysis: {
    appName: "Weather App",
    appType: WEATHER,
    primaryFeatures: ["Current Weather", "Forecast"],
    complexity: "moderate",
    screens: 2,
    needsAPI: true
}
```

---

### 3. **Project Planning Service**
Generate detailed project blueprints from extracted requirements.

**Implementation:**
- `data/service/ProjectPlannerService.kt`
- `util/ProjectBlueprint.kt`

**Output:**
```kotlin
data class ProjectBlueprint(
    val appName: String,
    val packageName: String,
    val screens: List<ScreenBlueprint>,
    val viewModels: List<ViewModelBlueprint>,
    val repositories: List<RepositoryBlueprint>,
    val dataModels: List<DataModelSpec>,
    val dependencies: List<Dependency>,
    val permissions: List<String>,
    val colorScheme: ColorScheme,
    val databaseSchema: DatabaseSchema,
    val apiClients: List<ApiClientSpec>,
    val buildConfig: BuildConfigSpec
)

data class ScreenBlueprint(
    val name: String,
    val route: String,
    val type: ScreenType,
    val components: List<ComponentSpec>,
    val dataBinding: DataBinding,
    val navigation: List<NavigationEdge>
)

data class ComponentSpec(
    val name: String,
    val type: ComponentType, // Button, TextField, List, etc.
    val properties: Map<String, String>,
    val eventHandlers: List<EventHandler>
)
```

---

### 4. **Code Generation Engine**
Transform blueprints into production-ready Kotlin/Compose code.

**Implementation Files:**
- `util/CodeGenerator.kt` - Main code generation orchestrator
- `util/generators/ComposeScreenGenerator.kt`
- `util/generators/ViewModelGenerator.kt`
- `util/generators/RepositoryGenerator.kt`
- `util/generators/DatabaseGenerator.kt`
- `util/generators/ManifestGenerator.kt`
- `util/generators/GradleGenerator.kt`

**Generation Pipeline:**
```
ProjectBlueprint
    ↓
[CodeGenerator]
    ├─→ Compose Screens → .kt files
    ├─→ ViewModels → .kt files
    ├─→ Repositories → .kt files
    ├─→ Database Models → .kt files
    ├─→ DAOs → .kt files
    ├─→ API Clients → .kt files
    ├─→ Theme → colors.kt, typography.kt
    ├─→ Navigation → AppNavigation.kt
    ├─→ build.gradle.kts
    ├─→ AndroidManifest.xml
    ├─→ strings.xml (i18n)
    └─→ Generated ZIP
```

**Code Quality:**
- Proper Kotlin conventions
- Material3 design compliance
- Compose best practices
- Type safety
- Coroutine patterns
- Error handling

---

### 5. **Template-Based Generation**
Use pre-built templates as starting points and customize.

**Implementation:**
- `util/TemplateProcessor.kt` - Template variable substitution
- `util/TemplateCache.kt` - Template caching

**Template Variables:**
```
${APP_NAME}
${PACKAGE_NAME}
${SCREEN_NAME}
${VIEWMODEL_NAME}
${DATA_MODELS}
${API_ENDPOINTS}
${DATABASE_TABLES}
${THEME_COLORS}
${FEATURES}
```

**Example Template:**
```kotlin
// templates/screen_list.kt
package ${PACKAGE_NAME}.ui.screens

@Composable
fun ${SCREEN_NAME}Screen(
    viewModel: ${VIEWMODEL_NAME} = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    
    LazyColumn {
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = { Text(item.description) }
            )
        }
    }
}
```

---

### 6. **Response Processing & Validation**
Parse AI responses and ensure code quality.

**Implementation:**
- `util/ResponseParser.kt` - JSON/structured response parsing
- `util/CodeValidator.kt` - Syntax and structure validation

**Validation Checks:**
```kotlin
✓ Valid Kotlin syntax
✓ Required imports present
✓ Proper package structure
✓ No undefined references
✓ Correct Compose usage
✓ Hilt annotations present (if using DI)
✓ Coroutine scope handling
✓ Resource cleanup
✓ Error handling present
```

---

### 7. **Prompt Streaming Support**
Enable real-time display of generation progress.

**Implementation:**
- `util/StreamingResponseHandler.kt`
- `data/model/StreamingEvent.kt`

**Features:**
```
UI receives incremental updates:
[10%] Analyzing requirements...
[20%] Planning project structure...
[40%] Generating screens...
[60%] Creating ViewModels...
[80%] Setting up database...
[100%] Finalizing configuration...
```

**Event Types:**
```kotlin
sealed class GenerationEvent {
    data class Progress(val percent: Int, val message: String) : GenerationEvent()
    data class ScreenGenerated(val screenName: String) : GenerationEvent()
    data class CodeSegment(val code: String) : GenerationEvent()
    data class Error(val message: String) : GenerationEvent()
    object Complete : GenerationEvent()
}
```

---

## Implementation Checklist

### Step 1: AI Provider Manager
- [ ] Create AIProviderManager interface
- [ ] Implement OpenAI adapter
- [ ] Implement OpenRouter adapter
- [ ] Implement Groq adapter
- [ ] Implement Ollama support
- [ ] Add fallback logic
- [ ] Add rate limiting
- [ ] Add token tracking

### Step 2: Prompt Analysis
- [ ] Create PromptAnalyzer
- [ ] Extract app requirements
- [ ] Classify app type
- [ ] Parse features
- [ ] Identify complexity
- [ ] Estimate screens
- [ ] Create ExtractedRequirements

### Step 3: Project Planning
- [ ] Create ProjectPlannerService
- [ ] Design database schema from requirements
- [ ] Plan API integrations
- [ ] Define screen hierarchy
- [ ] Create ViewModels list
- [ ] Define data models
- [ ] Plan navigation flow

### Step 4: Code Generators
- [ ] Create ComposeScreenGenerator
- [ ] Create ViewModelGenerator
- [ ] Create RepositoryGenerator
- [ ] Create DatabaseGenerator
- [ ] Create ManifestGenerator
- [ ] Create GradleGenerator
- [ ] Create NavigationGenerator
- [ ] Create ThemeGenerator

### Step 5: Template System
- [ ] Create TemplateProcessor
- [ ] Build template library
- [ ] Add variable substitution
- [ ] Create template cache
- [ ] Test template rendering

### Step 6: Response Processing
- [ ] Create ResponseParser
- [ ] Implement CodeValidator
- [ ] Add syntax checking
- [ ] Create error recovery
- [ ] Add retry logic

### Step 7: Streaming Support
- [ ] Create StreamingResponseHandler
- [ ] Define event types
- [ ] Implement progress tracking
- [ ] Add UI event flow
- [ ] Create demo animation

---

## Data Flow Example

```
User Input: "Create a weather app"
    ↓
[PromptAnalyzer]
    ↓
ExtractedRequirements {
    appName: "Weather App",
    appType: WEATHER,
    features: ["Current Weather", "Forecast"],
    screens: 2,
    needsAPI: true,
    complexity: MODERATE
}
    ↓
[ProjectPlannerService]
    ↓
ProjectBlueprint {
    screens: [HomeScreen, DetailsScreen],
    viewModels: [WeatherViewModel],
    repositories: [WeatherRepository],
    dataModels: [Weather, Forecast],
    dependencies: [Retrofit, Room],
    apiEndpoints: ["https://api.weather.gov"]
}
    ↓
[CodeGenerator] (generates files)
    ├─ HomeScreen.kt
    ├─ DetailsScreen.kt
    ├─ WeatherViewModel.kt
    ├─ WeatherRepository.kt
    ├─ Weather.kt
    ├─ WeatherApi.kt
    ├─ WeatherDatabase.kt
    ├─ build.gradle.kts
    └─ AndroidManifest.xml
    ↓
[ProjectPackager]
    ↓
Generated APK + Source ZIP
```

---

## AI Prompt Examples

### Weather App
```
System: "You are an expert Android developer..."
User: "Build a weather app that shows current weather and 5-day forecast"

AI Response:
{
  "app": {
    "name": "WeatherPro",
    "type": "weather",
    "screens": [
      {
        "name": "HomeScreen",
        "components": ["TopAppBar", "CurrentWeatherCard", "ForecastList"],
        "data": ["temperature", "humidity", "windSpeed"]
      },
      {
        "name": "DetailsScreen",
        "components": ["DetailedWeatherChart", "RadarMap"]
      }
    ],
    "api": "https://api.openweathermap.org",
    "database": {
      "tables": ["weather_cache", "locations"]
    }
  }
}
```

### E-Commerce App
```
System: "You are an expert Android developer..."
User: "Create an e-commerce app with product listing, shopping cart, and checkout"

AI Response:
{
  "app": {
    "name": "ShopHub",
    "type": "ecommerce",
    "screens": [
      {
        "name": "ProductListScreen",
        "components": ["TopAppBar", "FilterChips", "ProductGrid"]
      },
      {
        "name": "ProductDetailScreen",
        "components": ["ProductImages", "PriceInfo", "RatingBar", "AddToCartButton"]
      },
      {
        "name": "CartScreen",
        "components": ["CartItems", "PriceSummary", "CheckoutButton"]
      },
      {
        "name": "CheckoutScreen",
        "components": ["AddressForm", "PaymentMethod", "ConfirmButton"]
      }
    ],
    "features": ["ProductSearch", "Wishlist", "Reviews", "Orders"],
    "api": "https://api.commerce.example.com",
    "payment": "Stripe"
  }
}
```

---

## Performance Considerations

1. **Response Caching**: Cache generated plans for 1 hour
2. **Token Optimization**: Compress prompts to reduce token usage
3. **Parallel Generation**: Generate multiple screens concurrently
4. **Incremental Building**: Build/test components incrementally
5. **Lazy Loading**: Load templates on-demand

---

## Error Handling

```kotlin
try {
    val extracted = promptAnalyzer.analyze(userPrompt)
    val blueprint = projectPlanner.createBlueprint(extracted)
    val generated = codeGenerator.generate(blueprint)
    val validated = codeValidator.validate(generated)
    return validated
} catch (e: InvalidPromptException) {
    // Handle unclear or invalid prompts
    return askForClarification(e.message)
} catch (e: AIProviderException) {
    // Fallback to next provider
    return generateWithFallback(userPrompt)
} catch (e: GenerationException) {
    // Log and return partial results
    return returnPartialGeneration(e)
}
```

---

## Testing Strategy

### Unit Tests
- PromptAnalyzer: Parse various prompt formats
- ResponseParser: Handle malformed responses
- CodeValidator: Detect invalid code
- TemplateProcessor: Variable substitution

### Integration Tests
- AI Provider: Mock API responses
- Code Generation: Compare against templates
- Full Pipeline: End-to-end generation

### UI Tests
- Progress display
- Error messages
- Result preview

---

## Success Criteria for Phase 2

✅ Prompt analysis extracts 90%+ of intended requirements
✅ Generated code compiles without errors
✅ Generated code follows Kotlin conventions
✅ All templates render correctly
✅ Response parsing handles 95%+ of valid responses
✅ AI provider fallback works seamlessly
✅ Streaming updates display smoothly
✅ Code generation completes in <30 seconds
✅ Unit test coverage >80%

---

## Timeline Estimate

- **Week 1**: AI provider integration + prompt analysis
- **Week 2**: Project planning + code generators
- **Week 3**: Template system + response processing
- **Week 4**: Streaming support + testing + optimization

---

## Dependencies

See Phase 1 for base dependencies. Additional:
- `org.jetbrains.kotlinx:kotlinx-serialization-json` (structured responses)
- `okhttp3:okhttp-sse` (Server-Sent Events for streaming)
- Custom LLM SDKs (Groq, OpenRouter, Ollama)

---

## Related Files from Phase 1

- `CodeGenerationService.kt` - Interface to implement
- `ProjectGenerationUtils.kt` - Utility functions
- `TemplateData.kt` - Template definitions
- `ErrorHandling.kt` - Error types
- `ProjectRepository.kt` - Data persistence

---

## Next Phase (Phase 3)

**Project Generation UI**
- GenerateScreen composable
- AI prompt builder interface
- Feature selection UI
- Real-time generation preview
- Error display and retry

