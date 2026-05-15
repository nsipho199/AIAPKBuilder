# Local Validation Checklist

Run these commands locally **before pushing** to ensure GitHub Actions will succeed.

## Prerequisites
```bash
# Verify Java 17 is installed
java -version
# Expected output: openjdk version "17.x.x" or temurin-17.x.x

# Verify Gradle wrapper exists
ls -la gradlew

# Make gradlew executable
chmod +x ./gradlew
```

## Quick Health Check (2 minutes)
Run this first to catch obvious errors:

```bash
./gradlew clean lint
```

**Expected**: No errors, no warnings (or warnings are acceptable).

## Full Validation Suite (10-15 minutes)

Run all these commands in sequence:

### 1. Clean Build
```bash
./gradlew clean
```
**Expected**: Cleans build directory.

### 2. Lint Analysis
```bash
./gradlew lint
# Output: app/build/reports/lint-results.html
```
**Expected**: No critical errors. Warnings are OK.

### 3. Unit Tests
```bash
./gradlew test
# Output: app/build/test-results/
```
**Expected**: All tests pass. If any fail, debug locally before pushing.

### 4. Code Coverage Report
```bash
./gradlew testDebugUnitTestCoverage
# Output: app/build/reports/coverage/debug/index.html
```
**Expected**: Coverage report generated. Check coverage % if desired.
View locally: `open app/build/reports/coverage/debug/index.html` (macOS)

### 5. Debug APK Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```
**Expected**: Debug APK created successfully.
Verify file exists: `ls -lh app/build/outputs/apk/debug/app-debug.apk`

### 6. Release Bundle (Unsigned)
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```
**Expected**: Release AAB created successfully (unsigned).
Verify file exists: `ls -lh app/build/outputs/bundle/release/app-release.aab`

## Full Build (Equivalent to CI)
```bash
./gradlew clean lint test testDebugUnitTestCoverage assembleDebug bundleRelease
```
This runs all steps in sequence (same as GitHub Actions CI).

**Expected runtime**: 5-10 minutes depending on machine.

## Android Lint Report
After running lint, open the HTML report:
```bash
# macOS
open app/build/reports/lint-results.html

# Linux
xdg-open app/build/reports/lint-results.html

# Windows (PowerShell)
start app/build/reports/lint-results.html
```

## Code Coverage Report
After running coverage, open the HTML report:
```bash
# macOS
open app/build/reports/coverage/debug/index.html

# Linux
xdg-open app/build/reports/coverage/debug/index.html

# Windows (PowerShell)
start app/build/reports/coverage/debug/index.html
```

## Instrumentation Tests (Optional, requires running device/emulator)
```bash
# Start emulator or connect device first
./gradlew connectedAndroidTest
# Output: app/build/reports/androidTests/connected/
```
**Note**: Only run if you have an emulator running or device connected.

## Testing Your Local Build

### Install and Run Debug App
```bash
./gradlew installDebug
# App installs on connected device/emulator (if one is running)
```

### Generate APK Signing Report
```bash
./gradlew signingReport
# Shows signing certificate info (useful for Play Store)
```

---

## Troubleshooting Local Builds

### Issue: "Command not found: gradlew"
```bash
chmod +x ./gradlew
./gradlew --version
```

### Issue: "JAVA_HOME not set" or "Java not found"
```bash
# Verify Java installation
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=$(/usr/libexec/java_home -v 17)  # macOS
# or
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk  # Linux

# Try again
./gradlew --version
```

### Issue: "Out of memory" (Xmx error)
```bash
# Gradle already set to use 3GB in gradle.properties
# But if still failing, increase temporarily:
./gradlew -Porg.gradle.jvmargs="-Xmx4g" build
```

### Issue: Tests failing locally
```bash
# Run just one test to debug
./gradlew test --tests "com.aiapkbuilder.app.YourTestClass"

# Run with verbose output
./gradlew test --info

# See detailed test reports
open app/build/reports/tests/test/index.html  # macOS
```

### Issue: Lint errors
```bash
# View detailed lint output
./gradlew lint --continue

# Open report to see violations
open app/build/reports/lint-results.html  # macOS
```

---

## Pre-Push Checklist

Before running `git push`, verify:

- [ ] `./gradlew clean lint` passes
- [ ] `./gradlew test` passes (all tests green)
- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew bundleRelease` succeeds
- [ ] No uncommitted changes related to gradle/versions
- [ ] Commit message is descriptive
- [ ] Branch is correct (pushing to main?)

If all checkboxes pass, you're ready to push:

```bash
git push origin main
# GitHub Actions will run the same checks
```

---

## Cleanup

To clean up after testing:

```bash
# Remove build artifacts
./gradlew clean

# Remove Gradle cache (caution: will slow next build)
rm -rf ~/.gradle/caches/

# Reset to last commit
git reset --hard HEAD
```

---

## Performance Tuning

If local builds are slow:

```bash
# Enable Gradle daemon (speeds up subsequent builds)
./gradlew --daemon build

# Use parallel builds
./gradlew --parallel build

# Enable build cache
./gradlew --build-cache build

# All together
./gradlew --daemon --parallel --build-cache build

# Stop daemon if needed
./gradlew --stop
```

---

## One-Liner Full Validation

```bash
chmod +x ./gradlew && ./gradlew clean lint test testDebugUnitTestCoverage assembleDebug bundleRelease && echo "✅ All checks passed!"
```

If this completes without errors, you're ready to push to GitHub.

---

## Next Steps

1. ✅ Run local validation above
2. ✅ Fix any failing tests or lint errors
3. ✅ Commit and push:
   ```bash
   git add .
   git commit -m "feat: add new feature"
   git push origin main
   ```
4. ✅ Go to GitHub Actions and watch the CI run
5. ✅ Download artifacts from workflow run
6. ✅ (Optional) Create release tag when ready:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

---

**Happy building! 🚀**
