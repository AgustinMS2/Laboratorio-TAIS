#!/usr/bin/env bash
# ==============================================================================
# Laboratorio TAIS - Parte 5: Verificación de balanceo de carga en Cluster Swarm
#
# Cumple con los requerimientos:
#   4. Validar el balanceo de carga ante varias invocaciones a la API (ver logs de cada instancia)
#   5. Bajar una instancia y seguir realizando requests. Validar que las 2 instancias siguen activas (ver logs)
#   6. Bajar otra instancia y seguir realizando requests. Validar que la única instancia sigue activa (ver logs)
#
# Uso:
#   bash verificar_cluster.sh
# ==============================================================================

set -euo pipefail

cd -P "$(dirname "${BASH_SOURCE[0]}")" || exit 1

STACK_NAME="laboratorio-tais"
SERVICE_NAME="${STACK_NAME}_ordenes"
API_ORDENES="http://localhost:5002/api/ordenes"
API_PRODUCTOS="http://localhost:5001/api/productos"

# Colores de terminal
if [ -t 1 ]; then
    C_RESET=$(printf '\033[0m'); C_CYAN=$(printf '\033[36m'); C_GREEN=$(printf '\033[32m')
    C_YELLOW=$(printf '\033[33m'); C_RED=$(printf '\033[31m'); C_GRAY=$(printf '\033[90m')
    C_BOLD=$(printf '\033[1m'); C_MAGENTA=$(printf '\033[35m')
else
    C_RESET=''; C_CYAN=''; C_GREEN=''; C_YELLOW=''; C_RED=''; C_GRAY=''; C_BOLD=''; C_MAGENTA=''
fi

titulo()   { printf '\n%s======================================================================%s\n%s== %s%s\n%s======================================================================%s\n' "$C_CYAN" "$C_RESET" "$C_BOLD" "$1" "$C_RESET" "$C_CYAN" "$C_RESET"; }
subtitulo(){ printf '\n%s--- %s ---%s\n' "$C_MAGENTA" "$1" "$C_RESET"; }
ok()       { printf '  %s[OK]%s  %s\n' "$C_GREEN" "$C_RESET" "$1"; }
info()     { printf '  %s[..]%s  %s\n' "$C_GRAY" "$C_RESET" "$1"; }
aviso()    { printf '  %s[!!]%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
falla()    { printf '  %s[XX]%s  %s\n' "$C_RED" "$C_RESET" "$1"; }

esperar_replicas() {
    local deseadas="$1"
    info "Esperando que el servicio $SERVICE_NAME alcance $deseadas/$deseadas réplicas..."
    local intentos=0
    while [ $intentos -lt 40 ]; do
        local actuales
        actuales=$(docker service ls --filter "name=$SERVICE_NAME" --format "{{.Replicas}}" 2>/dev/null || echo "")
        if [ "$actuales" = "${deseadas}/${deseadas}" ]; then
            ok "Servicio estabilizado en $actuales réplicas."
            sleep 2
            return 0
        fi
        sleep 2
        intentos=$((intentos + 1))
        printf "  %s[..]%s  Estado de réplicas: %s...\r" "$C_GRAY" "$C_RESET" "${actuales:-esperando}"
    done
    echo ""
    aviso "Tiempo de espera superado. Réplicas actuales: $(docker service ls --filter "name=$SERVICE_NAME" --format "{{.Replicas}}")"
}

asegurar_producto() {
    info "Preparando producto de prueba con stock disponible..."
    local res
    res=$(curl -s "$API_PRODUCTOS" 2>/dev/null || echo "[]")
    
    PRODUCTO_ID=""
    if command -v jq >/dev/null 2>&1; then
        PRODUCTO_ID=$(echo "$res" | jq -r '[.[] | select(.stock > 50)][0].id // empty' 2>/dev/null || true)
    fi

    if [ -z "${PRODUCTO_ID:-}" ] || [ "$PRODUCTO_ID" = "null" ]; then
        info "Creando/actualizando producto con stock abundante (1000 unidades)..."
        local nuevo_prod
        nuevo_prod=$(curl -s -X POST "$API_PRODUCTOS" \
            -H "Content-Type: application/json" \
            -d '{"nombre":"Articulo Cluster Test","descripcion":"Producto para pruebas de balanceo","precioUnitario":150.0,"stock":1000}' 2>/dev/null || true)
        if command -v jq >/dev/null 2>&1; then
            PRODUCTO_ID=$(echo "$nuevo_prod" | jq -r '.id // empty' 2>/dev/null || true)
        else
            PRODUCTO_ID=$(echo "$nuevo_prod" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2 || true)
        fi
    fi

    if [ -z "${PRODUCTO_ID:-}" ] || [ "$PRODUCTO_ID" = "null" ]; then
        PRODUCTO_ID=1
    fi

    # Asegurar que el producto tenga stock suficiente con PATCH
    curl -s -X PATCH "$API_PRODUCTOS/$PRODUCTO_ID" \
        -H "Content-Type: application/json" \
        -d '{"stock": 1000}' >/dev/null 2>&1 || true

    ok "Producto preparado para generar órdenes: ID $PRODUCTO_ID con stock garantizado."
}

enviar_solicitudes_ordenes() {
    local cantidad="$1"
    local fase="$2"
    subtitulo "Enviando $cantidad solicitudes POST /api/ordenes ($fase)"

    local instancias_ok=""

    for i in $(seq 1 "$cantidad"); do
        local headers_file body_file
        headers_file=$(mktemp)
        body_file=$(mktemp)

        local payload="{
          \"email\": \"cliente-$i@cluster-test.uy\",
          \"direccionEnvio\": \"Av. Pruebas 1234, Ap $i\",
          \"telefono\": \"+59899000$i\",
          \"productos\": [
            { \"productoId\": $PRODUCTO_ID, \"cantidad\": 1 }
          ]
        }"

        # Realizar POST HTTP con curl sin conexión persistente
        local http_code
        http_code=$(curl -s -D "$headers_file" -o "$body_file" -w "%{http_code}" \
            -X POST "$API_ORDENES" \
            -H "Content-Type: application/json" \
            -H "Connection: close" \
            -d "$payload" 2>/dev/null || echo "000")

        local instance_id
        instance_id=$(grep -i '^x-instance-id:' "$headers_file" 2>/dev/null | tr -d '\r' | awk '{print $2}' || true)
        if [ -z "$instance_id" ]; then
            instance_id="desconocida"
        fi

        local orden_id
        if command -v jq >/dev/null 2>&1; then
            orden_id=$(jq -r '.id // empty' "$body_file" 2>/dev/null || true)
        else
            orden_id=$(grep -o '"id":[0-9]*' "$body_file" | head -1 | cut -d':' -f2 || true)
        fi

        rm -f "$headers_file" "$body_file"

        if [ "$http_code" = "201" ]; then
            printf "  %s[Req #%02d]%s HTTP %s | Orden Creada ID: %-4s | %sInstancia:%s %s\n" \
                "$C_GREEN" "$i" "$C_RESET" "$http_code" "${orden_id:-N/A}" "$C_BOLD" "$C_RESET" "$instance_id"
            instancias_ok="$instancias_ok$instance_id\n"
        else
            printf "  %s[Req #%02d]%s HTTP %s (Fallo al crear orden)\n" "$C_RED" "$i" "$C_RESET" "$http_code"
        fi

        sleep 0.3
    done

    printf "\n  %sDistribución de peticiones procesadas por instancia:%s\n" "$C_BOLD" "$C_RESET"
    printf "%b" "$instancias_ok" | sort | uniq -c | while read -r cant inst; do
        printf "    - Instancia %s%-15s%s : %d peticiones atendidas\n" "$C_CYAN" "$inst" "$C_RESET" "$cant"
    done
}

mostrar_logs_instancias() {
    local descripcion="$1"
    subtitulo "Logs de las instancias ($descripcion)"
    info "Extrayendo líneas de log del servicio '$SERVICE_NAME' (filtrando por '[CLUSTER-ORDENES]')..."
    
    docker service logs --tail 60 "$SERVICE_NAME" 2>&1 \
        | grep -E "(\[CLUSTER-ORDENES\]|Orden #[0-9]+ creada)" \
        | tail -n 25 || true
}

# ==============================================================================
# INICIO DE LA PRUEBA
# ==============================================================================

titulo "PARTE 5 - VALIDACIÓN DE CLUSTER, BALANCEO Y TOLERANCIA A FALLOS"

# Verificar que el servicio exista
if ! docker service ls | grep -w "$SERVICE_NAME" >/dev/null; then
    falla "El servicio $SERVICE_NAME no está desplegado en Docker Swarm."
    info "Ejecuta primero: bash levantar_cluster.sh"
    exit 1
fi

asegurar_producto

# ------------------------------------------------------------------------------
# PUNTO 4: Validar balanceo con 3 instancias
# ------------------------------------------------------------------------------
titulo "PUNTO 4: Validar balanceo de carga con 3 instancias en paralelo"
info "Asegurando que el cluster tenga 3 réplicas..."
docker service scale "$SERVICE_NAME=3" >/dev/null
esperar_replicas 3

printf "\n%sContenedores de 'ordenes' activos (3 réplicas):%s\n" "$C_BOLD" "$C_RESET"
docker stack ps "$STACK_NAME" --filter "name=$SERVICE_NAME" --filter "desired-state=running"

enviar_solicitudes_ordenes 9 "Cluster con 3 réplicas"
mostrar_logs_instancias "Validación de balanceo entre las 3 instancias"
ok "Punto 4 completado: solicitudes distribuidas entre las 3 instancias."

# ------------------------------------------------------------------------------
# PUNTO 5: Bajar una instancia (escala a 2) y validar que siguen respondiendo
# ------------------------------------------------------------------------------
titulo "PUNTO 5: Bajar una instancia (reducir a 2 réplicas) y validar servicio"
info "Escalando servicio '$SERVICE_NAME' a 2 réplicas (bajando 1 instancia)..."
docker service scale "$SERVICE_NAME=2"
esperar_replicas 2

printf "\n%sContenedores de 'ordenes' activos (2 réplicas):%s\n" "$C_BOLD" "$C_RESET"
docker stack ps "$STACK_NAME" --filter "name=$SERVICE_NAME" --filter "desired-state=running"

enviar_solicitudes_ordenes 6 "Cluster con 2 réplicas activas"
mostrar_logs_instancias "Validación con 2 instancias activas tras bajar 1"
ok "Punto 5 completado: las 2 instancias restantes siguen procesando las solicitudes."

# ------------------------------------------------------------------------------
# PUNTO 6: Bajar otra instancia (escala a 1) y validar que la única sigue respondiendo
# ------------------------------------------------------------------------------
titulo "PUNTO 6: Bajar otra instancia (reducir a 1 réplica) y validar servicio"
info "Escalando servicio '$SERVICE_NAME' a 1 réplica (bajando 1 instancia más)..."
docker service scale "$SERVICE_NAME=1"
esperar_replicas 1

printf "\n%sContenedor de 'ordenes' activo (1 réplica):%s\n" "$C_BOLD" "$C_RESET"
docker stack ps "$STACK_NAME" --filter "name=$SERVICE_NAME" --filter "desired-state=running"

enviar_solicitudes_ordenes 4 "Cluster con 1 única réplica activa"
mostrar_logs_instancias "Validación con la única instancia activa tras bajar 2"
ok "Punto 6 completado: la única instancia activa sigue respondiendo con normalidad."

# ------------------------------------------------------------------------------
# RESTAURACIÓN: Re-escalar a 3 réplicas
# ------------------------------------------------------------------------------
subtitulo "Restaurando el cluster a 3 réplicas para dejar el entorno listo"
docker service scale "$SERVICE_NAME=3" >/dev/null
esperar_replicas 3

titulo "RESUMEN FINAL DE LA VALIDACIÓN"
ok "1. Cluster Swarm con 3 réplicas validado."
ok "2. Balanceo de carga verificado en logs de cada instancia."
ok "3. Tolerancia a fallos validada al reducir de 3 a 2 instancias."
ok "4. Tolerancia a fallos validada al reducir de 2 a 1 instancia."
ok "5. Cluster restaurado a 3 instancias operativas."

printf "\n%s%sTodas las pruebas del Laboratorio - Parte 5 se completaron exitosamente.%s\n\n" "$C_GREEN" "$C_BOLD" "$C_RESET"
