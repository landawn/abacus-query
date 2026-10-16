param(
    [string]$BaselineClasses = '.audit/parser-review-2026-10-15/classes',
    [string]$CurrentClasses = '.audit/parser-implementation-2026-10-15/classes',
    [string]$ReviewProbes = '.audit/parser-review-2026-10-15/probes',
    [string]$ImplementationProbes = '.audit/parser-implementation-2026-10-15/probes',
    [string]$DependencyFile = '.audit/parser-review-2026-10-15/dependencies.txt'
)

$ErrorActionPreference = 'Stop'
$dependencies = (Get-Content -LiteralPath $DependencyFile -Raw).Trim()
$out = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Invoke-Probe([string]$classes, [string]$mainClass, [int]$fork, [string]$name) {
    $classpath = "$classes;$dependencies;$ReviewProbes;$ImplementationProbes"
    $log = Join-Path $out "$name-$fork.log"
    # Native stderr is retained separately. A failing JVM is never accepted as a measurement.
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $rows = & java -Xms256m -Xmx1024m -Xbatch '-Dfile.encoding=UTF-8' -cp $classpath $mainClass $fork 2> $log
    $code = $LASTEXITCODE
    $ErrorActionPreference = $savedPreference
    if ($code -ne 0) { throw "$mainClass ($name fork $fork) failed with exit $code; see $log" }
    [System.IO.File]::WriteAllLines((Join-Path $out "$name-$fork.csv"), [string[]]$rows, $utf8)
    Write-Output "Completed $name fork $fork"
}

# Alternate order across pairs. All JVMs run sequentially; finish builds/tests first.
foreach ($run in @(@('before', 1), @('after', 1), @('after', 2), @('before', 2), @('before', 3), @('after', 3))) {
    $variant = [string]$run[0]
    $fork = [int]$run[1]
    $classes = if ($variant -eq 'before') { $BaselineClasses } else { $CurrentClasses }
    Invoke-Probe $classes 'com.landawn.abacus.query.ParserReviewProbe' $fork $variant
    Invoke-Probe $classes 'com.landawn.abacus.query.SupplementaryParsedSqlProbe' $fork "supplement-$variant"
}

foreach ($fork in 1..3) {
    Invoke-Probe $CurrentClasses 'com.landawn.abacus.query.CursorProbe' $fork 'cursor'
}
