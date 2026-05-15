# GitHub Actions Setup Guide

This guide walks you through setting up GitHub Actions CI/CD for the AIAPKBuilder project, including building, testing, code coverage, and Play Store deployment.

## Table of Contents
1. [Initial Setup](#initial-setup)
2. [Local Validation](#local-validation)
3. [GitHub Secrets Configuration](#github-secrets-configuration)
4. [Triggering Workflows](#triggering-workflows)
5. [Monitoring Builds](#monitoring-builds)
6. [Troubleshooting](#troubleshooting)

---

## Initial Setup

### 1. Push the Workflow Files
The workflows have been created in `.github/workflows/`:
- `android-ci.yml` — Main CI (lint, tests, APK/AAB build)
- `release.yml` — Release builds and GitHub releases
- `instrumented-tests.yml` — Emulator-based integration tests
- `playstore-deploy.yml` — Play Store deployment

To activate these workflows:

```bash
git add .github/workflows/ gradle.properties CI.md SETUP.md app/build.gradle.kts
git commit -m "ci: add comprehensive GitHub Actions workflows"
git push origin main
```

GitHub will automatically detect the workflows and enable them.

### 2. Verify Workflows Are Active
1. Go to your GitHub repository
2. Click the **Actions** tab
3. You should see all four workflows listed
4. The main CI should run automatically on the push

---

## Local Validation

Before pushing to GitHub, validate everything works locally:

### Prerequisites
```bash
# Make gradle wrapper executable
chmod +x ./gradlew

# Verify Java 17 is installed
java -version
# Should output: openjdk version "17.x.x" or similar
```

### Run Local Tests

```bash
# Clean build
./gradlew clean

# Lint
./gradlew lint

# Unit tests
./gradlew test

# Code coverage report (generates HTML)
./gradlew testDebugUnitTestCoverage
# Open report: open app/build/reports/coverage/debug/index.html (macOS)
# or: xdg-open app/build/reports/coverage/debug/index.html (Linux)
# or: start app/build/reports/coverage/debug/index.html (Windows)

# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Bundle unsigned release AAB
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

If all these commands succeed locally, the GitHub Actions workflows should also succeed.

---

## GitHub Secrets Configuration

GitHub Secrets are encrypted environment variables used by workflows. Never commit passwords or keystores to the repo.

### For All Workflows (Optional)
- **CODECOV_TOKEN** — For code coverage reporting
  - Get token from [codecov.io](https://codecov.io)
  - Settings → Secrets and variables → Actions → New repository secret

### For Release Builds (Recommended)
To build signed release APKs/AABs, you need a keystore:

#### Step 1: Create a Keystore (one-time)
```bash
# Generate a new keystore
keytool -genkey -v -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Your Name,O=Your Company,L=City,ST=State,C=CountryCode"

# Or use interactive mode:
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

#### Step 2: Encode to Base64
```bash
# Single line Base64 encoding (important: -w 0 removes line breaks)
base64 -w 0 release.jks > release.jks.base64

# Copy the entire output to clipboard
cat release.jks.base64 | pbcopy  # macOS
cat release.jks.base64 | xclip -selection clipboard  # Linux
# or manually open the file and copy the content
```

#### Step 3: Add to GitHub Secrets
1. Go to GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret** and add:

| Secret Name | Value |
|---|---|
| `KEYSTORE_BASE64` | Content of `release.jks.base64` (long Base64 string) |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `release` (or whatever alias you used) |
| `KEY_PASSWORD` | Your key password |

⚠️ **Security Note**: Never commit `release.jks` to the repository. Keep it locally. The workflow will automatically decode the Base64 and use the keystore.

### For Play Store Deployment (Optional)
To automatically upload releases to Google Play Store:

#### Step 1: Create a Service Account
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing one
3. Go to **APIs & Services** → **Credentials**
4. Create a **Service Account**
5. Add a key (JSON format) and download it
6. Share the service account email with your Google Play Console

#### Step 2: Add to Google Play Console
1. Go to [Google Play Console](https://play.google.com/console)
2. Settings → Users and permissions → Invite user
3. Add the service account email with **Admin** role

#### Step 3: Encode and Add to GitHub Secrets
```bash
# Base64 encode the service account JSON
base64 -w 0 service-account.json > service-account.json.base64

# Copy to clipboard (same as above)
cat service-account.json.base64 | pbcopy
```

Add to GitHub Secrets:

| Secret Name | Value |
|---|---|
| `SERVICE_ACCOUNT_JSON_BASE64` | Content of `service-account.json.base64` |

---

## Triggering Workflows

### 1. Main CI (Automatic)
Runs automatically on every push and PR to `main`/`master`:

```bash
git add .
git commit -m "feat: add new feature"
git push origin main
# CI workflow runs automatically
```

View results: **Actions** tab → **Android CI**

### 2. Instrumentation Tests (Automatic + Scheduled)
Runs automatically on push/PR, or daily at 2 AM UTC.

Can also trigger manually via GitHub Actions UI.

### 3. Release Build (Manual + Tag)
**Option A: Push a Git Tag**
```bash
# Create a semantic version tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# Release workflow builds and creates GitHub Release with AAB
```

**Option B: Manual Trigger**
1. Go to **Actions** tab
2. Select **Release AAB** workflow
3. Click **Run workflow** (if available in workflow file)

View results: **Actions** tab → **Release AAB** or **Releases** tab

### 4. Play Store Deployment (Manual Only)
```bash
# Go to Actions tab → "Deploy to Google Play Store"
# Click "Run workflow"
# Enter track (internal/alpha/beta/production) and git tag (e.g., v1.0.0)
# Click "Run workflow"
```

---

## Monitoring Builds

### Real-Time Logs
1. Go to your GitHub repo → **Actions** tab
2. Click the workflow run you want to monitor
3. Click the job (e.g., "build") to expand logs
4. Scroll through logs to see step-by-step output

### Viewing Artifacts
After a workflow completes:
1. Go to the workflow run details
2. Scroll to **Artifacts** section
3. Download APKs, AABs, or test reports

### GitHub Releases
After a release workflow completes:
1. Go to repo → **Releases** tab
2. Click the release version
3. Download the AAB asset
4. View release notes

### Code Coverage
After main CI completes:
1. Check [Codecov.io](https://codecov.io) for coverage trends
2. Or view locally (if you ran `./gradlew testDebugUnitTestCoverage`):
   - Open `app/build/reports/coverage/debug/index.html`

---

## Troubleshooting

### Common Issues

#### Issue: "Java version not available" or "Java 17 not found"
**Solution**: The `setup-java` action will download JDK 17. This is automatic.

#### Issue: "KEYSTORE_BASE64 secret not set" (Release fails)
**Solution**: 
- Go to Settings → Secrets and variables → Actions
- Add the `KEYSTORE_BASE64` secret
- Or use unsigned builds for CI and sign locally before Play Store upload

#### Issue: "No AAB found" or "APK not generated"
**Solution**:
- Check local build: `./gradlew bundleRelease` or `./gradlew assembleDebug`
- Review Gradle error logs in GitHub Actions
- Ensure `compileSdk = 35` in `app/build.gradle.kts`

#### Issue: "Android SDK not found" or "compileSdk 35 not installed"
**Solution**: The `android-actions/setup-android@v2` action installs API 35 and build-tools automatically. If still failing:
- Check GitHub Actions logs for errors during "Set up Android SDK" step
- Possible: runner out of disk space (rare)

#### Issue: Instrumentation tests fail on emulator
**Solution**:
- Instrumentation tests run on macOS (better emulator support)
- If a test fails, check the artifact (test reports)
- Some UI tests may need `@Ignore` annotations temporarily

#### Issue: Play Store upload fails
**Solution**:
- Verify service account has "Admin" role in Google Play Console
- Check `SERVICE_ACCOUNT_JSON_BASE64` is correctly Base64-encoded
- Ensure the app is already created in Google Play Console
- Check track (internal/alpha/beta/production) is valid for your release

### Viewing Detailed Logs
1. Go to failed workflow run
2. Click the failed job step
3. Expand the step to see full output
4. Look for error messages (usually red text)
5. Google the error message for solutions

### Re-running a Failed Workflow
1. Go to the failed workflow run
2. Click "Re-run jobs" button (top right)
3. Select which jobs to re-run
4. Click "Re-run failed jobs"

---

## Next Steps

1. ✅ Push workflow files to GitHub (`git push`)
2. ✅ Monitor the first CI run (should complete in 5-10 minutes)
3. ✅ Create a keystore and add secrets (if planning releases)
4. ✅ Create your first release tag (e.g., `git tag -a v1.0.0`)
5. ✅ Download the signed AAB from GitHub Release
6. ✅ (Optional) Set up Play Store deployment

## Further Reading
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Build System](https://developer.android.com/build)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- See `CI.md` for workflow details and local commands
