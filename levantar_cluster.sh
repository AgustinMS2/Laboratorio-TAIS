#!/usr/bin/env bash
# ==============================================================================
# Laboratorio TAIS - Parte 5: Cluster con Docker Swarm (3 réplicas de ordenes)
#
# Uso:
#   bash levantar_cluster.sh           # Inicia Swarm (si es necesario), compila y despliega el stack
#   bash levantar_cluster.sh --down    # Detiene y remueve el stack
#   bash levantar_cluster.sh --status  # Muestra el estado del cluster y sus servicios
# ==============================================================================

set -euo pipefail

cd -P "$(dirname "${BASH_SOURCE[0]}")" || exit 1
STACK_NAME="laboratorio-tais"

# Colores de terminal
if [ -t 1 ]; then
    C_RESET=$(printf '\033[0m'); C_CYAN=$(printf '\033[36m'); C_GREEN=$(printf '\033[32m')
    C_YELLOW=$(printf '\033[33m'); C_RED=$(printf '\033[31m'); C_GRAY=$(printf '\033[90m')
    C_BOLD=$(printf '\033[1m')
else
    C_RESET=''; C_CYAN=''; C_GREEN=''; C_YELLOW=''; C_RED=''; C_GRAY=''; C_BOLD=''
fi

titulo() { printf '\n%s== %s%s\n' "$C_CYAN" "$1" "$C_RESET"; }
ok()     { printf '  %s[OK]%s  %s\n' "$C_GREEN" "$C_RESET" "$1"; }
info()   { printf '  %s[..]%s  %s\n' "$C_GRAY" "$C_RESET" "$1"; }
aviso()  { printf '  %s[!!]%s  %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
falla()  { printf '  %s[XX]%s  %s\n' "$C_RED" "$C_RESET" "$1"; }

detener_stack() {
    titulo "Deteniendo stack de Docker Swarm: $STACK_NAME"
    if docker stack ls | grep -w "$STACK_NAME" >/dev/null; then
        info "Eliminando stack $STACK_NAME..."
        docker stack rm "$STACK_NAME"
        info "Esperando a que los servicios se detengan por completo..."
        while docker service ls | grep -w "$STACK_NAME" >/dev/null; do
            sleep 2
        done
        ok "Stack $STACK_NAME eliminado exitosamente."
    else
        aviso "El stack $STACK_NAME no se encuentra en ejecución."
    fi
    exit 0
}

mostrar_estado() {
    titulo "Estado de los servicios en el stack: $STACK_NAME"
    if docker stack ls | grep -w "$STACK_NAME" >/dev/null; then
        docker stack services "$STACK_NAME"
        printf '\n%sTareas / Contenedores en ejecución:%s\n' "$C_BOLD" "$C_RESET"
        docker stack ps "$STACK_NAME" --filter "desired-state=running"
    else
        aviso "El stack $STACK_NAME no está activo."
    fi
    exit 0
}

# Procesar argumentos
case "${1:-}" in
    --down) detener_stack ;;
    --status) mostrar_estado ;;
    -h|--help)
        echo "Uso: bash levantar_cluster.sh [--down | --status | --help]"
        exit 0
        ;;
esac

# 1. Verificar Docker
titulo "1. Verificando Docker y entorno Swarm"
if ! docker info >/dev/null 2>&1; then
    falla "Docker no está respondiendo o el daemon está apagado."
    exit 1
fi
ok "Docker está activo."

SWARM_STATE=$(docker info --format '{{.Swarm.LocalNodeState}}' 2>/dev/null || echo "inactive")
if [ "$SWARM_STATE" != "active" ]; then
    info "Docker Swarm está inactivo. Inicializando swarm en el nodo local..."
    docker swarm init >/dev/null 2>&1 || docker swarm init --advertise-addr 127.0.0.1
    ok "Docker Swarm inicializado correctamente."
else
    ok "Docker Swarm ya se encuentra activo."
fi

# 2. Compilar imágenes requeridas
titulo "2. Construyendo / actualizando imágenes locales de los microservicios"
info "Compilando imágenes con docker compose build (incluye microservicio ordenes con logging de cluster)..."
docker compose build ordenes productos publicador procesamiento web

ok "Imágenes compiladas y etiquetadas localmente."

# 3. Desplegar el stack en Swarm
titulo "3. Desplegando stack '$STACK_NAME' en Docker Swarm"
# Si existía una red local previa de tipo bridge con el mismo nombre, removerla para evitar conflictos con el driver overlay
if docker network ls --format '{{.Name}} {{.Driver}}' | grep "^${STACK_NAME}_backend bridge" >/dev/null; then
    info "Removiendo red bridge local residual '${STACK_NAME}_backend'..."
    docker network rm "${STACK_NAME}_backend" >/dev/null 2>&1 || true
fi

info "Desplegando docker-compose.yml en Swarm..."
docker stack deploy -c docker-compose.yml "$STACK_NAME"
ok "Stack '$STACK_NAME' enviado a Docker Swarm."

# 4. Esperar convergencia de réplicas
titulo "4. Esperando a que el cluster de 'ordenes' (3 réplicas) esté listo"
info "Esperando a que las 3 réplicas del servicio '${STACK_NAME}_ordenes' estén en estado 3/3..."

MAX_WAIT=180
ELAPSED=0
READY=false

while [ $ELAPSED -lt $MAX_WAIT ]; do
    REPLICAS=$(docker service ls --filter "name=${STACK_NAME}_ordenes" --format "{{.Replicas}}" 2>/dev/null || true)
    if [ "$REPLICAS" = "3/3" ]; then
        READY=true
        break
    fi
    sleep 3
    ELAPSED=$((ELAPSED + 3))
    printf "  %s[..]%s  Réplicas actuales de ordenes: %s (tiempo: %ss)\r" "$C_GRAY" "$C_RESET" "${REPLICAS:-0/3}" "$ELAPSED"
done
echo ""

if [ "$READY" = true ]; then
    ok "Las 3 réplicas del servicio ordenes están corriendo en paralelo."
else
    aviso "El servicio ordenes aún no alcanzó 3/3 réplicas (actual: $REPLICAS). Verificando estado general..."
fi

# 5. Esperar a que la API responda en el puerto 5002
titulo "5. Verificando conectividad HTTP con la API de órdenes (puerto 5002)"
API_READY=false
for i in $(seq 1 30); do
    HTTP_CODE=$(curl -s -m 3 -o /dev/null -w "%{http_code}" "http://localhost:5002/api/ordenes" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        API_READY=true
        break
    fi
    sleep 2
done

if [ "$API_READY" = true ]; then
    ok "API de órdenes respondiendo correctamente en http://localhost:5002/api/ordenes (HTTP $HTTP_CODE)."
else
    aviso "La API de órdenes aún está iniciando. Puedes verificar su progreso con 'docker service logs ${STACK_NAME}_ordenes'."
fi

titulo "6. Resumen del cluster Swarm"
docker stack services "$STACK_NAME"
printf "\n%sContenedores de 'ordenes' corriendo:%s\n" "$C_BOLD" "$C_RESET"
docker stack ps "$STACK_NAME" --filter "name=${STACK_NAME}_ordenes" --filter "desired-state=running"

printf "\n%s%sCluster desplegado con éxito.%s\n" "$C_GREEN" "$C_BOLD" "$C_RESET"
printf "Para ejecutar las pruebas de balanceo, baja de instancias y logs, ejecuta:\n"
printf "  %sbash verificar_cluster.sh%s\n\n" "$C_CYAN" "$C_RESET"
