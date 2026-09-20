<#
.SYNOPSIS
    Levanta todo lo necesario para probar el Laboratorio TAIS (partes 2 y 3).

.DESCRIPTION
    1. Verifica Docker y, si Docker Desktop esta apagado, lo inicia y espera a que responda.
    2. Construye las imagenes una por una (con un reintento ante fallas transitorias).
    3. Levanta todo con docker compose: 3 MySQL, Mosquitto (MQTT) y los servicios
       productos (:5001), ordenes (:5002), procesamiento (:5003) y publicador (:5004).
    4. Espera a que los 4 servicios respondan y muestra un resumen con las URLs.
    5. Con -Test, ejecuta ademas el script de pruebas test_apis.sh.

.PARAMETER Reset
    Borra los contenedores Y LOS DATOS (volumenes) antes de levantar: arranca desde cero.

.PARAMETER NoBuild
    No reconstruye las imagenes (usa las que ya existen). Util si no cambiaste codigo.

.PARAMETER Test
    Al terminar de levantar, ejecuta test_apis.sh (requiere Git Bash; descarga jq si falta).

.PARAMETER Down
    Detiene todo (con -Reset tambien borra los datos) y termina.

.PARAMETER TimeoutSegundos
    Tiempo maximo de espera a que los servicios respondan (por defecto 300).

.EXAMPLE
    .\levantar.ps1                 # construye y levanta todo
.EXAMPLE
    .\levantar.ps1 -Reset -Test    # desde cero y con las pruebas
.EXAMPLE
    .\levantar.ps1 -NoBuild        # levanta con las imagenes ya construidas
.EXAMPLE
    .\levantar.ps1 -Down           # detiene todo
#>
param(
    [switch]$Reset,
    [switch]$NoBuild,
    [switch]$Test,
    [switch]$Down,
    [int]$TimeoutSegundos = 300
)

$ErrorActionPreference = 'Continue'
$Root = $PSScriptRoot
Set-Location $Root

# Orden de construccion de imagenes (una por una)
$ServiciosApp = @('productos', 'ordenes', 'publicador', 'procesamiento')

# Endpoint que se consulta para saber si cada servicio ya responde
$Servicios = @(
    @{ Nombre = 'productos';     Puerto = 5001; Url = 'http://localhost:5001/api/productos' },
    @{ Nombre = 'ordenes';       Puerto = 5002; Url = 'http://localhost:5002/api/ordenes' },
    @{ Nombre = 'procesamiento'; Puerto = 5003; Url = 'http://localhost:5003/api/facturas' },
    @{ Nombre = 'publicador';    Puerto = 5004; Url = 'http://localhost:5004/api/publicaciones' }
)

function Titulo($texto) { Write-Host ""; Write-Host "== $texto" -ForegroundColor Cyan }
function Ok($texto)     { Write-Host "  [OK]  $texto" -ForegroundColor Green }
function Info($texto)   { Write-Host "  [..]  $texto" -ForegroundColor Gray }
function Aviso($texto)  { Write-Host "  [!!]  $texto" -ForegroundColor Yellow }
function Falla($texto)  { Write-Host "  [XX]  $texto" -ForegroundColor Red }

function Salir-ConError($texto) {
    Falla $texto
    exit 1
}

function Docker-Responde {
    docker info *> $null
    return ($LASTEXITCODE -eq 0)
}

function Servicio-Responde($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -eq 200)
    } catch {
        return $false
    }
}

# ---------------------------------------------------------------- 1. Docker
Titulo 'Docker'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Salir-ConError 'No se encontro el comando "docker". Instala Docker Desktop: https://www.docker.com/products/docker-desktop/'
}

if (-not (Docker-Responde)) {
    $exe = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path $exe)) {
        Salir-ConError 'Docker no responde y no se encontro Docker Desktop. Inicialo a mano y volve a ejecutar este script.'
    }
    Aviso 'Docker Desktop esta apagado: iniciandolo (puede tardar 1-2 minutos)...'
    Start-Process -FilePath $exe | Out-Null
    $limite = (Get-Date).AddSeconds(240)
    while (-not (Docker-Responde)) {
        if ((Get-Date) -gt $limite) {
            Salir-ConError 'Docker Desktop no respondio en 4 minutos. Revisalo a mano (puede estar pidiendo aceptar terminos o actualizar WSL).'
        }
        Start-Sleep -Seconds 5
        Write-Host '.' -NoNewline
    }
    Write-Host ''
}
Ok 'Docker responde'

docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Salir-ConError 'No se encontro "docker compose" (Compose v2). Actualiza Docker Desktop.'
}

# ---------------------------------------------------------------- Modo -Down
if ($Down) {
    Titulo 'Deteniendo todo'
    if ($Reset) {
        docker compose down -v --remove-orphans
        Ok 'Contenedores y datos eliminados'
    } else {
        docker compose down --remove-orphans
        Ok 'Contenedores detenidos (los datos se conservan; usa -Reset para borrarlos)'
    }
    exit 0
}

# ---------------------------------------------------------------- 2. Reset
if ($Reset) {
    Titulo 'Reset (borra contenedores y datos)'
    docker compose down -v --remove-orphans
    if ($LASTEXITCODE -ne 0) { Salir-ConError 'No se pudo limpiar el entorno anterior.' }
    Ok 'Entorno limpio'
}

# ---------------------------------------------------------------- 3. Build
if ($NoBuild) {
    Titulo 'Construccion de imagenes'
    Info 'Omitida (-NoBuild): se usan las imagenes existentes'
} else {
    Titulo 'Construyendo imagenes (una por una)'
    Info 'La primera vez baja dependencias de Maven dentro del contenedor y puede tardar varios minutos.'
    foreach ($svc in $ServiciosApp) {
        # Hasta 2 intentos: una falla suele ser transitoria (corte de red al bajar dependencias de Maven)
        $construida = $false
        for ($intento = 1; $intento -le 2 -and -not $construida; $intento++) {
            Info "docker compose build $svc  (intento $intento de 2)"
            docker compose build $svc
            if ($LASTEXITCODE -eq 0) {
                $construida = $true
            } elseif ($intento -lt 2) {
                Aviso "Fallo la construccion de '$svc'; reintentando en 5 s..."
                Start-Sleep -Seconds 5
            }
        }
        if (-not $construida) {
            Salir-ConError "Fallo la construccion de '$svc' dos veces. Causa habitual: sin conexion a internet (descarga de dependencias). Ejecuta de nuevo para reintentar."
        }
        Ok "imagen '$svc' lista"
    }
}

# ---------------------------------------------------------------- 4. Up
Titulo 'Levantando contenedores'
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Salir-ConError 'docker compose up fallo. Mira los detalles con: docker compose logs'
}

# ---------------------------------------------------------------- 5. Espera
Titulo "Esperando a que los servicios respondan (maximo $TimeoutSegundos s)"
$inicio = Get-Date
$listos = @{}
while ($listos.Count -lt $Servicios.Count) {
    foreach ($s in $Servicios) {
        if (-not $listos.ContainsKey($s.Nombre) -and (Servicio-Responde $s.Url)) {
            $listos[$s.Nombre] = $true
            $seg = [int]((Get-Date) - $inicio).TotalSeconds
            Ok ("{0,-14} responde en :{1}  ({2} s)" -f $s.Nombre, $s.Puerto, $seg)
        }
    }
    if ($listos.Count -eq $Servicios.Count) { break }

    # Si algun contenedor termino (crash), no tiene sentido seguir esperando
    $caidos = docker ps -a --filter 'label=com.docker.compose.project=laboratorio-tais' --filter 'status=exited' --format '{{.Names}}'
    if ($caidos) {
        Falla "Estos contenedores terminaron inesperadamente: $($caidos -join ', ')"
        foreach ($c in $caidos) { docker logs --tail 25 $c }
        exit 1
    }

    if (((Get-Date) - $inicio).TotalSeconds -gt $TimeoutSegundos) {
        Falla 'Tiempo agotado. Servicios que no responden:'
        foreach ($s in $Servicios) {
            if (-not $listos.ContainsKey($s.Nombre)) {
                Falla "  - $($s.Nombre) ($($s.Url))"
                docker compose logs --tail 20 $s.Nombre
            }
        }
        Info 'Podes aumentar la espera con -TimeoutSegundos 600'
        exit 1
    }
    Start-Sleep -Seconds 4
}

# ---------------------------------------------------------------- Resumen
Titulo 'Todo listo'
Write-Host ''
Write-Host '  Servicio        URL'
Write-Host '  --------------  ------------------------------------------'
Write-Host '  productos       http://localhost:5001/api/productos'
Write-Host '  ordenes         http://localhost:5002/api/ordenes'
Write-Host '  procesamiento   http://localhost:5003/api/facturas   (tambien /api/procesamientos)'
Write-Host '  publicador      http://localhost:5004/api/publicaciones'
Write-Host '  mosquitto       tcp://localhost:1883   (topico ordenes/procesar)'
Write-Host ''
Write-Host '  Ejemplos:' -ForegroundColor Cyan
Write-Host '    Pruebas automaticas ......  bash test_apis.sh      (o: .\levantar.ps1 -Test)'
Write-Host '    Ver mensajes del broker ..  docker compose exec mosquitto mosquitto_sub -t "ordenes/#" -v'
Write-Host '    Ver logs de un servicio ..  docker compose logs -f procesamiento'
Write-Host '    Detener todo .............  .\levantar.ps1 -Down        (con -Reset borra tambien los datos)'
Write-Host ''
Write-Host '  Al iniciar, la orden 1 de ejemplo (estado Created) se publica y se procesa sola en unos segundos.'

# ---------------------------------------------------------------- 6. Pruebas
if ($Test) {
    Titulo 'Ejecutando test_apis.sh'

    # Git Bash (no el bash.exe de WSL que suele aparecer primero en el PATH)
    $candidatos = @(
        (Join-Path $env:ProgramFiles 'Git\bin\bash.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'Git\bin\bash.exe'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Git\bin\bash.exe')
    )
    $bash = $candidatos | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
    if (-not $bash) {
        $cmd = Get-Command bash -ErrorAction SilentlyContinue
        if ($cmd) { $bash = $cmd.Source }
    }
    if (-not $bash) {
        Salir-ConError 'No se encontro Git Bash. Instala Git para Windows (https://git-scm.com) o ejecuta test_apis.sh desde otra terminal bash.'
    }

    # jq es necesario: sin el, el script omite las aserciones JSON y las pruebas no prueban nada
    if (-not (Get-Command jq -ErrorAction SilentlyContinue)) {
        $tools = Join-Path $Root '.tools'
        $jq = Join-Path $tools 'jq.exe'
        if (-not (Test-Path $jq)) {
            Aviso 'jq no esta instalado: descargandolo a .tools\ (solo para este proyecto)...'
            New-Item -ItemType Directory -Force -Path $tools | Out-Null
            try {
                [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
                Invoke-WebRequest -UseBasicParsing -OutFile $jq `
                    -Uri 'https://github.com/jqlang/jq/releases/download/jq-1.7.1/jq-windows-amd64.exe'
            } catch {
                Aviso 'No se pudo descargar jq. Las pruebas se ejecutaran SIN validar el contenido JSON.'
                Aviso 'Instalalo con: winget install jqlang.jq'
            }
        }
        if (Test-Path $jq) { $env:PATH = "$tools;$env:PATH" }
    }

    & $bash 'test_apis.sh'
    $codigo = $LASTEXITCODE
    if ($codigo -eq 0) { Ok 'Todas las pruebas pasaron' } else { Falla "Hubo pruebas fallidas (codigo $codigo)" }
    exit $codigo
}
