# Automation & CI/CD Overview

This document provides a high-level overview of the GitHub Actions CI/CD automation for AIAPKBuilder.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                   GitHub Actions CI/CD Pipeline                 │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌───────────────────┐
│   Push to main   │────────▶│   Main CI Workflow │
│   or PR opened   │         │ (android-ci.yml)   │
└──────────────────┘         └───────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
            ┌───────────────┐ ┌──────────────┐ ┌─────────────┐
            │  JDK 17 Build │ │ JDK 11 Build │ │  Run Tests  │
            └───────────────┘ └──────────────┘ └─────────────┘
                    │                 │                 │
                    └─────────────────┼─────────────────┘
                                      ▼
                    ┌─────────────────────────────────┐
                    │ Generate Coverage + Lint + APK  │
                    │ Upload to Codecov & Artifacts   │
                    └─────────────────────────────────┘

┌──────────────────┐         ┌──────────────────────┐
│  Git tag v*.*.* │────────▶│ Release Workflow      │
│  (e.g., v1.0.0)│         │ (release.yml)         │
└──────────────────┘         └──────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                    ▼
         ┌──────────────────┐           ┌──────────────────────┐
         │ Build signed AAB │           │ Create GitHub Release│
         │ Upload to Assets │           │ Attach AAB to Release│
         └──────────────────┘           └──────────────────────┘

┌──────────────────┐         ┌──────────────────────────┐
│  Manual Dispatch │────────▶│ Instrumented Tests       │
│  (or daily 2 AM) │         │ (instrumented-tests.yml) │
└──────────────────┘         └──────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
           ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
           │ API Level 30 │  │ API Level 35 │  │ Upload Test  │
           │  Emulator    │  │   Emulator   │  │ Reports      │
           └──────────────┘  └──────────────┘  └──────────────┘

┌──────────────────┐         ┌──────────────────────────┐
│  Manual Dispatch │────────▶│ Play Store Deploy        │
│  + Select Track  │         │ (playstore-deploy.yml)   │
└──────────────────┘         └──────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                    ▼
         ┌──────────────────┐           ┌──────────────────────┐
         │ Build signed AAB │           │ Upload to Play Store │
         │ from Git tag     │           │ (alpha/beta/prod)    │
         └──────────────────┘           └──────────────────────┘
```

---

## Workflows at a Glance

### 1. **Main CI Workflow** (`android-ci.yml`)
**When**: Push to `main`/`master` or PR opened  
**Environment**: Ubuntu (matrix: JDK 17 & JDK 11)  
**Steps**:
- Checkout code
- Setup JDK (matrix: 17, 11)
- Setup Android SDK (API 35)
- Cache Gradle for speed
- Run lint checks
- Run unit tests
- Generate code coverage (JaCoCo)
- Upload coverage to Codecov
- Build debug APK
- Bundle unsigned release AAB
- Upload artifacts (APKs & AABs)

**Artifacts**:
- `apks/` — Debug APK files
- `aabs/` — Unsigned release AABs

**Failure modes**:
- Lint errors → workflow fails
- Unit test failures → workflow fails
- Missing dependencies → workflow fails

---

### 2. **Release Workflow** (`release.yml`)
**When**: Push semver tag (e.g., `v1.0.0`)  
**Environment**: Ubuntu  
**Requirements**: `KEYSTORE_BASE64` secret **must** be set  
**Steps**:
- Checkout the tag
- Setup JDK 17
- Setup Android SDK (API 35)
- Cache Gradle
- Decode keystore from `KEYSTORE_BASE64`
- Build **signed** release AAB with Gradle
- Create GitHub Release
- Upload AAB to Release assets
- Save AAB as workflow artifact

**Failure modes**:
- Missing keystore secret → **workflow fails**
- Invalid Base64 encoding → **workflow fails**
- Signing password incorrect → **workflow fails**
- Build errors → **workflow fails**

**Security**:
- Keystore is **never** stored in repo
- Decoded keystore is **ephemeral** (deleted after workflow)
- Secrets are **masked** in logs

---

### 3. **Instrumentation Tests** (`instrumented-tests.yml`)
**When**: Push/PR to `main`/`master`, manual dispatch, or daily at 2 AM UTC  
**Environment**: macOS (better emulator support)  
**Matrix**: API 30 & API 35  
**Steps**:
- Checkout code
- Setup JDK 17
- Cache Gradle
- Launch Android emulator (matrix: API 30, 35)
- Run `connectedAndroidTest` (integration/UI tests)
- Upload test reports & screenshots

**Artifacts**:
- `instrumented-test-results-api30/` — Test results for API 30
- `instrumented-test-results-api35/` — Test results for API 35

**Notes**:
- Slower than unit tests (requires emulator)
- Optional in PR (can be skipped for faster feedback)
- Good for catching API-level issues

---

### 4. **Play Store Deployment** (`playstore-deploy.yml`)
**When**: Manual workflow dispatch only  
**Environment**: Ubuntu + Ruby (fastlane)  
**Requirements**: 
- `SERVICE_ACCOUNT_JSON_BASE64` secret (Google Play service account)
- `KEYSTORE_BASE64` + signing secrets  
- App already created in Google Play Console  
**Steps**:
- Checkout specified git tag
- Setup JDK 17
- Setup Android SDK (API 35)
- Cache Gradle
- Decode service account JSON
- Decode keystore
- Build **signed** release AAB
- Use fastlane to upload to Play Store (selected track)
- Save AAB as artifact

**Tracks supported**:
- `internal` — Internal testing (closed track)
- `alpha` — Alpha testing (open or closed)
- `beta` — Beta testing (open or closed)
- `production` — Production release

**Failure modes**:
- Missing secrets → **workflow fails**
- Service account lacks permissions → **workflow fails**
- Invalid track → **workflow fails**
- Play Store quota exceeded → **workflow fails**

**Note**: After upload, release goes through Play Store review process before becoming available to users.

---

## Workflow Interaction

### Typical Release Flow
```
1. Push code to main
   ├─ Main CI runs (tests, lint, coverage)
   ├─ Artifacts generated (APK, unsigned AAB)
   └─ Artifacts downloadable from Actions

2. When ready to release:
   ├─ Create git tag: git tag -a v1.0.0 -m "Release v1.0.0"
   ├─ Push tag: git push origin v1.0.0
   └─ Release workflow triggers

3. Release workflow:
   ├─ Builds signed AAB
   ├─ Creates GitHub Release
   ├─ Attaches AAB to release
   └─ Release is public on GitHub

4. To deploy to Play Store:
   ├─ Go to Actions → "Deploy to Google Play Store"
   ├─ Select track (internal/alpha/beta/production)
   ├─ Enter tag to deploy (e.g., v1.0.0)
   └─ Workflow uploads AAB to Play Store

5. Play Store review:
   ├─ Release is reviewed (15 min - few hours)
   ├─ Approved releases roll out to users
   └─ You can monitor rollout in Play Store Console
```

---

## Secrets Configuration Summary

### Required for Releases
```
KEYSTORE_BASE64           (Base64-encoded JKS file)
KEYSTORE_PASSWORD         (Keystore password)
KEY_ALIAS                 (e.g., "release")
KEY_PASSWORD              (Key password)
```

### Optional for Coverage
```
CODECOV_TOKEN             (Codecov.io token)
```

### Required for Play Store
```
SERVICE_ACCOUNT_JSON_BASE64 (Base64-encoded Google Play service account)
```

### How to Get Secrets
- **KEYSTORE_BASE64**: `keytool -genkey -v ...` → `base64 -w 0 keystore.jks`
- **CODECOV_TOKEN**: Sign up at [codecov.io](https://codecov.io)
- **SERVICE_ACCOUNT_JSON_BASE64**: Create service account in [Google Cloud Console](https://console.cloud.google.com) → download JSON → `base64 -w 0 service-account.json`

---

## Performance & Caching

### Build Time Optimization
- **Gradle cache**: `~/.gradle` cached across runs (saves ~2-3 minutes)
- **Wrapper cache**: `~/.gradle/wrapper` cached (saves ~1 minute)
- **JVM args**: `-Xmx3g` allows parallel task execution
- **Matrix strategy**: JDK 17 and 11 run in parallel (not sequential)

### Typical CI Times
- **Main CI (single JDK)**: 5-8 minutes
- **Main CI (both JDK 17 + 11)**: 5-8 minutes (parallel)
- **Release build**: 4-6 minutes (with cache)
- **Instrumented tests**: 15-20 minutes per API level (slow due to emulator)
- **Play Store upload**: 5-8 minutes

---

## Monitoring & Debugging

### View Workflow Status
1. GitHub repo → **Actions** tab
2. Click workflow name (e.g., "Android CI")
3. Click the run you want to inspect
4. Expand each job/step to see logs

### Check for Failures
- Red ❌ badge = workflow failed
- Green ✅ badge = workflow passed
- Yellow 🟡 badge = workflow in progress
- Gray ⚫ badge = workflow skipped/cancelled

### Download Artifacts
1. Go to completed workflow run
2. Scroll to **Artifacts** section
3. Download APK, AAB, or test reports

### View Code Coverage
- [Codecov.io](https://codecov.io) for trends over time
- Local: `open app/build/reports/coverage/debug/index.html`

---

## Common Automation Patterns

### Pattern 1: Feature Development
```
1. Create feature branch: git checkout -b feature/my-feature
2. Make changes & commit
3. Push: git push origin feature/my-feature
4. Open PR → Main CI runs automatically
5. Review CI logs & coverage
6. Merge PR when CI passes
```

### Pattern 2: Release Management
```
1. Update version in build.gradle.kts (e.g., 1.0.0)
2. Commit: git commit -m "chore: bump version to 1.0.0"
3. Tag: git tag -a v1.0.0 -m "Release 1.0.0"
4. Push: git push origin main && git push origin v1.0.0
5. Release CI builds & creates GitHub Release
6. Download AAB for testing or Play Store upload
```

### Pattern 3: Automated Play Store Rollout
```
1. Push version tag (e.g., v1.0.0)
2. Release workflow builds signed AAB
3. Go to Actions → "Deploy to Google Play Store"
4. Select track: internal → beta → production
5. Deploy to each track in sequence
6. Each track can go through review process
```

---

## Troubleshooting Quick Reference

| Problem | Likely Cause | Solution |
|---------|--------------|----------|
| "No AAB found" | Build failed | Check Gradle errors in logs |
| "KEYSTORE_BASE64 not set" | Secret missing | Add to GitHub Secrets |
| "Invalid Base64" | Bad encoding | Re-encode: `base64 -w 0 file.jks` |
| "Lint failed" | Code style issues | Run `./gradlew lint` locally, fix issues |
| "Tests failed" | Unit test errors | Run `./gradlew test` locally, debug |
| "API 35 not found" | Android SDK issue | Usually auto-installed; check runner logs |
| "Service account invalid" | Wrong JSON or permissions | Re-check service account in Play Console |
| "Play Store upload failed" | Track or permissions issue | Verify release already in Play Console |

---

## Best Practices

1. **Always test locally first**: Run `./gradlew build` before pushing
2. **Use semantic versioning**: Tags should follow `v1.2.3` format
3. **Keep secrets secure**: Never log or share `KEYSTORE_BASE64`, `SERVICE_ACCOUNT_JSON_BASE64`
4. **Monitor CI regularly**: Check Actions tab for failures or slow builds
5. **Use meaningful commit messages**: Helps with CI debugging
6. **Automate gradually**: Add workflows incrementally, test each one
7. **Backup keystore**: Keep `release.jks` in a safe place (NOT in repo)
8. **Review coverage**: Aim for >70% code coverage (monitored via Codecov)

---

## Next Steps

See **SETUP.md** for step-by-step configuration instructions.
