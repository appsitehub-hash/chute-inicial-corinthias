<#
install-and-run-windows.ps1
Automação para Windows (PowerShell) que:
 - Verifica se Java (JRE/JDK) está presente
 - Tenta instalar Java automaticamente via winget ou chocolatey (se necessário)
 - Baixa um app.jar de uma URL (configurável)
 - Executa o JAR com java -jar app.jar --gui

Uso (no Windows, PowerShell):
 1) Baixe este script para a mesma pasta onde você quer que o app fique.
 2) Rode (PowerShell como Administrador recomendado apenas se o script precisar instalar pacotes):
    powershell -ExecutionPolicy Bypass -File .\install-and-run-windows.ps1 -DownloadUrl "https://github.com/SEU_USUARIO/SEU_REPO/releases/latest/download/app.jar"

Parâmetros:
 -DownloadUrl : URL pública do asset app.jar (GitHub Release URL por exemplo). Obrigatório.
 -JarName     : Nome do arquivo local (padrão: app.jar)
 -Force       : Força re-download do JAR mesmo se já existir
 -NonInteractive : Se passado, o script não pedirá confirmações e tentará instalar automaticamente
#>

param(
    [Parameter(Mandatory=$true)][string]$DownloadUrl,
    [string]$JarName = "app.jar",
    [switch]$Force,
    [switch]$NonInteractive
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Info { param($m) Write-Host "[INFO] $m" -ForegroundColor Cyan }
function Write-Warn { param($m) Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Write-Err  { param($m) Write-Host "[ERROR] $m" -ForegroundColor Red }

function Is-CommandAvailable($cmd) {
    return (Get-Command $cmd -ErrorAction SilentlyContinue) -ne $null
}

function Is-JavaInstalled {
    try {
        $j = & java -version 2>&1
        if ($j) { return $true }
        return $false
    } catch {
        return $false
    }
}

function Install-Using-Winget {
    Write-Info "Tentando instalar Java via winget..."
    try {
        # prefer Temurin 17 (LTS) or Microsoft OpenJDK
        if (Is-CommandAvailable 'winget') {
            winget install --id EclipseAdoptium.Temurin.17.JRE -e --accept-package-agreements --accept-source-agreements -h 2>$null
            if ($LASTEXITCODE -eq 0) { Write-Info "Java instalado via winget."; return $true }
            winget install --id Microsoft.OpenJDK.17 -e --accept-package-agreements --accept-source-agreements -h 2>$null
            if ($LASTEXITCODE -eq 0) { Write-Info "Java (Microsoft.OpenJDK.17) instalado via winget."; return $true }
            Write-Warn "winget não conseguiu instalar a JVM automaticamente."; return $false
        } else {
            Write-Warn "winget não encontrado."; return $false
        }
    } catch {
        Write-Warn "Falha ao instalar via winget: $_"; return $false
    }
}

function Install-Using-Choco {
    Write-Info "Tentando instalar Java via Chocolatey..."
    try {
        if (-not (Is-CommandAvailable 'choco')) {
            if ($NonInteractive) {
                Write-Warn "Chocolatey não encontrado e NonInteractive habilitado; não será instalado automaticamente."; return $false
            }
            Write-Info "Chocolatey não encontrado. Vou instalar o Chocolatey (requer execução como Administrador)."
            Set-ExecutionPolicy Bypass -Scope Process -Force
            iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
            if (-not (Is-CommandAvailable 'choco')) { Write-Warn "Falha ao instalar Chocolatey."; return $false }
        }
        choco install temurinjre -y --no-progress
        if ($LASTEXITCODE -eq 0) { Write-Info "Java instalado via chocolatey."; return $true }
        Write-Warn "choco não conseguiu instalar a JVM automaticamente."; return $false
    } catch {
        Write-Warn "Falha ao instalar via choco: $_"; return $false
    }
}

function Ensure-Java {
    if (Is-JavaInstalled) { Write-Info "Java já instalado."; return $true }

    # if running non-interactive, try winget then choco
    if ($NonInteractive) {
        if (Install-Using-Winget) { return $true }
        if (Install-Using-Choco) { return $true }
        Write-Warn "Não foi possível instalar Java automaticamente no modo non-interactive."; return $false
    }

    # interactive flow
    Write-Warn "Java não encontrado na máquina. O script tenta instalar automaticamente via winget ou Chocolatey."
    if (Is-CommandAvailable 'winget') {
        Write-Info "winget disponível."; $ok = Install-Using-Winget; if ($ok) { return $true }
    }
    if (Is-CommandAvailable 'choco') {
        Write-Info "choco disponível."; $ok = Install-Using-Choco; if ($ok) { return $true }
    }

    # try to install chocolatey if allowed
    $choice = Read-Host "Deseja tentar instalar Chocolatey (requer privilégios de administrador)? (S/n)"
    if ($choice -in @('','s','S','y','Y')) {
        if (Install-Using-Choco) { return $true }
    }

    Write-Warn "Por favor instale Java manualmente (JRE/JDK 11+). Link oficial: https://adoptium.net/ ou https://adoptium.net/releases.html"
    return $false
}

function Download-Jar {
    param(
        [string]$Url,
        [string]$OutPath
    )
    if (Test-Path $OutPath -PathType Leaf -and -not $Force) {
        Write-Info "$OutPath já existe. Use -Force para sobrescrever."; return $true
    }
    Write-Info "Baixando $Url -> $OutPath"
    try {
        Invoke-WebRequest -Uri $Url -OutFile $OutPath -UseBasicParsing -TimeoutSec 300
        Write-Info "Download concluído."; return $true
    } catch {
        Write-Err "Falha ao baixar $Url: $_"; return $false
    }
}

# Main
Write-Info "Início do instalador automático"
$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $root
$jarPath = Join-Path $root $JarName

# 1) Ensure Java present
if (-not (Ensure-Java)) {
    Write-Err "Java não está disponível. O script será abortado."; exit 2
}

# 2) Download app.jar
if (-not (Download-Jar -Url $DownloadUrl -OutPath $jarPath)) {
    Write-Err "Download do app.jar falhou."; exit 3
}

# 3) Run the jar
Write-Info "Executando: java -jar $jarPath --gui"
try {
    # Start-Process keeps the console usable; use -Wait to block until app exits
    Start-Process -FilePath "java" -ArgumentList "-jar", "`"$jarPath`"", "--gui" -NoNewWindow -Wait
    Write-Info "Aplicação finalizada."; exit 0
} catch {
    Write-Err "Falha ao executar o JAR: $_"; exit 4
}
