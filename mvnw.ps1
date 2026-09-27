[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$rootDir = $PSScriptRoot
$mvnCmd = Join-Path $rootDir ".tools\apache-maven-3.9.6\bin\mvn.cmd"

if (Test-Path $mvnCmd) {
    & $mvnCmd $Arguments
} else {
    mvn @Arguments
}
