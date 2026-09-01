# Laboratorio IISS – Parte 2

Aplicación para dar soporte a la operativa de un local de venta de artículos, que
vende de forma presencial y también recibe pedidos por la Web.

Se implementa con un estilo arquitectónico de **microservicios**: dos servicios
independientes exponen las APIs REST de los recursos **Productos** y **Órdenes**.

## Arquitectura

| Servicio    | Puerto | Base de datos          | Descripción                              |
|-------------|--------|------------------------|------------------------------------------|
| `productos` | `5001` | MySQL (`mydatabase`)   | Catálogo de productos                    |
| `ordenes`   | `5002` | MySQL (`mydatabase`)   | Registro y consulta de órdenes de compra |

Cada servicio es una aplicación Spring Boot autónoma, con su propio `pom.xml`,
su `compose.yaml` (MySQL para desarrollo) y su base de datos.

## Estructura del repositorio

```
.
├── openapi.yaml            # Especificación OpenAPI 3.0 de todas las APIs (Productos + Órdenes)
├── README.md
├── productos/              # Microservicio de Productos
│   ├── pom.xml
│   ├── compose.yaml        # MySQL para desarrollo (Docker Compose support de Spring Boot)
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/java/uy/edu/utec/taller/productos/
│       │   ├── ProductosApplication.java
│       │   ├── config/       # SecurityConfig, DataInitializer (datos de ejemplo)
│       │   ├── controller/   # ProductoController  (endpoints REST)
│       │   ├── dto/          # ProductoDTO         (contrato de la API)
│       │   ├── model/        # Producto            (entidad JPA)
│       │   ├── repository/   # ProductoRepository  (Spring Data JPA)
│       │   └── service/      # ProductoService     (lógica de negocio)
│       ├── main/resources/application.yaml
│       └── test/…            # Tests unitarios y de integración (MockMvc + H2)
└── ordenes/               # Microservicio de Órdenes — MISMA estructura de paquetes
    └── src/main/java/uy/edu/utec/taller/ordenes/
        ├── OrdenesApplication.java
        ├── config/           # SecurityConfig, DataInitializer
        ├── controller/       # OrdenController
        ├── dto/              # OrdenDTO, LineaOrdenDTO
        ├── model/            # Orden, LineaOrden, EstadoOrden
        ├── repository/       # OrdenRepository
        └── service/          # OrdenService
```

Ambos módulos siguen la misma convención de nombres de paquetes
(`config`, `controller`, `dto`, `model`, `repository`, `service`), el mismo estilo
de código (Lombok `@Data`/`@Builder`, inyección por constructor con
`@RequiredArgsConstructor`, DTOs con método estático `fromEntity`) y la misma
configuración de build y de tests.

## Requisitos

- **JDK 21** (los `pom.xml` fijan `<java.version>21</java.version>`).
- **Docker** / Docker Desktop en ejecución (para la base de datos MySQL).
- No hace falta instalar Maven: cada módulo trae el wrapper (`./mvnw`).