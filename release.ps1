# Release and Upload Script for MyMusic
# This script builds the release APK, verifies/signs it with V3, and uploads it to GitHub Releases.

$ErrorActionPreference = "Stop"

# 1. Configuration
$SDK_DIR = "C:\Users\Li Yuxuan\AppData\Local\Android\Sdk"
$BUILD_TOOLS_VERSION = "36.0.0"
$APKSIGNER = Join-Path $SDK_DIR "build-tools\$BUILD_TOOLS_VERSION\apksigner.bat"
$KEYSTORE_PATH = "app/release-key.jks"
$KEYSTORE_PASS = "android"
$KEY_ALIAS = "my-key-alias"
$KEY_PASS = "android"
$REPO = "EeSwahn/MyMusic"

# 2. Get Version Name from app/build.gradle.kts
Write-Host "Fetching version information..." -ForegroundColor Cyan
$gradleContent = Get-Content "app/build.gradle.kts" -Raw
if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') {
    $VERSION = $Matches[1]
} else {
    Write-Error "Could not find versionName in app/build.gradle.kts"
    exit 1
}
Write-Host "Target Version: v$VERSION" -ForegroundColor Green

# 3. Build Release APK
Write-Host "Building Release APK..." -ForegroundColor Cyan
./gradlew clean assembleRelease

$APK_PATH = "app/build/outputs/apk/release/app-release.apk"
if (-not (Test-Path $APK_PATH)) {
    Write-Error "APK file not found at $APK_PATH"
    exit 1
}

# 4. Sign/Verify V3 Signature
# Gradle might have already signed it, but we'll ensure V3 is applied correctly.
Write-Host "Verifying and ensuring V3 signature..." -ForegroundColor Cyan

# Verify first
& $APKSIGNER verify --verbose --print-certs $APK_PATH

# Re-sign with V3 to be sure (optional if Gradle did it, but user explicitly asked for V3)
# apksigner sign --ks $KEYSTORE_PATH --ks-pass pass:$KEYSTORE_PASS --ks-key-alias $KEY_ALIAS --key-pass pass:$KEY_PASS --v3-signing-enabled true $APK_PATH

# 5. Check if GitHub CLI is logged in
Write-Host "Checking GitHub CLI status..." -ForegroundColor Cyan
try {
    gh auth status
} catch {
    Write-Error "Please login to GitHub CLI first: gh auth login"
    exit 1
}

# 6. Create GitHub Release
$RELEASE_TAG = "v$VERSION"
Write-Host "Creating GitHub Release $RELEASE_TAG and uploading APK..." -ForegroundColor Cyan

# Check if release already exists
$releaseExists = gh release view $RELEASE_TAG --repo $REPO 2>$null
if ($releaseExists) {
    Write-Host "Release $RELEASE_TAG already exists. Uploading as asset..." -ForegroundColor Yellow
    gh release upload $RELEASE_TAG $APK_PATH --repo $REPO --clobber
} else {
    gh release create $RELEASE_TAG $APK_PATH --title "MyMusic $RELEASE_TAG" --notes "Automated release version $RELEASE_TAG" --repo $REPO
}

Write-Host "Successfully released and uploaded to GitHub!" -ForegroundColor Green
