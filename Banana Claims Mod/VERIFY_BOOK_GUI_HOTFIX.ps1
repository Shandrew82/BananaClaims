$ErrorActionPreference = 'Stop'

$required = @(
    'src/main/java/com/bananasandwich/bananaclaims/book/BookComponents.java',
    'src/main/java/com/bananasandwich/bananaclaims/book/BookDialogs.java',
    'src/main/java/com/bananasandwich/bananaclaims/book/BookPacketSender.java',
    'src/main/java/com/bananasandwich/bananaclaims/book/ClaimBookManager.java',
    'src/main/java/com/bananasandwich/bananaclaims/mixin/ServerCustomClickActionMixin.java',
    'src/main/java/com/bananasandwich/bananaclaims/command/BlueMapClaimCommand.java',
    'src/main/java/com/bananasandwich/bananaclaims/command/InviteClaimCommand.java',
    'src/main/java/com/bananasandwich/bananaclaims/invitation/ClaimInvitationManager.java',
    'src/main/resources/bananaclaims.mixins.json'
)

foreach ($path in $required) {
    if (-not (Test-Path $path)) {
        throw "Missing required file: $path"
    }
}

$bookFiles = @(
    'src/main/java/com/bananasandwich/bananaclaims/book/BookComponents.java',
    'src/main/java/com/bananasandwich/bananaclaims/book/ClaimBookManager.java',
    'src/main/java/com/bananasandwich/bananaclaims/invitation/ClaimInvitationManager.java'
)

$forbidden = Select-String -Path $bookFiles -Pattern 'ClickEvent\.RunCommand|ClickEvent\.SuggestCommand' -ErrorAction SilentlyContinue
if ($forbidden) {
    throw 'Old RUN_COMMAND or SUGGEST_COMMAND book actions are still present.'
}

$mixins = Get-Content 'src/main/resources/bananaclaims.mixins.json' -Raw
if ($mixins -notmatch 'ServerCustomClickActionMixin') {
    throw 'ServerCustomClickActionMixin is not registered.'
}

$bookSender = Get-Content 'src/main/java/com/bananasandwich/bananaclaims/book/BookPacketSender.java' -Raw
if ($bookSender -notmatch 'Items\.WRITTEN_BOOK' -or $bookSender -notmatch 'bananaclaims_management_book') {
    throw 'Physical Banana Claims book support is missing.'
}

$blueMap = Get-Content 'src/main/java/com/bananasandwich/bananaclaims/command/BlueMapClaimCommand.java' -Raw
if (($blueMap | Select-String -Pattern 'argument\("hex", StringArgumentType\.greedyString\(\)' -AllMatches).Matches.Count -lt 2) {
    throw 'BlueMap hex arguments are not using greedyString.'
}

$invite = Get-Content 'src/main/java/com/bananasandwich/bananaclaims/command/InviteClaimCommand.java' -Raw
if (($invite | Select-String -Pattern 'literal\("accept"\)[\s\S]*?greedyString' -AllMatches).Matches.Count -lt 1 -or
    ($invite | Select-String -Pattern 'literal\("deny"\)[\s\S]*?greedyString' -AllMatches).Matches.Count -lt 1) {
    throw 'Invitation accept/deny selectors are not using greedyString.'
}

Write-Host 'SUCCESS: Banana Claims Book GUI hotfix files are installed and verified.' -ForegroundColor Green
