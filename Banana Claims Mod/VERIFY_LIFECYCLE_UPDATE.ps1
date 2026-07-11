$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$claimRoot = Join-Path $root "src\main\java\com\bananasandwich\bananaclaims\Claim"
$commandRoot = Join-Path $root "src\main\java\com\bananasandwich\bananaclaims\command"

$required = @(
    (Join-Path $claimRoot "ClaimRole.java"),
    (Join-Path $claimRoot "ClaimMutationResult.java"),
    (Join-Path $claimRoot "ClaimManager.java"),
    (Join-Path $commandRoot "LeaveClaimCommand.java"),
    (Join-Path $commandRoot "TransferClaimCommand.java")
)

$missing = $required | Where-Object { -not (Test-Path $_) }
if ($missing.Count -gt 0) {
    Write-Host "FAILED: Missing required files:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

$commandFiles = Get-ChildItem $commandRoot -Recurse -Filter *.java
$directMutations = $commandFiles | Select-String -Pattern "claim\.(addMember|removeMember|addSubOwner|removeSubOwner|demoteSubOwnerToMember|transferOwnership)\("

if ($directMutations) {
    Write-Host "FAILED: A command still mutates claim roles directly:" -ForegroundColor Red
    $directMutations | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}

Write-Host "SUCCESS: Claim lifecycle and ownership polish files are installed." -ForegroundColor Green
