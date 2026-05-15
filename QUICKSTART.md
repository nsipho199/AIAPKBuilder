# 🚀 Quick Start Checklist - GitHub Actions

Follow this checklist to get your app building in GitHub Actions in < 10 minutes.

## ✅ Step 1: Push Workflows to GitHub (2 minutes)
```bash
cd /path/to/AIAPKBuilder
git add .github/workflows/ gradle.properties CI.md SETUP.md AUTOMATION.md app/build.gradle.kts
git commit -m "ci: add comprehensive GitHub Actions CI/CD workflows"
git push origin main
```

**Verify**: Go to GitHub repo → **Actions** tab. You should see workflows running.

---

## ✅ Step 2: Wait for First CI Run (5-10 minutes)
- Click on the "Android CI" workflow run
- Monitor the logs as it builds
- It should pass (unless there are pre-existing test failures)

**Success indicators**:
- ✅ Green checkmark next to workflow
- ✅ APK and AAB artifacts created
- ✅ No red errors in logs

**If it fails**:
- Check the error in the logs
- Run locally: `./gradlew clean build` to debug
- Fix and push again

---

## ✅ Step 3: (Optional) Add Signing for Releases (3-5 minutes)

### 3A: Create a Keystore
```bash
keytool -genkey -v -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release
```
Follow the prompts and note your passwords.

### 3B: Encode to Base64
```bash
base64 -w 0 release.jks > release.jks.base64
cat release.jks.base64  # Copy this entire output
```

### 3C: Add to GitHub Secrets
1. Go to GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret** for each:

| Name | Value |
|------|-------|
| `KEYSTORE_BASE64` | Paste the Base64 string from step 3B |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `release` |
| `KEY_PASSWORD` | Your key password |

**Done!** Now releases will be signed automatically.

---

## ✅ Step 4: Test Release Build (optional, takes 5 minutes)
```bash
# Create a test tag
git tag -a v0.0.1 -m "Test release"
git push origin v0.0.1
```

Go to GitHub **Actions** → **Release AAB**. Watch it build and create a release.

Then go to **Releases** tab and download the signed AAB.

**Success**: AAB appears in GitHub Release with a name like `aiapkbuilder-v0.0.1.aab`

---

## ✅ Step 5: (Optional) Set Up Code Coverage (2 minutes)

### 5A: Get Codecov Token
1. Visit [codecov.io](https://codecov.io)
2. Sign in with GitHub
3. Add your repo
4. Copy the token

### 5B: Add to GitHub Secrets
1. Go to GitHub → **Settings** → **Secrets and variables** → **Actions**
2. Add **New repository secret**:
   - Name: `CODECOV_TOKEN`
   - Value: Paste your token

**Done!** Coverage reports will now upload automatically.

---

## ✅ Step 6: (Optional) Set Up Play Store Deployment (5 minutes)

### 6A: Create a Google Play Service Account
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create or select your project
3. Go to **APIs & Services** → **Credentials**
4. Create a **Service Account**
5. Create a **JSON key** and download it

### 6B: Add to Google Play Console
1. Go to [Google Play Console](https://play.google.com/console)
2. **Settings** → **Users and permissions**
3. **Invite user** → Add the service account email with **Admin** role

### 6C: Encode and Add to GitHub Secrets
```bash
base64 -w 0 service-account.json > service-account.json.base64
cat service-account.json.base64  # Copy this
```

Add to GitHub **Secrets**:
- Name: `SERVICE_ACCOUNT_JSON_BASE64`
- Value: Paste the Base64 string

**Done!** Now you can deploy to Play Store with one click.

---

## ✅ All Done! 🎉

Your app is now fully automated. Here's what happens:

| Action | Trigger | Result |
|--------|---------|--------|
| Push to `main` | Automatic | ✅ CI runs: lint, tests, APK/AAB build, coverage |
| Create git tag | `git tag -a v1.0.0` | ✅ Signed AAB created, GitHub Release published |
| Deploy to Play Store | Manual via Actions UI | ✅ AAB uploaded to Play Store (internal/alpha/beta/prod) |

---

## 📚 Further Reading

- **CI.md** — Workflow details and local validation commands
- **SETUP.md** — Detailed setup guide with troubleshooting
- **AUTOMATION.md** — High-level overview of all workflows

---

## 🆘 Need Help?

### My CI failed!
1. Click the failed run in Actions
2. Expand the job and find the red error
3. Copy the error message
4. Run the same Gradle command locally: `./gradlew clean build`
5. Fix and push again

### I forgot my keystore password
1. Create a new keystore: `keytool -genkey -v -keystore release.jks ...`
2. Encode to Base64 and update GitHub secret
3. Update `KEYSTORE_PASSWORD` secret

### I can't find the APK/AAB after build
1. Go to the completed workflow run
2. Scroll to **Artifacts** section at the bottom
3. Download from there

### Play Store upload failed
1. Check the error message in the workflow logs
2. Verify service account has Admin role in Play Store
3. Verify your app exists in Play Store Console
4. Try again

---

## 💡 Pro Tips

```bash
# Skip CI for a commit (use sparingly)
git commit -m "docs: update README [skip ci]"

# Force a workflow re-run (if it failed)
# Go to Actions → Failed run → "Re-run jobs"

# Test locally before pushing
./gradlew clean build

# Download AAB from command line
gh run download <run-id> -n aabs
```

---

**You're all set! Start building! 🚀**

For detailed docs, see:
- `CI.md` — Workflow configuration
- `SETUP.md` — Step-by-step setup
- `AUTOMATION.md` — Architecture overview
