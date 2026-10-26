$ErrorActionPreference = 'Stop'
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ReleaseRoot = Split-Path -Parent $ScriptRoot
Set-Location $ReleaseRoot

if (-not $env:DB_URL -or -not $env:DB_USERNAME -or -not $env:DB_PASSWORD -or -not $env:JWT_SECRET) {
    throw '请先配置 DB_URL、DB_USERNAME、DB_PASSWORD、JWT_SECRET 环境变量。'
}

java -jar '.\backend\lims-backend.jar'

