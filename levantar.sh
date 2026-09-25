#!/usr/bin/env bash
# ==============================================================================
# Laboratorio TAIS (partes 2 y 3) - Levanta todo lo necesario para probar.
# Para Linux y macOS (en Windows usar levantar.ps1).
#
# Que hace:
#   1. Verifica Docker y, en macOS, inicia Docker Desktop si esta apagado.
#   2. Construye las imagenes una por una (con un reintento ante fallas transitorias).
#   3. Levanta todo con docker compose: 3 MySQL, Mosquitto (MQTT) y los servicios
#      productos (:5001), ordenes (:5002), procesamiento (:5003) y publicador (:5004).
#   4. Espera a que los 4 servicios respondan y muestra un resumen con las URLs.
#   5. Con --test, ejecuta ademas el script de pruebas test_apis.sh.
#
# Uso:
#   bash levantar.sh [opciones]        (o ./levantar.sh si tiene permiso de ejecucion)
#
# Opciones:
#   --reset        Borra los contenedores Y LOS DATOS (volumenes) antes de levantar
#   --no-build     No reconstruye las imagenes (usa las que ya existen)
#   --test         Al terminar de levantar, ejecuta test_apis.sh (descarga jq si falta)
#   --down         Detiene todo y termina (con --reset tambien borra los datos)
#   --timeout N    Espera maxima, en segundos, a que los servicios respondan (defecto 300)
#   -h, --help     Muestra esta ayuda
#
# Ejemplos:
#   bash levantar.sh                 # construye y levanta todo
#   bash levantar.sh --reset --test  # desde cero y con las pruebas
#   bash levantar.sh --no-build      # levanta con las imagenes ya construidas
#   bash levantar.sh --down          # detiene todo
# ==============================================================================

set -u

cd -P "$(dirname "${BASH_SOURCE[0]}")" || exit 1
ROOT=$(pwd)

RESET=false
NO_BUILD=false
RUN_TEST=false
DOWN=false
TIMEOUT=300

# Orden de construccion de imagenes (una por una)
SERVICIOS_APP="productos ordenes publicador procesamiento web jenkins"

# nombre|puerto|url que se consulta para saber si el servicio ya responde
SERVICIOS=(
    "productos|5001|http://localhost:5001/api/productos"
    "ordenes|5002|http://localhost:5002/api/ordenes"
    "procesamiento|5003|http://localhost:5003/api/facturas"
    "publicador|5004|http://localhost:5004/api/publicaciones"
    "web|8081|http://localhost:8081/"
    "jenkins|8080|http://localhost:8080/login"
)

# ---------------------------------------------------------------- utilidades
if [ -t 1 ]; then
    C_RESET=$(printf '\033[0m'); C_CYAN=$(printf '\033[36m'); C_GREEN=$(printf '\033[32m')
    C_YELLOW=$(printf '\033[33m'); C_RED=$(printf '\033[31m'); C_GRAY=$(printf '\033[90m')
else
    C_RESET=''; C_CYAN=''; C_GREEN=''; C_YELLOW=''; C_RED=''; C_GRAY=''
fi

titulo() { printf '\n%s== %s%s\n' "$C_CYAN" "$1" "$C_RESET"; }
ok()     { printf '  %s[OK]%s  %s\n' "$C_GREEN" "$C_RESET" "$1"; }
info()   { printf '  %s[..]%s  %s\n' "$C_GRAY" "$C_RESET" "$1"; }
aviso()  { printf '  %s[!!]%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
falla()  { printf '  %s[XX]%s  %s\n' "$C_RED" "$C_RESET" "$1"; }
salir_con_error() { falla "$1"; exit 1; }

usage() {
    # Imprime el bloque de comentarios del encabezado (hasta la primera linea que no es comentario)
    awk 'NR == 1 { next } /^# ?=+$/ { next } /^#/ { sub(/^# ?/, ""); print; next } { exit }' "${BASH_SOURCE[0]}"
}

docker_responde() { docker info >/dev/null 2>&1; }

# ---------------------------------------------------------------- argumentos
while [ $# -gt 0 ]; do
    case "$1" in
        --reset)    RESET=true ;;
        --no-build) NO_BUILD=true ;;
        --test)     RUN_TEST=true ;;
        --down)     DOWN=true ;;
        --timeout)
            shift
            TIMEOUT="${1:-}"
            ;;
        --timeout=*) TIMEOUT="${1#*=}" ;;
        -h|--help)  usage; exit 0 ;;
        *)
            echo "Opcion desconocida: $1" >&2
            echo "Usa 'bash levantar.sh --help' para ver las opciones." >&2
            exit 1
            ;;
    esac
    shift
done

case "$TIMEOUT" in
    ''|*[!0-9]*) echo "--timeout necesita un numero de segundos (recibido: '$TIMEOUT')" >&2; exit 1 ;;
esac

# ---------------------------------------------------------------- 1. Docker
titulo 'Docker'

command -v docker >/dev/null 2>&1 || \
    salir_con_error 'No se encontro el comando "docker". Instala Docker: https://docs.docker.com/get-docker/'

if ! docker_responde; then
    case "$(uname -s)" in
        Darwin)
            aviso 'Docker Desktop esta apagado: iniciandolo (puede tardar 1-2 minutos)...'
            open -a Docker >/dev/null 2>&1 || \
                salir_con_error 'No se pudo abrir Docker Desktop. Iniciala a mano y volve a ejecutar este script.'
            limite=$((SECONDS + 240))
            while ! docker_responde; do
                if [ "$SECONDS" -gt "$limite" ]; then
                    salir_con_error 'Docker Desktop no respondio en 4 minutos. Revisalo a mano.'
                fi
                sleep 5
                printf '.'
            done
            echo
            ;;
        *)
            salir_con_error 'El servicio de Docker no responde. Iniciarlo con: sudo systemctl start docker (o abri Docker Desktop). Si el error es de permisos, agrega tu usuario al grupo "docker" o usa sudo.'
            ;;
    esac
fi
ok 'Docker responde'

docker compose version >/dev/null 2>&1 || \
    salir_con_error 'No se encontro "docker compose" (Compose v2). Actualiza Docker o instala el plugin docker-compose-plugin.'

# ---------------------------------------------------------------- modo --down
if [ "$DOWN" = true ]; then
    titulo 'Deteniendo todo'
    if [ "$RESET" = true ]; then
        docker compose down -v --remove-orphans
        ok 'Contenedores y datos eliminados'
    else
        docker compose down --remove-orphans
        ok 'Contenedores detenidos (los datos se conservan; usa --reset para borrarlos)'
    fi
    exit 0
fi

command -v curl >/dev/null 2>&1 || \
    salir_con_error 'Hace falta "curl" para comprobar que los servicios respondan. Instalalo e intenta de nuevo.'

# ---------------------------------------------------------------- 2. Reset
if [ "$RESET" = true ]; then
    titulo 'Reset (borra contenedores y datos)'
    docker compose down -v --remove-orphans || salir_con_error 'No se pudo limpiar el entorno anterior.'
    ok 'Entorno limpio'
fi

# ---------------------------------------------------------------- 3. Build
if [ "$NO_BUILD" = true ]; then
    titulo 'Construccion de imagenes'
    info 'Omitida (--no-build): se usan las imagenes existentes'
else
    titulo 'Construyendo imagenes (una por una)'
    info 'La primera vez baja dependencias de Maven dentro del contenedor y puede tardar varios minutos.'
    for svc in $SERVICIOS_APP; do
        # Hasta 2 intentos: una falla suele ser transitoria (corte de red al bajar dependencias de Maven)
        construida=false
        intento=1
        while [ "$intento" -le 2 ] && [ "$construida" = false ]; do
            info "docker compose build $svc  (intento $intento de 2)"
            if docker compose build "$svc"; then
                construida=true
            elif [ "$intento" -lt 2 ]; then
                aviso "Fallo la construccion de '$svc'; reintentando en 5 s..."
                sleep 5
            fi
            intento=$((intento + 1))
        done
        [ "$construida" = true ] || \
            salir_con_error "Fallo la construccion de '$svc' dos veces. Causa habitual: sin conexion a internet (descarga de dependencias). Ejecuta de nuevo para reintentar."
        ok "imagen '$svc' lista"
    done
fi

# ---------------------------------------------------------------- 4. Up
titulo 'Levantando contenedores'
docker compose up -d || salir_con_error 'docker compose up fallo. Mira los detalles con: docker compose logs'

# ---------------------------------------------------------------- 5. Espera
titulo "Esperando a que los servicios respondan (maximo ${TIMEOUT} s)"
inicio=$SECONDS
listos=" "
total=${#SERVICIOS[@]}
while true; do
    cantidad=0
    for s in "${SERVICIOS[@]}"; do
        nombre=${s%%|*}
        resto=${s#*|}
        puerto=${resto%%|*}
        url=${resto#*|}
        case "$listos" in
            *" $nombre "*) cantidad=$((cantidad + 1)); continue ;;
        esac
        if curl -fs -m 3 -o /dev/null "$url"; then
            listos="$listos$nombre "
            cantidad=$((cantidad + 1))
            ok "$(printf '%-14s responde en :%s  (%s s)' "$nombre" "$puerto" "$((SECONDS - inicio))")"
        fi
    done
    [ "$cantidad" -eq "$total" ] && break

    # Si algun contenedor termino (crash), no tiene sentido seguir esperando
    caidos=$(docker ps -a --filter 'label=com.docker.compose.project=laboratorio-tais' --filter 'status=exited' --format '{{.Names}}')
    if [ -n "$caidos" ]; then
        falla "Estos contenedores terminaron inesperadamente: $(echo $caidos)"
        for c in $caidos; do docker logs --tail 25 "$c"; done
        exit 1
    fi

    if [ $((SECONDS - inicio)) -gt "$TIMEOUT" ]; then
        falla 'Tiempo agotado. Servicios que no responden:'
        for s in "${SERVICIOS[@]}"; do
            nombre=${s%%|*}
            url=${s##*|}
            case "$listos" in
                *" $nombre "*) ;;
                *) falla "  - $nombre ($url)"; docker compose logs --tail 20 "$nombre" ;;
            esac
        done
        info 'Podes aumentar la espera con --timeout 600'
        exit 1
    fi
    sleep 4
done

# ---------------------------------------------------------------- Resumen
titulo 'Todo listo'
cat <<'EOF'

  Servicio        URL
  --------------  ------------------------------------------
  productos       http://localhost:5001/api/productos
  ordenes         http://localhost:5002/api/ordenes
  procesamiento   http://localhost:5003/api/facturas   (tambien /api/procesamientos)
  publicador      http://localhost:5004/api/publicaciones
  web             http://localhost:8081   (ordenes y productos)
  jenkins         http://localhost:8080   (job: copiar-nueva-version)
  mosquitto       tcp://localhost:1883   (topico ordenes/procesar)

  Ejemplos:
    Pruebas automaticas ......  bash test_apis.sh      (o: bash levantar.sh --test)
    Ver mensajes del broker ..  docker compose exec mosquitto mosquitto_sub -t "ordenes/#" -v
    Ver logs de un servicio ..  docker compose logs -f procesamiento
    Detener todo .............  bash levantar.sh --down        (con --reset borra tambien los datos)

  Al iniciar, la orden 1 de ejemplo (estado Created) se publica y se procesa sola en unos segundos.
EOF

# ---------------------------------------------------------------- 6. Pruebas
if [ "$RUN_TEST" = true ]; then
    titulo 'Ejecutando test_apis.sh'

    # jq es necesario: sin el, el script omite las aserciones JSON y las pruebas no prueban nada
    if ! command -v jq >/dev/null 2>&1; then
        TOOLS="$ROOT/.tools"
        JQ="$TOOLS/jq"
        if [ ! -x "$JQ" ]; then
            case "$(uname -s)-$(uname -m)" in
                Linux-x86_64)                asset=jq-linux-amd64 ;;
                Linux-aarch64|Linux-arm64)   asset=jq-linux-arm64 ;;
                Darwin-arm64)                asset=jq-macos-arm64 ;;
                Darwin-x86_64)               asset=jq-macos-amd64 ;;
                *)                           asset='' ;;
            esac
            if [ -n "$asset" ]; then
                aviso 'jq no esta instalado: descargandolo a .tools/ (solo para este proyecto)...'
                mkdir -p "$TOOLS"
                if curl -fsSL -o "$JQ" "https://github.com/jqlang/jq/releases/download/jq-1.7.1/$asset"; then
                    chmod +x "$JQ"
                    "$JQ" --version >/dev/null 2>&1 || rm -f "$JQ"
                else
                    rm -f "$JQ"
                fi
            fi
        fi
        if [ -x "$JQ" ]; then
            PATH="$TOOLS:$PATH"
            export PATH
        else
            aviso 'No se pudo obtener jq: las pruebas se ejecutaran SIN validar el contenido JSON.'
            aviso 'Instalalo con: brew install jq  (macOS)  o  sudo apt install jq  (Debian/Ubuntu)'
        fi
    fi

    bash test_apis.sh
    codigo=$?
    if [ "$codigo" -eq 0 ]; then
        ok 'Todas las pruebas pasaron'
    else
        falla "Hubo pruebas fallidas (codigo $codigo)"
    fi
    exit "$codigo"
fi
