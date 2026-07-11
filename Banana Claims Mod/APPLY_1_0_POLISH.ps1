$ErrorActionPreference = "Stop"

$projectRoot = Get-Location
$sourceRoot = Join-Path $projectRoot "src\main"

if (-not (Test-Path (Join-Path $projectRoot "gradlew.bat"))) {
    throw "Run this script from the Banana Claims project root."
}

if (-not (Test-Path $sourceRoot)) {
    throw "The Banana Claims src/main directory was not found."
}

$deletePaths = @(
    "src\main\java\com\bananasandwich\bananaclaims\mixin\ExampleMixin.java",
    "src\main\java\com\bananasandwich\bananaclaims\command\subowner\ClaimSubOwner.java",
    "src\client",
    "docs",
    "README.txt",
    "RETIRE_LEGACY_RENDERER.ps1",
    "VERIFY_ADMIN_UPDATE.ps1",
    "VERIFY_LIFECYCLE_UPDATE.ps1",
    "VERIFY_PERMISSIONS_UPDATE.ps1",
    "VERIFY_PRESSURE_PLATE_HOTFIX.ps1",
    "VERIFY_PROTECTION_UPDATE.ps1"
)

foreach ($relativePath in $deletePaths) {
    $path = Join-Path $projectRoot $relativePath
    if (Test-Path $path) {
        Remove-Item $path -Recurse -Force
        Write-Host "REMOVED: $relativePath"
    }
}

Get-ChildItem $projectRoot -Filter "legacy-renderer-backup-*.zip" -File -ErrorAction SilentlyContinue |
    ForEach-Object {
        Remove-Item $_.FullName -Force
        Write-Host "REMOVED: $($_.Name)"
    }

$requiredFiles = @(
    "DEVELOPMENT.md",
    "COMMANDS.md",
    "FLAGS.md",
    "CONFIGURATION.md",
    "PERFORMANCE.md",
    "RELEASE_CHECKLIST.md",
    "src\main\java\com\bananasandwich\bananaclaims\localization\BananaClaimsMessages.java",
    "src\main\java\com\bananasandwich\bananaclaims\Claim\ClaimSubOwner.java",
    "src\main\resources\assets\bananaclaims\lang\en_us.json"
)

foreach ($relativePath in $requiredFiles) {
    if (-not (Test-Path (Join-Path $projectRoot $relativePath))) {
        throw "Missing required release-polish file: $relativePath"
    }
}

$languageFile = Join-Path $projectRoot "src\main\resources\assets\bananaclaims\lang\en_us.json"
$metadataFile = Join-Path $projectRoot "src\main\resources\fabric.mod.json"

Get-Content $languageFile -Raw | ConvertFrom-Json | Out-Null
Get-Content $metadataFile -Raw | ConvertFrom-Json | Out-Null

$translatableReferences = Get-ChildItem (Join-Path $projectRoot "src\main\java") -Recurse -Filter "*.java" |
    Select-String -Pattern "Component\.translatable"

if ($translatableReferences) {
    $translatableReferences | ForEach-Object { Write-Host $_ }
    throw "Client-resolved custom translation calls remain in the source tree."
}

if (Test-Path (Join-Path $projectRoot "src\client")) {
    throw "Placeholder client sources were not removed."
}

Write-Host "SUCCESS: Banana Claims 1.0 polish files are installed and the source tree is cleaned."
