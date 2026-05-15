# CI / GitHub Actions Guide

This document explains how the GitHub Actions workflows in this repository work and how to configure secrets and run local validations.

## Workflows

### `.github/workflows/android-ci.yml` (Main CI)
Runs on push and pull_request to `main`/`master`. Features:
- **Matrix build**: Tests on JDK 17 and JDK 11 for compatibility
- **Checkout**: Full repo with history
- **Android SDK**: Sets up compileSdk 35 and build-tools 35.0.0
- **Gradle cache**: Caches `~/.gradle` for faster builds
- **Lint**: Code quality checks via `lint` task
- **Unit tests**: Runs `test` task
- **Code coverage**: Generates JaCoCo report and uploads to Codecov
- **APK/AAB build**: Assembles debug APK and bundles unsigned AAB
- **Artifacts**: Uploads APKs and AABs to GitHub Actions

### `.github/workflows/release.yml` (Release Build & GitHub Release)
Runs on push of semver tags `v*.*.*` and on manual workflow dispatch. Features:
- **Mandatory signing**: Requires `KEYSTORE_BASE64` secret; will fail without it
- **Release AAB**: Builds a signed AAB using signing secrets
- **GitHub Release**: Creates a release and uploads the AAB as an asset
- **Artifact backup**: Also stores AAB as workflow artifact

### `.github/workflows/instrumented-tests.yml` (Emulator Tests)
Runs on push/PR to `main`/`master`, or manually. Features:
- **Multi-API matrix**: Tests on API 30 and 35
- **Android Emulator**: Uses macOS runner and android-emulator-runner
- **Connected tests**: Runs `connectedAndroidTest` task
- **Results upload**: Stores test reports and screenshots

### `.github/workflows/playstore-deploy.yml` (Play Store Upload)
Manual workflow dispatch only. Features:
- **Track selection**: Choose internal, alpha, beta, or production track
- **Tag checkout**: Deploys a specific git tag
- **Service account**: Decodes service account JSON from secret
- **fastlane upload**: Uses fastlane supply to upload AAB to Play Store
- **Artifact backup**: Stores AAB as workflow artifact

## Required GitHub Secrets (for signing)
- `KEYSTORE_BASE64` — Base64-encoded keystore file (JKS). **Required for release builds.**
- `KEYSTORE_PASSWORD` — Keystore password.
- `KEY_ALIAS` — Key alias inside the keystore.
- `KEY_PASSWORD` — Key password.

## Optional GitHub Secrets
- `CODECOV_TOKEN` — Codecov token for code coverage reporting. If not set, coverage upload will fail silently (non-blocking).
- `SERVICE_ACCOUNT_JSON_BASE64` — Base64-encoded Google Play service account JSON. **Required for Play Store deployment.**

How to create `KEYSTORE_BASE64` locally
1. Create or locate your keystore (JKS). Example to create a keystore:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

2. Encode it to base64 and copy the string to the `KEYSTORE_BASE64` secret in GitHub

```bash
base64 -w 0 release.jks > release.jks.base64
# copy the content of release.jks.base64 into GitHub secret KEYSTORE_BASE64
```

## How to create `SERVICE_ACCOUNT_JSON_BASE64` (for Play Store)
1. Create a Google Play service account in Google Cloud Console:
   - Go to [Google Play Console](https://play.google.com/console) → Settings → API access
   - Create a new service account (or link an existing one)
   - Download the JSON key file

2. Encode it to base64:

```bash
base64 -w 0 service-account.json > service-account.json.base64
# copy the content into GitHub secret SERVICE_ACCOUNT_JSON_BASE64
```

3. Ensure the service account has the "Admin" role in Play Store

Local validation commands

```bash
# make gradle wrapper executable
chmod +x ./gradlew

# Clean + lint
./gradlew clean lint

# Unit tests
./gradlew test

# Unit tests with code coverage (JaCoCo)
./gradlew testDebugUnitTestCoverage
# Coverage report: app/build/reports/coverage/debug/index.html

# Assemble debug APK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Bundle release (unsigned by default locally)
./gradlew bundleRelease
# AAB: app/build/outputs/bundle/release/*.aab

# Build signed release locally (example)
./gradlew bundleRelease -Pandroid.injected.signing.store.file=/path/to/release.jks -Pandroid.injected.signing.store.password=... -Pandroid.injected.signing.key.alias=... -Pandroid.injected.signing.key.password=...

# Run instrumentation tests (requires running emulator or device)
./gradlew connectedAndroidTest
```

## Triggering CI Workflows

### Main CI (android-ci.yml)
Runs automatically on:
- Push to `main` or `master`
- Pull requests to `main` or `master`

No action needed; CI runs automatically.

### Release Build (release.yml)
Triggered by:
- Pushing a semver tag (e.g., `v1.0.0`)
- Manual workflow dispatch via GitHub Actions UI

Create and push a release tag:

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
# This triggers release.yml; creates GitHub Release with signed AAB
```

### Instrumentation Tests (instrumented-tests.yml)
Runs automatically on:
- Push to `main` or `master`
- Pull requests to `main` or `master`
- Daily schedule at 2 AM UTC

Or trigger manually via GitHub Actions UI.

### Play Store Deployment (playstore-deploy.yml)
Manual workflow dispatch only. To deploy:

1. Go to GitHub Actions → "Deploy to Google Play Store"
2. Click "Run workflow"
3. Enter:
   - **Track**: internal / alpha / beta / production
   - **Tag**: Git tag to deploy (e.g., v1.0.0)
4. Click "Run workflow"

The workflow will build the AAB from the specified tag and upload to Play Store.

## Tips & Troubleshooting

### JDK Compatibility Matrix
The main CI runs on both JDK 17 and JDK 11 to ensure compatibility:
- **JDK 17**: Required for AGP 8.5.2 and Kotlin 2.0
- **JDK 11**: Older Java for backward compatibility testing

If tests fail only on JDK 11, you can remove it from the matrix in `.github/workflows/android-ci.yml` (not recommended).

### Gradle Caching
- Gradle caches are stored in `~/.gradle` and persisted across runs
- Cache keys use checksums of `gradle-wrapper.properties` and `build.gradle.kts`
- Caches are invalidated automatically when dependencies change
- To manually invalidate, add `--no-build-cache` to Gradle commands

### Code Coverage
- JaCoCo code coverage reports are generated for unit tests
- Reports are uploaded to Codecov (requires `CODECOV_TOKEN` secret)
- View reports locally: `open app/build/reports/coverage/debug/index.html`
- If Codecov upload fails, the workflow continues (non-blocking)

### Release Signing
- Release builds **require** the `KEYSTORE_BASE64` secret
- If the secret is missing, the release workflow will **fail** (by design, for security)
- Always sign release APKs/AABs before publishing to Play Store
- Do **not** commit keystore files to the repository

### Play Store Uploads
- Requires a Google Play service account with "Admin" role
- The service account JSON must be encoded as Base64 in `SERVICE_ACCOUNT_JSON_BASE64`
- Supports uploading to internal, alpha, beta, or production tracks
- After upload, the release goes through Play Store review process

### Instrumentation Tests
- Runs on macOS runner (faster emulator support via ARM64 virtualization)
- Tests multiple Android API levels (30, 35)
- Requires running emulator; slower than unit tests
- Use for integration tests, UI tests, and database tests

### Debugging CI Failures
1. Check GitHub Actions logs for detailed error messages
2. Reproduce locally using the same `./gradlew` commands
3. For signing errors, verify secrets are correctly base64-encoded
4. For Gradle errors, check `gradle.properties` and `build.gradle.kts` syntax
5. For emulator tests, check emulator logs and test output in artifacts

### Skipping CI for Commits
If you need to skip CI for a particular commit:

```bash
git commit -m "docs: update README [skip ci]"
git push
```

This will skip the android-ci.yml workflow (use cautiously).

## Quick Start Checklist

### 1. Push code to trigger main CI
```bash
git add .
git commit -m "feat: initial setup"
git push origin main
# Main CI (android-ci.yml) runs automatically
```

### 2. (Optional) Create keystore for signed releases
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
base64 -w 0 release.jks > release.jks.base64
```

### 3. Add secrets to GitHub (if signing needed)
- Go to GitHub repo → Settings → Secrets and variables → Actions
- Add:
  - `KEYSTORE_BASE64`: content of `release.jks.base64`
  - `KEYSTORE_PASSWORD`: password you entered
  - `KEY_ALIAS`: `release` (or what you used)
  - `KEY_PASSWORD`: password you entered

### 4. Create a release tag to trigger signed build
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
# Release workflow (release.yml) runs; creates GitHub Release with signed AAB
```

### 5. (Optional) Deploy to Play Store
- Prepare Google Play service account JSON, convert to Base64
- Add `SERVICE_ACCOUNT_JSON_BASE64` secret to GitHub
- Go to GitHub Actions → "Deploy to Google Play Store" → "Run workflow"
- Select track (internal/alpha/beta/production) and tag
- Click "Run workflow"
