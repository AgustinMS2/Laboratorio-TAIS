# Laboratorio IISS – Partes 2 y 3

Aplicación para dar soporte a la operativa de un local de venta de artículos, que
vende de forma presencial y también recibe pedidos por la Web.

Se implementa con un estilo arquitectónico de **microservicios**:

- **Parte 2** – dos servicios independientes exponen las APIs REST de **Productos** y
  **Órdenes**, cada uno con su propia base de datos MySQL. `ordenes` consume la API de
  `productos` (vía HTTP) para validar existencia y stock al registrar una orden.
- **Parte 3** – las órdenes se comunican por **mensajería** (Eclipse Mosquitto / MQTT): un servicio
  **publicador** publica las órdenes listas para procesar y otro servicio, **procesamiento**, las
  recibe y las procesa: valida el stock, lo descuenta, genera la factura y deja la orden lista para
  entregar.

## Arquitectura

| Servicio        | Puerto | Base de datos                   | Descripción                                                         |
|-----------------|--------|---------------------------------|---------------------------------------------------------------------|
| `productos`     | `5001` | MySQL (`mysql-productos`)       | Catálogo de productos y su stock                                    |
| `ordenes`       | `5002` | MySQL (`mysql-ordenes`)         | Órdenes de compra. **No conoce el broker**: solo expone la API      |
| `publicador`    | `5004` | – (sin base de datos)           | **Publica** en el broker las órdenes listas para procesar (`Created`) |
| `procesamiento` | `5003` | MySQL (`mysql-procesamiento`)   | **Recibe** las órdenes del broker, las procesa y genera las facturas |
| `mosquitto`     | `1883` | (persistencia propia)           | Broker MQTT (Eclipse Mosquitto 2)                                   |

```
                               curl / cliente
                                     │
        ┌───────────────────┬────────┴──────────┬────────────────────┐
        ▼                   ▼                   ▼                    ▼
 productos:5001       ordenes:5002       publicador:5004     procesamiento:5003
 (mysql-productos)   (mysql-ordenes)      (sin BD)          (mysql-procesamiento)
        ▲                ▲   ▲                 │                     │ ▲
        │ GET producto   │   │ GET ?estado=    │ publica QoS 1       │ │ suscribe QoS 1
        │ (alta de orden)│   │   Created       ▼                     │ │ (sesión persistente)
        │                │   └──────────── ordenes/procesar ◄────────┘ │
        │                │             ┌───────────────────────────────┴──┐
        │                │             │       mosquitto:1883             │
        │                │             └──────────────────────────────────┘
        │                └── GET orden · PATCH estado ◄─────── procesamiento
        └────────── GET producto · PATCH stock ◄────────────── procesamiento
```

Cada servicio tiene su propia base de datos (el publicador no necesita una: consulta a `ordenes` por
API y no guarda nada que no pueda reconstruir). `ordenes` **ni siquiera incluye un cliente MQTT**: la
mensajería es responsabilidad de `publicador` (emisor) y `procesamiento` (receptor).

### Flujo de una orden (parte 3)

1. `POST /api/ordenes` (`ordenes`) valida que los productos existan y tengan stock y guarda la
   orden en estado **`Created`**. **No descuenta stock** en este momento.
2. `publicador` consulta a `ordenes` (`GET /api/ordenes?estado=Created`) cada 2 segundos y publica en
   Mosquitto, tópico **`ordenes/procesar`** (QoS 1), un mensaje JSON con el id, el estado y la
   fecha de creación de cada orden lista para procesar (la fecha se publica tal como la emitió `ordenes`):
   ```json
   { "id": 1, "estado": "Created", "fechaCreacion": "2026-06-21T14:30:00-03:00" }
   ```
3. `procesamiento` recibe el mensaje y procesa la orden:
   1. **Valida que no esté previamente procesada** (registro propio `procesamientos` + estado de
      la orden en `ordenes`). Si ya estaba procesada, ignora el mensaje.
   2. Verifica el stock de **cada** producto solicitado.
   3. **Con stock** → descuenta el stock de cada producto (`PATCH /api/productos/{id}`), genera la
      **factura** (un ítem por producto con su precio unitario; el total es la suma de los
      subtotales) y deja la orden en **`Ready to Delivery`**.
   4. **Sin stock** en alguno (o producto inexistente) → no descuenta nada y deja la orden en
      **`No Stock`**.
4. `procesamiento` informa el estado a `ordenes` con `PATCH /api/ordenes/{id}/estado`.

`No Stock` es posible aunque el alta valide el stock: dos órdenes pueden pasar la validación con el
mismo stock disponible y la segunda en procesarse ya no lo encuentra.

### Por qué Mosquitto (MQTT)

- Es el ejemplo del enunciado y encaja con el modelo *publicar / suscribir* con mensajes JSON.
- **Muy liviano** (~10 MB de imagen, pocos MB de RAM): importa porque el stack ya tiene 3 apps Java y
  3 bases MySQL.
- QoS 1 (entrega "al menos una vez") y sesión persistente: si `procesamiento` está caído, el broker
  retiene los mensajes y se los entrega al volver.
- Cliente Java estable: Eclipse Paho.

### Confiabilidad e idempotencia

Con QoS 1 un mensaje puede llegar duplicado, así que el procesamiento es **idempotente**:

- `procesamientos.orden_id` es único: cada orden se procesa una sola vez. Un duplicado se ignora sin
  volver a descontar stock ni a facturar.
- El resultado se guarda localmente **antes** de informarlo a `ordenes`. Si esa llamada falla,
  `ordenActualizada` queda en `false` y cuando llega el duplicado solo se reintenta informar el estado.
- Si falla el descuento de stock a mitad de camino (o el guardado de la factura), el stock ya
  descontado se **devuelve** (compensación) y el mensaje se reintenta.
- `publicador` **republica** las órdenes que siguen en `Created` pasados 120 s (`reintento-segundos`),
  por lo que una orden publicada mientras `procesamiento` estaba caído o sin suscribirse no se pierde.
  Como la fuente de verdad es `ordenes` (no el registro del publicador), si el publicador se reinicia
  simplemente vuelve a publicar lo que siga en `Created`; el procesador ignora los duplicados.
- Ni `publicador` ni `procesamiento` necesitan que el broker (o `ordenes`) estén arriba para iniciar:
  publican o se suscriben cuando están disponibles, y reintentan en cada ciclo.

Límite conocido: si el proceso `procesamiento` se cae exactamente entre el descuento de stock y el
guardado de la factura, el reintento descontaría de nuevo. Es la ventana de cualquier flujo sin
transacción distribuida (2PC/saga completa), fuera del alcance del laboratorio.

## APIs REST

La especificación completa está en [`openapi.yaml`](openapi.yaml) (OpenAPI 3.0.3, validada).

### Productos — `http://localhost:5001`

| Método  | Ruta                    | Éxito                          | Error                              |
|---------|-------------------------|--------------------------------|------------------------------------|
| `GET`   | `/api/productos`        | `200` lista de productos       | –                                  |
| `GET`   | `/api/productos/{id}`   | `200` producto                 | `404` no encontrado                |
| `POST`  | `/api/productos`        | `201` + id del producto creado | `400` datos inválidos              |
| `PUT`   | `/api/productos/{id}`   | `200` + mensaje                | `404` no encontrado / `400`        |
| `PATCH` | `/api/productos/{id}`   | `200` + mensaje                | `404` no encontrado / `400`        |

### Órdenes — `http://localhost:5002`

| Método  | Ruta                          | Éxito                                                | Error                                                                 |
|---------|-------------------------------|------------------------------------------------------|----------------------------------------------------------------------|
| `GET`   | `/api/ordenes`                | `200` lista de órdenes (`?estado=Created` filtra por estado) | `400` estado desconocido                                     |
| `GET`   | `/api/ordenes/{id}`           | `200` orden (productos = id + cantidad)              | `404` no encontrada                                                  |
| `GET`   | `/api/ordenes/{id}/detalle`   | `200` orden con el detalle completo de cada producto | `404` no encontrada                                                  |
| `POST`  | `/api/ordenes`                | `201` + id de la orden creada                        | `400` datos inválidos / `409` stock insuficiente o producto inexistente / `502` servicio de productos no disponible |
| `PATCH` | `/api/ordenes/{id}/estado`    | `200` + mensaje (lo usa `procesamiento`)             | `400` estado no permitido / `404` no encontrada / `409` ya procesada con otro resultado |

Estados de una orden: `Created`, **`Ready to Delivery`**, **`No Stock`**, `Confirmed`, `Shipped`,
`Delivered`, `Cancelled`. Las órdenes se crean en `Created`; solo una orden en `Created` puede pasar a
`Ready to Delivery` o `No Stock` (repetir el mismo estado es idempotente). Los errores devuelven un
JSON `{ "codigo", "mensaje", "detalles": [...] }`.
Todo error de solicitud es `400` con ese mismo JSON, incluso un `Content-Type` distinto de
`application/json` (el enunciado pide 400, no el 415 por defecto de Spring) o un id no numérico
(`/api/productos/abc`).

### Publicador — `http://localhost:5004`

| Método | Ruta                   | Éxito                                                                 | Error |
|--------|------------------------|-----------------------------------------------------------------------|-------|
| `GET`  | `/api/publicaciones`   | `200` órdenes que el publicador publicó (`ordenId`, `ultimaPublicacion`, `veces`) | –     |

El registro vive en memoria (se reinicia con el servicio).

### Procesamiento — `http://localhost:5003`

| Método | Ruta                       | Éxito                                                        | Error               |
|--------|----------------------------|--------------------------------------------------------------|---------------------|
| `GET`  | `/api/facturas`            | `200` lista de facturas (`?ordenId=` filtra por orden)       | –                   |
| `GET`  | `/api/facturas/{id}`       | `200` factura                                                | `404` no encontrada |
| `GET`  | `/api/procesamientos`      | `200` órdenes procesadas y su resultado (`Ready to Delivery` / `No Stock`) | –     |

Ejemplo de factura:

```json
{
  "id": 1, "ordenId": 1, "fecha": "2026-06-21T14:30:05-03:00",
  "email": "cliente@email.com", "direccionEnvio": "Av. Italia 3333, Maldonado", "telefono": "+59899111222",
  "items": [
    { "productoId": 1, "descripcion": "Notebook Lenovo ThinkPad", "cantidad": 2, "precioUnitario": 1250.50, "subtotal": 2501.00 },
    { "productoId": 2, "descripcion": "Mouse Logitech MX Master 3", "cantidad": 5, "precioUnitario": 99.90, "subtotal": 499.50 }
  ],
  "total": 3000.50
}
```

## Estructura del repositorio

```
.
├── openapi.yaml                  # Especificación OpenAPI 3.0 (Productos + Órdenes + Publicador + Procesamiento)
├── docker-compose.yml            # Orquesta todo en contenedores (4 apps + 3 MySQL + Mosquitto)
├── MER_Laboratorio_TAIS.pdf      # Diagrama MER actualizado (partes 2 y 3)
├── levantar.ps1 / levantar.cmd   # Levanta todo lo necesario para probar en Windows (Docker, imágenes, servicios, pruebas)
├── levantar.sh                   # Lo mismo para Linux y macOS
├── test_apis.sh                  # Script de pruebas con curl (todas las APIs y el flujo por MQTT)
├── README.md
├── mosquitto/
│   └── mosquitto.conf            # Configuración del broker (listener 1883, persistencia)
│
├── productos/                    # Microservicio de Productos
│   ├── pom.xml · Dockerfile · .dockerignore · compose.yaml · mvnw / mvnw.cmd
│   └── src/main/java/uy/edu/utec/taller/productos/
│       ├── ProductosApplication.java
│       ├── config/       # SecurityConfig (permitAll), DataInitializer (datos de ejemplo)
│       ├── controller/   # ProductoController
│       ├── dto/          # ProductoDTO, ProductoCreateDTO, ProductoPatchDTO, ProductoCreadoDTO, MensajeDTO, ErrorDTO
│       ├── exception/    # ProductoNoEncontradoException, GlobalExceptionHandler
│       ├── model/        # Producto (entidad JPA, imágenes como colección)
│       ├── repository/   # ProductoRepository
│       ├── service/      # ProductoService
│       └── validation/   # @AlMenosUnCampoPresente (validación del PATCH)
│
├── ordenes/                      # Microservicio de Órdenes (sin dependencias de mensajería)
│   ├── pom.xml · Dockerfile · .dockerignore · compose.yaml
│   └── src/main/java/uy/edu/utec/taller/ordenes/
│       ├── client/       # ProductoClient (RestClient) + dto/ProductoResponse
│       ├── config/       # SecurityConfig, DataInitializer
│       ├── controller/   # OrdenController
│       ├── dto/          # OrdenDTO, OrdenDetalleDTO, OrdenCreateDTO, OrdenCreadaDTO, OrdenEstadoUpdateDTO,
│       │                 # MensajeDTO, LineaOrden*DTO, ErrorDTO
│       ├── exception/    # Orden/Producto/Stock/Transicion/Estado…Exception, GlobalExceptionHandler
│       ├── model/        # Orden, LineaOrden, EstadoOrden
│       ├── repository/   # OrdenRepository
│       └── service/      # OrdenService
│
├── publicador/                   # Microservicio Publicador (publica en el broker; sin base de datos)
│   ├── pom.xml · Dockerfile · .dockerignore · mvnw / mvnw.cmd
│   └── src/main/java/uy/edu/utec/taller/publicador/
│       ├── client/       # OrdenClient (GET /api/ordenes?estado=) + dto/OrdenResponse
│       ├── config/       # PublicadorConfig (Clock inyectable)
│       ├── controller/   # PublicacionController (GET /api/publicaciones)
│       ├── dto/          # OrdenMensajeDTO (mensaje MQTT), PublicacionDTO
│       ├── exception/    # ServicioExternoException, MensajeriaException
│       ├── messaging/    # MqttOrdenPublisher (Paho) + PublicadorScheduler
│       ├── model/        # Publicacion (registro en memoria)
│       └── service/      # PublicadorService (qué publicar y cuándo reintentar)
│
└── procesamiento/                # Microservicio de Procesamiento y Facturación (recibe del broker)
    ├── pom.xml · Dockerfile · .dockerignore · compose.yaml (MySQL + Mosquitto para desarrollo)
    └── src/main/java/uy/edu/utec/taller/procesamiento/
        ├── client/       # OrdenClient, ProductoClient (RestClient con timeouts) + dto/
        ├── config/       # SecurityConfig
        ├── controller/   # FacturaController, ProcesamientoController
        ├── dto/          # OrdenMensajeDTO, FacturaDTO, FacturaItemDTO, ProcesamientoDTO, ErrorDTO
        ├── exception/    # ServicioExternoException, FacturaNoEncontradaException, GlobalExceptionHandler
        ├── messaging/    # MqttOrdenSubscriber (suscripción QoS 1, reintentos)
        ├── model/        # Procesamiento, Factura, FacturaItem, ResultadoProcesamiento
        ├── repository/   # ProcesamientoRepository, FacturaRepository
        └── service/      # ProcesamientoService (orquestación), FacturacionService (persistencia)
```

Los tres módulos siguen la misma convención de nombres de paquetes, el mismo estilo de código
(Lombok `@Data`/`@Builder`, inyección por constructor con `@RequiredArgsConstructor`, DTOs con método
estático `fromEntity`) y la misma configuración de build y de tests.

## Requisitos

- **Docker** con Compose v2 (Docker Desktop en Windows y macOS; Docker Engine en Linux).
- Puertos libres: `5001` a `5004` (servicios) y `1883` (Mosquitto).
- Para desarrollo local sin Docker: **JDK 21** (los `pom.xml` fijan `<java.version>21</java.version>`).
  No hace falta instalar Maven: cada módulo trae el wrapper (`./mvnw`).

## Ejecución con Docker (recomendado)

### Con un solo comando

**Windows** (PowerShell):

```powershell
.levantar.ps1              # construye las imágenes y levanta todo
.levantar.ps1 -Reset -Test # desde cero (borra los datos) y ejecuta las pruebas al terminar
```

Si PowerShell bloquea la ejecución de scripts, usá el lanzador: `.levantar.cmd` (acepta las mismas opciones).

**Linux y macOS** (bash):

```bash
bash levantar.sh               # construye las imágenes y levanta todo
bash levantar.sh --reset --test # desde cero (borra los datos) y ejecuta las pruebas al terminar
```

(`./levantar.sh` también sirve si el archivo tiene permiso de ejecución: `chmod +x levantar.sh`.)

Ambos scripts hacen lo mismo: verifican Docker (y lo inician si está apagado en Windows y macOS),
construyen las imágenes una por una, levantan todo, esperan a que los 4 servicios respondan y muestran
las URLs. Opciones:

| Windows             | Linux / macOS     | Efecto                                                                              |
|---------------------|-------------------|-------------------------------------------------------------------------------------|
| `-Reset`            | `--reset`         | Borra contenedores **y datos** antes de levantar (arranca desde cero)               |
| `-NoBuild`          | `--no-build`      | No reconstruye las imágenes (usa las existentes; más rápido si no cambió el código) |
| `-Test`             | `--test`          | Ejecuta `test_apis.sh` al terminar (descarga `jq` a `.tools/` si falta; en Windows necesita Git Bash) |
| `-Down`             | `--down`          | Detiene todo (junto con reset también borra los datos)                              |
| `-TimeoutSegundos`  | `--timeout N`     | Espera máxima a que los servicios respondan (por defecto 300)                       |

### Manualmente

Todos los componentes corren en contenedores. Desde la raíz del repositorio:

```bash
docker compose up --build          # construye las imágenes y levanta todo
```

Arranca `mysql-productos`, `mysql-ordenes`, `mysql-procesamiento`, `mosquitto`, `productos` (`:5001`),
`ordenes` (`:5002`), `publicador` (`:5004`) y `procesamiento` (`:5003`). El `docker-compose.yml` inyecta por variables de entorno
la conexión a cada MySQL (`SPRING_DATASOURCE_*`), las URLs con que los servicios se consumen entre sí
(`PRODUCTOS_API_BASE_URL`, `ORDENES_API_BASE_URL`) y el broker, que usan `publicador` y `procesamiento`
(`MQTT_BROKER_URL=tcp://mosquitto:1883`).

```bash
docker compose down                # detener
docker compose down -v             # detener y borrar también los datos (volúmenes)
```

Prueba rápida del flujo completo:

```bash
# 1. Crear una orden (queda en Created)
curl -X POST http://localhost:5002/api/ordenes -H "Content-Type: application/json" \
  -d '{"email":"cliente@email.com","direccionEnvio":"Av. Italia 3333, Maldonado","telefono":"+59899111222","productos":[{"productoId":1,"cantidad":2}]}'

# 2. En unos segundos el procesamiento la resuelve: Ready to Delivery (o No Stock)
curl http://localhost:5002/api/ordenes/3

# 3. El publicador la publicó en el broker; factura generada y stock descontado
curl http://localhost:5004/api/publicaciones
curl "http://localhost:5003/api/facturas?ordenId=3"
curl http://localhost:5001/api/productos/1

# Ver los mensajes que circulan por el broker
docker compose exec mosquitto mosquitto_sub -t "ordenes/#" -v
```

Al iniciar por primera vez se cargan datos de ejemplo (productos 1 y 2 y dos órdenes). La orden 1
sembrada está en `Created`, por lo que el publicador la toma y se procesa sola (Ready to Delivery), dejando el
stock de los productos 1 y 2 en 13 y 35.

## Desarrollo local (sin contenedores para las apps)

Cada módulo trae un `compose.yaml` con lo que necesita; gracias a `spring-boot-docker-compose`
se levanta automáticamente al iniciar la app. El broker (puerto 1883) lo levanta
`procesamiento/compose.yaml`, así que conviene arrancar ese módulo primero:

```bash
cd procesamiento && ./mvnw spring-boot:run   # levanta MySQL + Mosquitto y se suscribe
cd productos     && ./mvnw spring-boot:run   # en otra terminal
cd ordenes       && ./mvnw spring-boot:run   # en otra terminal
cd publicador    && ./mvnw spring-boot:run   # en otra terminal (no necesita base de datos)
```

## Tests

### Tests unitarios y de integración (MockMvc + H2)

Usan H2 en memoria (perfil de test) y **no requieren Docker ni broker**:

```bash
cd productos      && ./mvnw test
cd ordenes        && ./mvnw test
cd publicador      && ./mvnw test
cd procesamiento  && ./mvnw test
```

Cubren, entre otros: alta sin descuento de stock, cambio de estado (`Ready to Delivery` / `No Stock`,
idempotencia y transiciones inválidas), filtro de órdenes por estado, formato exacto del mensaje MQTT,
publicador (reintentos, broker caído, lote, órdenes caído),
procesamiento con stock / sin stock / producto inexistente / mensaje duplicado / orden ya procesada,
compensación del stock ante fallos, cálculo de la factura y reintentos del suscriptor.

### Script de pruebas de APIs REST con invocaciones (curl)

El script [`test_apis.sh`](test_apis.sh) valida todos los endpoints contra los servicios en
ejecución (Docker o local). Requiere `curl` y, para las aserciones JSON, **`jq`** (sin `jq` las
aserciones JSON se omiten y la suite `procesamiento` no puede ejecutarse).

Suites:
1. **productos (`:5001`)**: listado, alta (201), consulta (200), PUT/PATCH (200) y errores (400, 404).
2. **ordenes (`:5002`)**: listado, alta (201), consulta, detalle y errores (400, 404, 409).
3. **e2e**: producto con stock controlado → orden → el procesamiento (vía MQTT) la deja en
   `Ready to Delivery` y **descuenta el stock** → total en `/detalle` → 409 por stock insuficiente.
4. **procesamiento (parte 3)**: dos órdenes (enviadas en paralelo) que compiten por el mismo stock (una `Ready to Delivery`
   y otra `No Stock`), stock descontado una sola vez, factura con sus ítems y total, orden sin factura
   cuando es `No Stock`, `GET /api/facturas`, `/api/facturas/{id}` (200/404), `/api/procesamientos`,
   `PATCH /api/ordenes/{id}/estado` (200 idempotente / 400 / 404 / 409), que el **publicador**
   registró la publicación (`GET /api/publicaciones`) y **mensaje MQTT duplicado**
   (no descuenta ni factura dos veces; requiere `docker compose` con `mosquitto` en ejecución).

```bash
./test_apis.sh                       # todas las suites
./test_apis.sh --suite procesamiento # solo la parte 3
./test_apis.sh --verbose             # muestra curl, payload y respuesta de cada invocación
./test_apis.sh --help                # opciones (URLs base, timeout de espera WAIT_TIMEOUT, etc.)
```

Como el procesamiento es asincrónico, el script espera (hasta `WAIT_TIMEOUT`, 60 s por defecto) a que
cada orden deje de estar en `Created`.

## Entregables

| Entregable                          | Ubicación                                                        |
|-------------------------------------|------------------------------------------------------------------|
| Especificación OpenAPI (YAML)       | `openapi.yaml`                                                   |
| Diagrama MER actualizado (PDF)      | `MER_Laboratorio_TAIS.pdf`                                       |
| Código fuente                       | `productos/`, `ordenes/`, `publicador/`, `procesamiento/`, `mosquitto/` |
| `docker-compose.yml`                | raíz del repositorio                                             |
| Script de invocaciones (curl)       | [`test_apis.sh`](test_apis.sh)                                   |
