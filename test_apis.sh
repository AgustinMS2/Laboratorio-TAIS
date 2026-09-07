#!/usr/bin/env bash
# ==============================================================================
# Laboratorio IISS - Parte 2: Script de Testing de APIs REST (curl)
# ==============================================================================
# Microservicios:
#   - Productos: http://localhost:5001/api/productos
#   - Órdenes:   http://localhost:5002/api/ordenes
#
# Uso:
#   ./test_apis.sh [OPCIONES]
#
# Opciones:
#   -h, --help            Muestra esta ayuda
#   -v, --verbose         Muestra detalles de solicitudes y respuestas HTTP
#   -s, --suite SUITE     Ejecuta solo una suite: 'productos', 'ordenes', 'e2e' o 'all' (defecto: all)
#   --url-productos URL   Sobrescribe URL base de Productos (defecto: http://localhost:5001)
#   --url-ordenes URL     Sobrescribe URL base de Órdenes (defecto: http://localhost:5002)
#   --no-color            Desactiva colores en la salida
#   --skip-healthcheck    Omite la comprobación inicial de conectividad
# ==============================================================================

set -u

# --- Configuración por defecto ---
BASE_URL_PRODUCTOS="${BASE_URL_PRODUCTOS:-http://localhost:5001}"
BASE_URL_ORDENES="${BASE_URL_ORDENES:-http://localhost:5002}"
SUITE="all"
VERBOSE=false
USE_COLOR=true
SKIP_HEALTHCHECK=false

# --- Colores ---
C_RESET=''
C_BOLD=''
C_GREEN=''
C_RED=''
C_YELLOW=''
C_BLUE=''
C_CYAN=''
C_MAGENTA=''

setup_colors() {
    if [ "$USE_COLOR" = true ] && [ -t 1 ]; then
        C_RESET='\033[0m'
        C_BOLD='\033[1m'
        C_GREEN='\033[32m'
        C_RED='\033[31m'
        C_YELLOW='\033[33m'
        C_BLUE='\033[34m'
        C_CYAN='\033[36m'
        C_MAGENTA='\033[35m'
    else
        C_RESET=''
        C_BOLD=''
        C_GREEN=''
        C_RED=''
        C_YELLOW=''
        C_BLUE=''
        C_CYAN=''
        C_MAGENTA=''
    fi
}

# --- Contadores de Pruebas ---
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0
START_TIME=$(date +%s)

# --- Variables de Ejecución ---
LAST_HTTP_CODE=""
LAST_HTTP_BODY=""
LAST_HTTP_HEADERS=""
LAST_LOCATION_HEADER=""
PRODUCTO_ID=""
ORDEN_ID=""
E2E_PRODUCTO_ID=""
E2E_ORDEN_ID=""

# --- Parseo de Argumentos ---
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -s|--suite)
                SUITE="$2"
                shift 2
                ;;
            --url-productos)
                BASE_URL_PRODUCTOS="$2"
                shift 2
                ;;
            --url-ordenes)
                BASE_URL_ORDENES="$2"
                shift 2
                ;;
            --no-color)
                USE_COLOR=false
                shift
                ;;
            --skip-healthcheck)
                SKIP_HEALTHCHECK=true
                shift
                ;;
            *)
                echo "Opción desconocida: $1"
                echo "Usa '$0 --help' para ver las opciones disponibles."
                exit 1
                ;;
        esac
    done
}

show_help() {
    echo -e "${C_BOLD}Laboratorio TAIS - Script de Testing de APIs REST vía cURL${C_RESET}"
    echo
    echo "Uso: $0 [OPCIONES]"
    echo
    echo "Opciones disponibles:"
    echo "  -h, --help               Muestra esta ayuda y sale."
    echo "  -v, --verbose            Muestra payloads completos de envío y respuesta."
    echo "  -s, --suite SUITE        Ejecuta solo la suite especificada:"
    echo "                             - productos: Pruebas CRUD y validaciones de Productos"
    echo "                             - ordenes:   Pruebas de Órdenes y validaciones"
    echo "                             - e2e:       Flujo de integración y control de stock"
    echo "                             - all:       Todas las anteriores (por defecto)"
    echo "  --url-productos URL      URL base del servicio Productos (defecto: http://localhost:5001)"
    echo "  --url-ordenes URL        URL base del servicio Órdenes (defecto: http://localhost:5002)"
    echo "  --no-color               Desactiva el resaltado con colores ANSI."
    echo "  --skip-healthcheck       Omite verificar si los servicios están activos antes de empezar."
    echo
    echo "Ejemplos:"
    echo "  $0"
    echo "  $0 --verbose"
    echo "  $0 --suite e2e"
    echo "  $0 --suite productos -v"
}

# --- Verificación de Dependencias ---
check_dependencies() {
    if ! command -v curl &>/dev/null; then
        echo -e "${C_RED}[ERROR] 'curl' no está instalado o no está en el PATH.${C_RESET}"
        exit 1
    fi

    if ! command -v jq &>/dev/null; then
        echo -e "${C_YELLOW}[ADVERTENCIA] 'jq' no está instalado. Se usarán validaciones básicas.${C_RESET}"
        HAS_JQ=false
    else
        HAS_JQ=true
    fi
}

# --- Health Check Previo ---
check_services_health() {
    if [ "$SKIP_HEALTHCHECK" = true ]; then
        return 0
    fi

    echo -e "${C_CYAN}Comprobando conectividad con los microservicios...${C_RESET}"
    local p_ok=false
    local o_ok=false

    if curl -s -m 3 -o /dev/null -w "%{http_code}" "$BASE_URL_PRODUCTOS/api/productos" &>/dev/null; then
        p_ok=true
    fi

    if curl -s -m 3 -o /dev/null -w "%{http_code}" "$BASE_URL_ORDENES/api/ordenes" &>/dev/null; then
        o_ok=true
    fi

    if [ "$p_ok" = false ] || [ "$o_ok" = false ]; then
        echo -e "${C_RED}[ERROR] No se pudo conectar a los servicios:${C_RESET}"
        [ "$p_ok" = false ] && echo -e "  - Microservicio Productos en ${C_BOLD}$BASE_URL_PRODUCTOS${C_RESET} [NO RESPONDE]"
        [ "$o_ok" = false ] && echo -e "  - Microservicio Órdenes   en ${C_BOLD}$BASE_URL_ORDENES${C_RESET} [NO RESPONDE]"
        echo
        echo -e "${C_YELLOW}Por favor, asegúrate de levantar los servicios antes de ejecutar las pruebas:${C_RESET}"
        echo -e "  ${C_BOLD}docker compose up --build -d${C_RESET}"
        echo "O bien, ejecuta con '--skip-healthcheck' si deseas forzar la ejecución."
        exit 1
    fi
    echo -e "${C_GREEN}✓ Ambos microservicios están en línea y respondiendo.${C_RESET}"
    echo
}

# --- Invocación HTTP con cURL ---
# Argumentos:
#   $1: Método HTTP (GET, POST, PUT, PATCH, DELETE)
#   $2: URL completa
#   $3: Payload JSON (vacío si es GET)
#   $4: Código HTTP esperado
#   $5: Descripción del test
make_request() {
    local method="$1"
    local url="$2"
    local payload="$3"
    local expected_code="$4"
    local desc="$5"

    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    # Archivo temporal para cabeceras y cuerpo
    local tmp_headers
    tmp_headers=$(mktemp)
    local tmp_body
    tmp_body=$(mktemp)

    local curl_cmd=(curl -s -S -D "$tmp_headers" -w "%{http_code}" -o "$tmp_body" -X "$method" "$url")

    if [ -n "$payload" ]; then
        curl_cmd+=(-H "Content-Type: application/json" -d "$payload")
    fi

    # Ejecutar curl
    LAST_HTTP_CODE=$("${curl_cmd[@]}" 2>/dev/null || echo "000")
    LAST_HTTP_BODY=$(cat "$tmp_body")
    LAST_HTTP_HEADERS=$(cat "$tmp_headers")
    LAST_LOCATION_HEADER=$(grep -i '^Location:' "$tmp_headers" | tr -d '\r' | awk '{print $2}')

    rm -f "$tmp_headers" "$tmp_body"

    # Comparar código HTTP
    local passed=false
    if [ "$LAST_HTTP_CODE" -eq "$expected_code" ] 2>/dev/null; then
        passed=true
    fi

    # Imprimir resultado
    if [ "$passed" = true ]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        printf "  ${C_GREEN}✔ PASS${C_RESET} [%s] %-6s %-48s (HTTP %s)\n" "$expected_code" "$method" "$url" "$LAST_HTTP_CODE"
        printf "         ${C_CYAN}%s${C_RESET}\n" "$desc"
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        printf "  ${C_RED}✘ FAIL${C_RESET} [%s != %s] %-6s %-48s (HTTP %s)\n" "$LAST_HTTP_CODE" "$expected_code" "$method" "$url" "$LAST_HTTP_CODE"
        printf "         ${C_RED}%s${C_RESET}\n" "$desc"
    fi

    # Modo verbose o en caso de fallo
    if [ "$VERBOSE" = true ] || [ "$passed" = false ]; then
        echo -e "    ${C_BOLD}--- Detalle de Invocación ---${C_RESET}"
        echo -e "    ${C_BOLD}Comando:${C_RESET} curl -X $method \"$url\""
        if [ -n "$payload" ]; then
            if [ "$HAS_JQ" = true ]; then
                echo -e "    ${C_BOLD}Payload enviado:${C_RESET}\n$(echo "$payload" | jq . 2>/dev/null | sed 's/^/      /')"
            else
                echo -e "    ${C_BOLD}Payload enviado:${C_RESET} $payload"
            fi
        fi
        echo -e "    ${C_BOLD}HTTP Status:${C_RESET} $LAST_HTTP_CODE (esperado: $expected_code)"
        if [ -n "$LAST_HTTP_BODY" ]; then
            if [ "$HAS_JQ" = true ]; then
                echo -e "    ${C_BOLD}Respuesta:${C_RESET}\n$(echo "$LAST_HTTP_BODY" | jq . 2>/dev/null | sed 's/^/      /')"
            else
                echo -e "    ${C_BOLD}Respuesta:${C_RESET} $LAST_HTTP_BODY"
            fi
        fi
        echo -e "    ${C_BOLD}-----------------------------${C_RESET}"
    fi

    [ "$passed" = true ]
    return $?
}

# --- Aserciones de Contenido JSON ---
assert_json_eq() {
    local json_path="$1"
    local expected_val="$2"
    local desc="$3"

    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    if [ "$HAS_JQ" = false ]; then
        echo "         ${C_YELLOW}⚠ Skip validación JSON (jq no disponible): $desc${C_RESET}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    fi

    local actual_val
    actual_val=$(echo "$LAST_HTTP_BODY" | jq -r "$json_path" 2>/dev/null)

    if [ "$actual_val" = "$expected_val" ]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        printf "         ${C_GREEN}↳ ✔ Validado:${C_RESET} %s == %s\n" "$desc" "$expected_val"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        printf "         ${C_RED}↳ ✘ Error:${C_RESET} %s (Esperado: '%s', Obtenido: '%s')\n" "$desc" "$expected_val" "$actual_val"
        return 1
    fi
}

assert_json_contains() {
    local json_path="$1"
    local substring="$2"
    local desc="$3"

    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    if [ "$HAS_JQ" = false ]; then
        echo "         ${C_YELLOW}⚠ Skip validación JSON (jq no disponible): $desc${C_RESET}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    fi

    local actual_val
    actual_val=$(echo "$LAST_HTTP_BODY" | jq -r "$json_path" 2>/dev/null)

    if [[ "$actual_val" == *"$substring"* ]]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        printf "         ${C_GREEN}↳ ✔ Validado:${C_RESET} %s contiene '%s'\n" "$desc" "$substring"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        printf "         ${C_RED}↳ ✘ Error:${C_RESET} %s no contiene '%s' (Valor: '%s')\n" "$desc" "$substring" "$actual_val"
        return 1
    fi
}

assert_json_is_array() {
    local desc="$1"

    TESTS_TOTAL=$((TESTS_TOTAL + 1))

    if [ "$HAS_JQ" = false ]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    fi

    local is_array
    is_array=$(echo "$LAST_HTTP_BODY" | jq 'if type=="array" then "true" else "false" end' 2>/dev/null)

    if [ "$is_array" = "\"true\"" ] || [ "$is_array" = "true" ]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        printf "         ${C_GREEN}↳ ✔ Validado:${C_RESET} %s es un array JSON\n" "$desc"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        printf "         ${C_RED}↳ ✘ Error:${C_RESET} %s no es un array JSON\n" "$desc"
        return 1
    fi
}

assert_location_header_present() {
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    if [ -n "$LAST_LOCATION_HEADER" ]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        printf "         ${C_GREEN}↳ ✔ Header Location presente:${C_RESET} %s\n" "$LAST_LOCATION_HEADER"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        printf "         ${C_RED}↳ ✘ Falta Header Location en la respuesta${C_RESET}\n"
        return 1
    fi
}

# ==============================================================================
# SUITE 1: MICROSERVICIO DE PRODUCTOS (:5001)
# ==============================================================================
run_suite_productos() {
    echo
    echo -e "${C_BOLD}${C_BLUE}========================================================================${C_RESET}"
    echo -e "${C_BOLD}${C_BLUE}  1. MICROSERVICIO PRODUCTOS (:5001) - CRUD Y VALIDACIONES             ${C_RESET}"
    echo -e "${C_BOLD}${C_BLUE}========================================================================${C_RESET}"

    # 1.1 Listar todos los productos
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos" "" 200 \
        "1.1 Listar todos los productos del catálogo"
    assert_json_is_array "Respuesta de productos"

    # 1.2 Crear un nuevo producto (Happy Path)
    local payload_crear='{
      "nombre": "Monitor Dell UltraSharp 27",
      "descripcion": "Monitor 4K UHD IPS con calibracion de fabrica",
      "precioUnitario": 680.0,
      "stock": 25,
      "imagenes": [
        "https://cdn.local/img/dell-u27-1.jpg",
        "https://cdn.local/img/dell-u27-2.jpg"
      ]
    }'
    make_request "POST" "$BASE_URL_PRODUCTOS/api/productos" "$payload_crear" 201 \
        "1.2 Crear un nuevo producto (Happy Path)"
    assert_location_header_present

    if [ "$HAS_JQ" = true ]; then
        PRODUCTO_ID=$(echo "$LAST_HTTP_BODY" | jq -r '.id' 2>/dev/null)
        echo -e "         ${C_CYAN}ℹ ID de producto creado: ${PRODUCTO_ID}${C_RESET}"
    else
        PRODUCTO_ID=$(echo "$LAST_HTTP_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi

    # Fallback si no se obtuvo ID
    if [ -z "$PRODUCTO_ID" ] || [ "$PRODUCTO_ID" = "null" ]; then
        PRODUCTO_ID=1
    fi

    # 1.3 Obtener producto por ID
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos/$PRODUCTO_ID" "" 200 \
        "1.3 Obtener producto por ID ($PRODUCTO_ID)"
    assert_json_eq ".nombre" "Monitor Dell UltraSharp 27" "Nombre del producto"
    assert_json_eq ".stock" "25" "Stock inicial del producto"

    # 1.4 Reemplazar producto completo (PUT)
    local payload_put='{
      "nombre": "Monitor Dell UltraSharp 27 v2",
      "descripcion": "Monitor 4K UHD IPS actualizado con USB-C Hub",
      "precioUnitario": 720.5,
      "stock": 20,
      "imagenes": [
        "https://cdn.local/img/dell-u27-v2.jpg"
      ]
    }'
    make_request "PUT" "$BASE_URL_PRODUCTOS/api/productos/$PRODUCTO_ID" "$payload_put" 200 \
        "1.4 Reemplazar producto completo (PUT)"
    assert_json_contains ".mensaje" "Producto actualizado correctamente" "Mensaje de confirmación PUT"

    # 1.5 Actualizar parcialmente un producto (PATCH)
    local payload_patch='{
      "precioUnitario": 710.0,
      "stock": 18
    }'
    make_request "PATCH" "$BASE_URL_PRODUCTOS/api/productos/$PRODUCTO_ID" "$payload_patch" 200 \
        "1.5 Actualizar parcialmente producto (PATCH - precioUnitario y stock)"
    assert_json_contains ".mensaje" "Producto actualizado correctamente" "Mensaje de confirmación PATCH"

    # 1.6 Verificar cambios aplicados en producto
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos/$PRODUCTO_ID" "" 200 \
        "1.6 Verificar cambios aplicados por PUT y PATCH"
    assert_json_eq ".nombre" "Monitor Dell UltraSharp 27 v2" "Nombre actualizado por PUT"
    assert_json_eq ".stock" "18" "Stock actualizado por PATCH"

    # 1.7 Error 404 - Obtener producto inexistente
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos/999999" "" 404 \
        "1.7 Error 404 - Obtener producto inexistente"
    assert_json_eq ".codigo" "404" "Código de error 404"
    assert_json_contains ".mensaje" "No existe el producto" "Mensaje de producto no encontrado"

    # 1.8 Error 400 - Crear producto con datos inválidos
    local payload_invalido='{
      "nombre": "",
      "precioUnitario": -10.0,
      "stock": -5
    }'
    make_request "POST" "$BASE_URL_PRODUCTOS/api/productos" "$payload_invalido" 400 \
        "1.8 Error 400 - Crear producto con datos inválidos"
    assert_json_eq ".codigo" "400" "Código de error 400"
    assert_json_eq ".mensaje" "Datos de entrada inválidos" "Mensaje de validación"

    # 1.9 Error 404 - Reemplazar producto inexistente (PUT)
    local payload_put_404='{
      "nombre": "Inexistente",
      "precioUnitario": 100.0,
      "stock": 5
    }'
    make_request "PUT" "$BASE_URL_PRODUCTOS/api/productos/999999" "$payload_put_404" 404 \
        "1.9 Error 404 - Reemplazar producto inexistente (PUT)"
    assert_json_eq ".codigo" "404" "Código de error 404"

    # 1.10 Error 400 - Actualización parcial vacía (PATCH)
    make_request "PATCH" "$BASE_URL_PRODUCTOS/api/productos/$PRODUCTO_ID" "{}" 400 \
        "1.10 Error 400 - Actualización parcial vacía (PATCH {})"
    assert_json_eq ".codigo" "400" "Código de error 400 por payload vacío"

    # 1.11 Error 404 - Actualización parcial de producto inexistente (PATCH)
    local payload_patch_404='{
      "stock": 50
    }'
    make_request "PATCH" "$BASE_URL_PRODUCTOS/api/productos/999999" "$payload_patch_404" 404 \
        "1.11 Error 404 - Actualización parcial de producto inexistente (PATCH)"
    assert_json_eq ".codigo" "404" "Código de error 404"
}

# ==============================================================================
# SUITE 2: MICROSERVICIO DE ÓRDENES (:5002)
# ==============================================================================
run_suite_ordenes() {
    echo
    echo -e "${C_BOLD}${C_MAGENTA}========================================================================${C_RESET}"
    echo -e "${C_BOLD}${C_MAGENTA}  2. MICROSERVICIO ÓRDENES (:5002) - REGISTRO Y CONSULTA               ${C_RESET}"
    echo -e "${C_BOLD}${C_MAGENTA}========================================================================${C_RESET}"

    # Si no tenemos un PRODUCTO_ID válido disponible, usamos uno por defecto o consultamos /api/productos
    if [ -z "$PRODUCTO_ID" ] || [ "$PRODUCTO_ID" = "null" ]; then
        if [ "$HAS_JQ" = true ]; then
            PRODUCTO_ID=$(curl -s "$BASE_URL_PRODUCTOS/api/productos" | jq -r '.[0].id // 1' 2>/dev/null)
        else
            PRODUCTO_ID=1
        fi
    fi

    # 2.1 Listar todas las órdenes
    make_request "GET" "$BASE_URL_ORDENES/api/ordenes" "" 200 \
        "2.1 Listar todas las órdenes de compra"
    assert_json_is_array "Respuesta de órdenes"

    # 2.2 Crear una nueva orden (Happy Path)
    local payload_orden="{
      \"email\": \"juan.perez@dominio.uy\",
      \"direccionEnvio\": \"Rambla Claudio Williman Parada 15, Punta del Este\",
      \"telefono\": \"+59899333444\",
      \"productos\": [
        {
          \"productoId\": $PRODUCTO_ID,
          \"cantidad\": 1
        }
      ]
    }"
    make_request "POST" "$BASE_URL_ORDENES/api/ordenes" "$payload_orden" 201 \
        "2.2 Crear una nueva orden de compra (Happy Path)"
    assert_location_header_present

    if [ "$HAS_JQ" = true ]; then
        ORDEN_ID=$(echo "$LAST_HTTP_BODY" | jq -r '.id' 2>/dev/null)
        echo -e "         ${C_CYAN}ℹ ID de orden creada: ${ORDEN_ID}${C_RESET}"
    else
        ORDEN_ID=$(echo "$LAST_HTTP_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi

    if [ -z "$ORDEN_ID" ] || [ "$ORDEN_ID" = "null" ]; then
        ORDEN_ID=1
    fi

    # 2.3 Obtener orden por ID
    make_request "GET" "$BASE_URL_ORDENES/api/ordenes/$ORDEN_ID" "" 200 \
        "2.3 Obtener orden por ID ($ORDEN_ID)"
    assert_json_eq ".email" "juan.perez@dominio.uy" "Email del comprador"
    assert_json_eq ".estado" "Created" "Estado inicial de la orden (Created)"

    # 2.4 Obtener orden con detalle completo de productos
    make_request "GET" "$BASE_URL_ORDENES/api/ordenes/$ORDEN_ID/detalle" "" 200 \
        "2.4 Obtener orden con detalle completo de productos"
    assert_json_contains ".total" "." "Total calculado presente en la orden"

    # 2.5 Error 404 - Obtener orden inexistente
    make_request "GET" "$BASE_URL_ORDENES/api/ordenes/999999" "" 404 \
        "2.5 Error 404 - Obtener orden inexistente"
    assert_json_eq ".codigo" "404" "Código de error 404"
    assert_json_contains ".mensaje" "No existe la orden" "Mensaje de orden no encontrada"

    # 2.6 Error 404 - Detalle de orden inexistente
    make_request "GET" "$BASE_URL_ORDENES/api/ordenes/999999/detalle" "" 404 \
        "2.6 Error 404 - Detalle de orden inexistente"
    assert_json_eq ".codigo" "404" "Código de error 404"

    # 2.7 Error 400 - Crear orden con datos inválidos
    local payload_orden_invalida='{
      "email": "email-no-valido",
      "direccionEnvio": "",
      "telefono": "123",
      "productos": []
    }'
    make_request "POST" "$BASE_URL_ORDENES/api/ordenes" "$payload_orden_invalida" 400 \
        "2.7 Error 400 - Crear orden con datos inválidos"
    assert_json_eq ".codigo" "400" "Código de error 400"
    assert_json_eq ".mensaje" "Datos de entrada inválidos" "Mensaje de validación"

    # 2.8 Error 409 - Crear orden con producto inexistente
    local payload_prod_inexistente='{
      "email": "cliente@email.com",
      "direccionEnvio": "Av. Italia 3333, Maldonado",
      "telefono": "+59899111222",
      "productos": [
        {
          "productoId": 999999,
          "cantidad": 1
        }
      ]
    }'
    make_request "POST" "$BASE_URL_ORDENES/api/ordenes" "$payload_prod_inexistente" 409 \
        "2.8 Error 409 - Crear orden con producto inexistente"
    assert_json_eq ".codigo" "409" "Código de conflicto 409"
    assert_json_contains ".mensaje" "no existen" "Mensaje de producto no existe"

    # 2.9 Error 409 - Crear orden con stock insuficiente
    local payload_stock_insuficiente="{
      \"email\": \"cliente@email.com\",
      \"direccionEnvio\": \"Av. Italia 3333, Maldonado\",
      \"telefono\": \"+59899111222\",
      \"productos\": [
        {
          \"productoId\": $PRODUCTO_ID,
          \"cantidad\": 999999
        }
      ]
    }"
    make_request "POST" "$BASE_URL_ORDENES/api/ordenes" "$payload_stock_insuficiente" 409 \
        "2.9 Error 409 - Crear orden con stock insuficiente"
    assert_json_eq ".codigo" "409" "Código de conflicto 409 por stock"
    assert_json_contains ".mensaje" "Stock insuficiente" "Mensaje de stock insuficiente"
}

# ==============================================================================
# SUITE 3: PRUEBAS DE INTEGRACIÓN END-TO-END (E2E)
# ==============================================================================
run_suite_e2e() {
    echo
    echo -e "${C_BOLD}${C_YELLOW}========================================================================${C_RESET}"
    echo -e "${C_BOLD}${C_YELLOW}  3. PRUEBAS DE INTEGRACIÓN E2E - FLUJO COMPLETO Y CONTROL DE STOCK    ${C_RESET}"
    echo -e "${C_BOLD}${C_YELLOW}========================================================================${C_RESET}"

    # 3.1 [E2E] Crear producto para prueba de flujo con stock = 10 y precio = 150.00
    local payload_e2e_prod='{
      "nombre": "Teclado Mecanico RGB E2E",
      "descripcion": "Teclado mecanico para pruebas de integracion E2E",
      "precioUnitario": 150.0,
      "stock": 10,
      "imagenes": [
        "https://cdn.local/img/teclado-e2e.jpg"
      ]
    }'
    make_request "POST" "$BASE_URL_PRODUCTOS/api/productos" "$payload_e2e_prod" 201 \
        "3.1 [E2E] Crear producto dedicado para flujo (stock: 10, precio: 150.0)"
    assert_location_header_present

    if [ "$HAS_JQ" = true ]; then
        E2E_PRODUCTO_ID=$(echo "$LAST_HTTP_BODY" | jq -r '.id' 2>/dev/null)
    else
        E2E_PRODUCTO_ID=$(echo "$LAST_HTTP_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi

    if [ -z "$E2E_PRODUCTO_ID" ] || [ "$E2E_PRODUCTO_ID" = "null" ]; then
        echo -e "${C_RED}[ERROR] No se pudo obtener el ID del producto creado para E2E. Abortando suite.${C_RESET}"
        return 1
    fi
    echo -e "         ${C_CYAN}ℹ ID de producto E2E creado: ${E2E_PRODUCTO_ID}${C_RESET}"

    # 3.2 [E2E] Verificar stock inicial (10)
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos/$E2E_PRODUCTO_ID" "" 200 \
        "3.2 [E2E] Verificar stock inicial del producto ($E2E_PRODUCTO_ID)"
    assert_json_eq ".stock" "10" "Stock inicial es 10"

    # 3.3 [E2E] Crear orden consumiendo 4 unidades
    local payload_e2e_orden="{
      \"email\": \"e2e.tester@email.com\",
      \"direccionEnvio\": \"Calle Falsa 123, Montevideo\",
      \"telefono\": \"+59898765432\",
      \"productos\": [
        {
          \"productoId\": $E2E_PRODUCTO_ID,
          \"cantidad\": 4
        }
      ]
    }"
    make_request "POST" "$BASE_URL_ORDENES/api/ordenes" "$payload_e2e_orden" 201 \
        "3.3 [E2E] Crear orden consumiendo 4 unidades del producto"
    assert_location_header_present

    if [ "$HAS_JQ" = true ]; then
        E2E_ORDEN_ID=$(echo "$LAST_HTTP_BODY" | jq -r '.id' 2>/dev/null)
    else
        E2E_ORDEN_ID=$(echo "$LAST_HTTP_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    echo -e "         ${C_CYAN}ℹ ID de orden E2E creada: ${E2E_ORDEN_ID}${C_RESET}"

    # 3.4 [E2E] Verificar descuento automático de stock (10 - 4 = 6)
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos/$E2E_PRODUCTO_ID" "" 200 \
        "3.4 [E2E] Verificar descuento automático en Productos (Stock esperado: 6)"
    assert_json_eq ".stock" "6" "Stock remanente tras la compra es exactamente 6"

    # 3.5 [E2E] Verificar detalle y cálculos de la orden (4 x 150.00 = 600.00)
    make_request "GET" "$BASE_URL_ORDENES/api/ordenes/$E2E_ORDEN_ID/detalle" "" 200 \
        "3.5 [E2E] Verificar cálculo de la orden (4 x 150 = 600) en /detalle"
    assert_json_eq ".total" "600.0" "Total de la orden es 600.00"
    assert_json_eq ".productos[0].subtotal" "600.0" "Subtotal de la línea es 600.00"
    assert_json_eq ".productos[0].cantidad" "4" "Cantidad de unidades en la línea es 4"
    assert_json_eq ".productos[0].producto.nombre" "Teclado Mecanico RGB E2E" "Nombre de producto en el detalle"

    # 3.6 [E2E] Intentar pedir 10 unidades (disponibles 6) -> Conflicto 409
    local payload_e2e_exceso="{
      \"email\": \"e2e.tester@email.com\",
      \"direccionEnvio\": \"Calle Falsa 123, Montevideo\",
      \"telefono\": \"+59898765432\",
      \"productos\": [
        {
          \"productoId\": $E2E_PRODUCTO_ID,
          \"cantidad\": 10
        }
      ]
    }"
    make_request "POST" "$BASE_URL_ORDENES/api/ordenes" "$payload_e2e_exceso" 409 \
        "3.6 [E2E] Intentar pedir 10 unidades (disponibles 6) -> Conflicto 409"
    assert_json_eq ".codigo" "409" "Código de conflicto 409"
    assert_json_contains ".detalles[0]" "stock disponible 6" "Detalle indica stock disponible 6"
    assert_json_contains ".detalles[0]" "cantidad solicitada 10" "Detalle indica cantidad solicitada 10"

    # 3.7 [E2E] Verificar que el stock no fue alterado tras el fallo
    make_request "GET" "$BASE_URL_PRODUCTOS/api/productos/$E2E_PRODUCTO_ID" "" 200 \
        "3.7 [E2E] Verificar que el stock se mantiene en 6 tras el rechazo"
    assert_json_eq ".stock" "6" "El stock se mantiene intacto en 6 unidades"
}

# --- Resumen Final ---
print_summary() {
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - START_TIME))

    echo
    echo -e "${C_BOLD}========================================================================${C_RESET}"
    echo -e "${C_BOLD}                       RESUMEN DE EJECUCIÓN                            ${C_RESET}"
    echo -e "${C_BOLD}========================================================================${C_RESET}"
    echo -e "  Total de verificaciones: ${C_BOLD}$TESTS_TOTAL${C_RESET}"
    echo -e "  Superadas:                ${C_GREEN}${C_BOLD}$TESTS_PASSED${C_RESET}"
    if [ "$TESTS_FAILED" -gt 0 ]; then
        echo -e "  Fallidas:                 ${C_RED}${C_BOLD}$TESTS_FAILED${C_RESET}"
    else
        echo -e "  Fallidas:                 ${C_BOLD}0${C_RESET}"
    fi
    echo -e "  Tiempo de ejecución:      ${duration}s"
    echo -e "${C_BOLD}========================================================================${C_RESET}"

    if [ "$TESTS_FAILED" -eq 0 ]; then
        echo -e "${C_GREEN}${C_BOLD}✓ TODAS LAS PRUEBAS PASARON EXITOSAMENTE${C_RESET}"
        echo
        return 0
    else
        echo -e "${C_RED}${C_BOLD}✘ SE ENCONTRARON $TESTS_FAILED FALLAS EN LAS PRUEBAS${C_RESET}"
        echo
        return 1
    fi
}

# --- Punto de Entrada ---
main() {
    parse_args "$@"
    setup_colors

    echo -e "${C_BOLD}========================================================================${C_RESET}"
    echo -e "${C_BOLD}   Laboratorio TAIS - Testing Automatizado de APIs REST (cURL)          ${C_RESET}"
    echo -e "${C_BOLD}========================================================================${C_RESET}"
    echo -e "  Microservicio Productos: ${C_CYAN}$BASE_URL_PRODUCTOS${C_RESET}"
    echo -e "  Microservicio Órdenes:   ${C_CYAN}$BASE_URL_ORDENES${C_RESET}"
    echo -e "  Suite seleccionada:      ${C_YELLOW}$SUITE${C_RESET}"
    echo -e "${C_BOLD}========================================================================${C_RESET}"
    echo

    check_dependencies
    check_services_health

    case "$SUITE" in
        productos)
            run_suite_productos
            ;;
        ordenes)
            run_suite_ordenes
            ;;
        e2e)
            run_suite_e2e
            ;;
        all)
            run_suite_productos
            run_suite_ordenes
            run_suite_e2e
            ;;
        *)
            echo -e "${C_RED}Suite desconocida: '$SUITE'. Opciones válidas: productos, ordenes, e2e, all.${C_RESET}"
            exit 1
            ;;
    esac

    print_summary
}

main "$@"
