# 🚀 AI APK Builder

**Generate Android apps from natural language prompts. Describe your idea — get a working APK.**

> 🎉 **100% FREE • NO ADS • NO SIGN-IN • NO SUBSCRIPTIONS • UNLIMITED USAGE**
> 
> [Read our Free Forever Policy →](FREE_FOREVER_POLICY.md)

---

## ✨ Features

- AI-Powered Generation — Describe any app idea in plain language
- Multiple Build Providers — GitHub Actions, Codemagic, Docker, Self-Hosted, Community
- 18+ App Templates — Calculator, Chat, E-Commerce, Taxi, AI Assistant, and more
- APK + Source Export — Download the compiled APK or full source ZIP
- Dark/Light Mode — Full Material You support
- Your Keys, Your Data — Bring your own AI API key; runs fully offline or self-hosted
- Build Log Viewer — Real-time terminal-style build output

---

## Requirements

- Android 8.0+ (API 26)
- Internet connection (for AI generation and cloud builds)

---

## Quick Start

### Option 1 — Build from Source
```bash
git clone https://github.com/YOUR_USERNAME/AIAPKBuilder.git
cd AIAPKBuilder
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### Option 2 — GitHub Actions
Push to main branch; the CI/CD workflow automatically builds and uploads the APK as an artifact.

---

## AI Providers Supported

| Provider | Models | Notes |
|----------|--------|-------|
| OpenAI | gpt-4o, gpt-4-turbo | Requires API key |
| OpenRouter | 100+ models | Cheaper alternatives |
| Groq | llama3, mixtral | Very fast inference |
| Ollama | llama3, phi3, gemma2 | 100% local / offline |
| Custom | Any OpenAI-compatible API | Self-hosted LLMs |

---

## Build Providers

| Provider | Speed | Cost | Requirements |
|----------|-------|------|--------------|
| GitHub Actions | ~5 min | Free (2000 min/month) | GitHub token |
| Codemagic | ~3 min | Free tier available | API key |
| Docker | ~4 min | Your server costs | Docker endpoint |
| Self-Hosted | Variable | Free | Your own build server |
| Community | ~10 min | Free | Internet |

---

## Architecture

```
app/
├── data/
│   ├── api/          # Retrofit interfaces (OpenAI, GitHub, Codemagic)
│   ├── local/        # Room database, DAOs
│   ├── model/        # Data classes and enums
│   ├── repository/   # Single source of truth
│   └── service/      # Background build workers (WorkManager)
├── di/               # Hilt dependency injection modules
├── ui/
│   ├── navigation/   # Compose NavHost setup
│   ├── screens/      # All screen composables
│   └── theme/        # Material3 theme, colors, typography
├── util/             # AI code generator
└── viewmodel/        # MVVM ViewModels with StateFlow
```

**Stack:** Kotlin 2.0 + Jetpack Compose + Material3 + Hilt + Room + Retrofit + WorkManager

---

---

## 🎯 Free Forever Commitment

This app is **permanently free** and will **always remain free**:

- ✅ **No subscription fees** - Never charge for core features
- ✅ **No ads** - Completely ad-free experience
- ✅ **No sign-in required** - Works offline immediately
- ✅ **No Play Store upload** - Distributed via GitHub
- ✅ **Unlimited usage** - Generate as many apps as you want
- ✅ **Open source** - MIT License, full source code available
- ✅ **Community owned** - No corporate control

### How We Guarantee This

1. **MIT License** - Legally guarantees freedom
2. **Community Governance** - Decisions made by users and contributors
3. **Automated Enforcement** - GitHub Actions prevent monetization code
4. **Open Source** - Anyone can fork and continue if needed

**Read more:** [Free Forever Policy](FREE_FOREVER_POLICY.md) | [Enforcement Mechanisms](FREE_FOREVER_ENFORCEMENT.md)

---

## License

**MIT License** - Free software with no restrictions

This means:
- ✅ Use commercially
- ✅ Modify as needed
- ✅ Distribute freely
- ✅ Personal or enterprise use

[Full License Text →](LICENSE)
