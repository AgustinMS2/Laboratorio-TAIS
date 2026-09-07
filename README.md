# Laboratorio IISS – Parte 2

Aplicación para dar soporte a la operativa de un local de venta de artículos, que
vende de forma presencial y también recibe pedidos por la Web.

Se implementa con un estilo arquitectónico de **microservicios**: dos servicios
independientes exponen las APIs REST de los recursos **Productos** y **Órdenes**,
cada uno con su propia base de datos MySQL. El microservicio de `ordenes` consume
la API de `productos` (vía HTTP) para validar existencia y stock al registrar una
orden.

## Arquitectura

| Servicio    | Puerto | Base de datos                 | Descripción                              |
|-------------|--------|-------------------------------|------------------------------------------|
| `productos` | `5001` | MySQL (`mysql-productos`)     | Catálogo de productos                    |
| `ordenes`   | `5002` | MySQL (`mysql-ordenes`)       | Registro y consulta de órdenes de compra |

```
navegador / curl
      │
      ├──────────────► productos:5001 ──► mysql-productos:3306
      │                     ▲
      └──► ordenes:5002 ────┘ (GET /api/productos/{id}, PATCH stock)
                │
                └────────────────────────► mysql-ordenes:3306
```

Cada servicio es una aplicación Spring Boot 4 (Java 21) autónoma, con su propio
`pom.xml`, su `Dockerfile` y su base de datos. El `docker-compose.yml` de la raíz
orquesta los cuatro contenedores (2 apps + 2 MySQL).

## APIs REST

La especificación completa está en [`openapi.yaml`](openapi.yaml) (OpenAPI 3.0.3).

### Productos — `http://localhost:5001`

| Método  | Ruta                    | Éxito                          | Error                              |
|---------|-------------------------|--------------------------------|------------------------------------|
| `GET`   | `/api/productos`        | `200` lista de productos       | –                                  |
| `GET`   | `/api/productos/{id}`   | `200` producto                 | `404` no encontrado                |
| `POST`  | `/api/productos`        | `201` + id del producto creado | `400` datos inválidos              |
| `PUT`   | `/api/productos/{id}`   | `200` + mensaje                | `404` no encontrado / `400`        |
| `PATCH` | `/api/productos/{id}`   | `200` + mensaje                | `404` no encontrado / `400`        |

### Órdenes — `http://localhost:5002`

| Método | Ruta                          | Éxito                             | Error                                                                 |
|--------|-------------------------------|-----------------------------------|----------------------------------------------------------------------|
| `GET`  | `/api/ordenes`                | `200` lista de órdenes            | –                                                                    |
| `GET`  | `/api/ordenes/{id}`           | `200` orden (productos = id + cantidad) | `404` no encontrada                                            |
| `GET`  | `/api/ordenes/{id}/detalle`   | `200` orden con el detalle completo de cada producto | `404` no encontrada                             |
| `POST` | `/api/ordenes`                | `201` + id de la orden creada     | `400` datos inválidos / `409` stock insuficiente o producto inexistente / `502` servicio de productos no disponible |

`estado` de una orden: `Created`, `Confirmed`, `Shipped`, `Delivered`, `Cancelled`
(las órdenes se crean en `Created`). Los errores devuelven un JSON
`{ "codigo", "mensaje", "detalles": [...] }`.

## Estructura del repositorio

```
.
├── openapi.yaml            # Especificación OpenAPI 3.0 de todas las APIs (Productos + Órdenes)
├── docker-compose.yml      # Orquesta apps + bases de datos, todo en contenedores
├── README.md
├── Laboratorio 2.pdf       # Diagrama MER + descripción del modelo de datos
│
├── productos/                       # Microservicio de Productos
│   ├── pom.xml
│   ├── Dockerfile                   # Build multi-etapa (Maven+JDK21 → JRE21)
│   ├── .dockerignore
│   ├── compose.yaml                 # Solo MySQL, para desarrollo local con ./mvnw
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/java/uy/edu/utec/taller/productos/
│       │   ├── ProductosApplication.java
│       │   ├── config/       # SecurityConfig (permitAll), DataInitializer (datos de ejemplo)
│       │   ├── controller/   # ProductoController
│       │   ├── dto/          # ProductoDTO, ProductoCreateDTO, ProductoPatchDTO,
│       │   │                 # ProductoCreadoDTO, MensajeDTO, ErrorDTO
│       │   ├── exception/    # ProductoNoEncontradoException, GlobalExceptionHandler
│       │   ├── model/        # Producto (entidad JPA, imágenes como colección)
│       │   ├── repository/   # ProductoRepository (Spring Data JPA)
│       │   ├── service/      # ProductoService (lógica de negocio)
│       │   └── validation/   # @AlMenosUnCampoPresente (validación del PATCH)
│       ├── main/resources/application.yaml
│       └── test/…            # Tests unitarios y de integración (MockMvc + H2)
│
└── ordenes/                         # Microservicio de Órdenes
    ├── pom.xml
    ├── Dockerfile
    ├── .dockerignore
    ├── compose.yaml
    └── src/main/java/uy/edu/utec/taller/ordenes/
        ├── OrdenesApplication.java
        ├── client/           # ProductoClient (RestClient) + dto/ProductoResponse
        ├── config/           # SecurityConfig, DataInitializer
        ├── controller/       # OrdenController
        ├── dto/              # OrdenDTO, OrdenDetalleDTO, OrdenCreateDTO, OrdenCreadaDTO,
        │                     # LineaOrdenDTO, LineaOrdenCreateDTO, LineaOrdenDetalleDTO, ErrorDTO
        ├── exception/        # OrdenNoEncontradaException, ConflictoOrdenException,
        │                     # StockInsuficienteException, ProductosInexistentesException,
        │                     # ProductoServicioException, GlobalExceptionHandler
        ├── model/            # Orden, LineaOrden, EstadoOrden
        ├── repository/       # OrdenRepository
        └── service/          # OrdenService
```

Ambos módulos siguen la misma convención de nombres de paquetes, el mismo estilo
de código (Lombok `@Data`/`@Builder`, inyección por constructor con
`@RequiredArgsConstructor`, DTOs con método estático `fromEntity`) y la misma
configuración de build y de tests.

## Requisitos

- **Docker** / Docker Desktop en ejecución.
- Para desarrollo local sin Docker: **JDK 21** (los `pom.xml` fijan
  `<java.version>21</java.version>`). No hace falta instalar Maven: cada módulo
  trae el wrapper (`./mvnw`).

## Ejecución con Docker (recomendado)

Todos los componentes corren en contenedores. Desde la raíz del repositorio:

```bash
docker compose up --build          # construye las imágenes y levanta todo
```

Esto arranca `mysql-productos`, `mysql-ordenes`, `productos` (`:5001`) y
`ordenes` (`:5002`). El `docker-compose.yml` inyecta por variables de entorno la
conexión a cada MySQL (`SPRING_DATASOURCE_*`) y la URL con la que `ordenes`
consume a `productos` (`PRODUCTOS_API_BASE_URL=http://productos:5001`).

```bash
docker compose down                # detener
docker compose down -v             # detener y borrar también los datos (volúmenes)
```

Prueba rápida:

```bash
curl http://localhost:5001/api/productos
curl http://localhost:5002/api/ordenes
```

## Desarrollo local (sin contenedores para las apps)

Cada módulo trae un `compose.yaml` con solo MySQL; gracias a
`spring-boot-docker-compose` se levanta automáticamente al iniciar la app:

```bash
cd productos && ./mvnw spring-boot:run     # en una terminal
cd ordenes   && ./mvnw spring-boot:run     # en otra
```

## Tests

### Tests unitarios y de integración (MockMvc + H2)

Los tests usan H2 en memoria (perfil de test), no requieren Docker:

```bash
cd productos && ./mvnw test
cd ordenes   && ./mvnw test
```

### Script de pruebas de APIs REST con invocaciones (curl)

Se incluye el script automatizado [`test_apis.sh`](test_apis.sh) que valida todos los endpoints de las APIs REST contra los microservicios en ejecución (Docker o local).

Cubre:
1. **Microservicio Productos (`:5001`)**: Listado, creación (201), consulta por ID (200), reemplazo total (PUT 200), actualización parcial (PATCH 200), validaciones y errores (400, 404).
2. **Microservicio Órdenes (`:5002`)**: Listado, creación (201), consulta por ID (200), detalle enriquecido (200), validaciones y errores (400, 404, conflicto 409 por stock o producto inexistente).
3. **Flujo E2E de integración**: Creación de producto con stock controlado, creación de orden, validación de descuento automático de stock en Productos, verificación de cálculo de totales en `/detalle`, control de stock insuficiente (409) e invariancia del stock ante fallo.

```bash
# Ejecutar todas las pruebas
./test_apis.sh

# Modo verbose (muestra curl exacto, payload enviado y respuesta HTTP)
./test_apis.sh --verbose

# Ejecutar una suite específica (productos, ordenes o e2e)
./test_apis.sh --suite e2e

# Ver todas las opciones y parámetros
./test_apis.sh --help
```

## Entregables (Laboratorio – Parte 2)

| Entregable                    | Ubicación                                  |
|-------------------------------|--------------------------------------------|
| Especificación OpenAPI (YAML) | `openapi.yaml`                             |
| Diagrama MER (PDF)            | `Laboratorio 2.pdf`                        |
| Código fuente                 | `productos/`, `ordenes/`                   |
| `docker-compose.yml`          | raíz del repositorio                       |
| Script de invocaciones (curl) | [`test_apis.sh`](test_apis.sh)             |

