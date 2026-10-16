param(
    [string]$OriginalClasses = '.audit/parser-review-2026-10-15/classes',
    [string]$PreviousClasses = '.audit/parser-implementation-2026-10-15/classes',
    [string]$CurrentClasses = '.audit/parser-followup-2026-10-15/classes',
    [string]$ProbeClasses = '.audit/parser-followup-2026-10-15/probes',
    [string]$DependencyFile = '.audit/parser-review-2026-10-15/dependencies.txt'
)

$ErrorActionPreference = 'Stop'
$dependencies = (Get-Content -LiteralPath $DependencyFile -Raw).Trim()
$out = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Invoke-Probe([string]$classes, [int]$fork, [string]$name, [string]$filter) {
    $classpath = "$classes;$dependencies;$ProbeClasses"
    $log = Join-Path $out "$name-$fork.log"
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $rows = & java -Xms256m -Xmx1024m -Xbatch '-Dfile.encoding=UTF-8' -cp $classpath com.landawn.abacus.query.FollowupProbe $fork $filter 2> $log
    $code = $LASTEXITCODE
    $ErrorActionPreference = $savedPreference
    if ($code -ne 0) { throw "$name fork $fork failed with exit $code; see $log" }
    [System.IO.File]::WriteAllLines((Join-Path $out "$name-$fork.csv"), [string[]]$rows, $utf8)
    Write-Output "Completed $name fork $fork"
}

# Alternate order across pairs. JVMs run sequentially, after compilation and correctness checks.
foreach ($run in @(@('previous', 1), @('current', 1), @('current', 2), @('previous', 2), @('previous', 3), @('current', 3))) {
    $variant = [string]$run[0]
    $fork = [int]$run[1]
    $classes = if ($variant -eq 'previous') { $PreviousClasses } else { $CurrentClasses }
    Invoke-Probe $classes $fork $variant ''
}

# Original HEAD provides an allocation reference for the incremental-memo tradeoff.
foreach ($fork in 1..3) {
    Invoke-Probe $OriginalClasses $fork 'original' 'allocation-reference'
}
