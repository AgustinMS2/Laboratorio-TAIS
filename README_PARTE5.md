# Laboratorio - Parte 5: Simulación de Cluster con Docker Swarm

Este documento detalla la implementación y resolución de la **Parte 5 del Laboratorio de Arquitectura de Software (TAIS / IISS)**, cumpliendo con todos los requerimientos y entregables solicitados.

---

## 1. Arquitectura del Cluster y Solución Técnica

### ¿Por qué Docker Swarm y Routing Mesh?
Para simular el cluster de 3 instancias del microservicio `ordenes` (API REST: `POST /api/ordenes`), se investigó y configuró **Docker Swarm**.

Docker Swarm incluye internamente un mecanismo de **Ingress Routing Mesh** basado en **IPVS (IP Virtual Server)** a nivel del kernel de Linux:
- Se declara en `docker-compose.yml` que el servicio `ordenes` posee `deploy.replicas: 3`.
- Docker Swarm asigna una **VIP (Virtual IP)** interna al servicio en la red `overlay`.
- Todas las réplicas exponen de manera unificada el puerto `5002` hacia el host y dentro de la red del stack.
- Cada nueva conexión entrante (por ejemplo, desde llamadas `curl` o clientes externos) es distribuida automáticamente entre las 3 réplicas mediante **Round-Robin** a nivel de transporte (L4).
- Por este motivo, **no fue necesario utilizar ni configurar Nginx**, cumpliendo con la indicación de la consigna: *"nginx.conf (en caso que haya sido necesario, sino fue necesario utilizar no se entrega)"*.

### Identificación de Instancias en Logs y Respuestas
Para facilitar la verificación requerida en los puntos 4, 5 y 6:
1. En [OrdenController.java](ordenes/src/main/java/uy/edu/utec/taller/ordenes/controller/OrdenController.java), se agregó registro estructurado con `@Slf4j`:
   ```java
   log.info("[CLUSTER-ORDENES] [Instancia: {}] Solicitud POST /api/ordenes recibida para email: {}", instancia, ordenCreate.getEmail());
   log.info("[CLUSTER-ORDENES] [Instancia: {}] Orden #{} creada exitosamente", instancia, creada.getId());
   ```
2. Además, se añade la cabecera HTTP de respuesta `X-Instance-Id: <ID_DE_CONTENEDOR>` para auditar en tiempo real qué réplica procesó cada orden individual.

---

## 2. Entregables del Laboratorio

Tal como solicita el enunciado, la entrega contiene:

| Entregable | Archivo | Descripción |
|---|---|---|
| **1. docker-compose.yml** | [`docker-compose.yml`](docker-compose.yml) | Compose v3 / Swarm con `deploy.replicas: 3`, políticas de reinicio y red `overlay` `attachable: true`. |
| **2. nginx.conf** | *(No necesario)* | No se incluye debido a que Docker Swarm Routing Mesh resuelve de forma nativa el balanceo y la alta disponibilidad. |
| **3. Script para levantar** | [`levantar_cluster.sh`](levantar_cluster.sh) | Inicia Docker Swarm si está inactivo, compila las imágenes y despliega el stack `laboratorio-tais` esperando convergencia (`3/3`). |
| **4. Script de verificación** | [`verificar_cluster.sh`](verificar_cluster.sh) | Realiza las pruebas automatizadas de balanceo (curls), baja de 1ra instancia (escala a 2), baja de 2da instancia (escala a 1), inspección de logs y restauración a 3 instancias. |

---

## 3. Instrucciones de Ejecución

### Paso 1: Levantar el cluster
Ejecuta el script:
```bash
bash levantar_cluster.sh
```
El script realizará:
1. Verificación de Docker y activación de Swarm (`docker swarm init`).
2. Compilación de imágenes locales (`docker compose build`).
3. Despliegue del stack en Swarm (`docker stack deploy -c docker-compose.yml laboratorio-tais`).
4. Espera hasta que las 3 réplicas de `ordenes` estén corriendo (`3/3`).
5. Confirmación de respuesta HTTP en `http://localhost:5002/api/ordenes`.

### Paso 2: Ejecutar las pruebas de balanceo y tolerancia a fallos
Con el cluster activo, ejecuta:
```bash
bash verificar_cluster.sh
```
El script ejecutará automáticamente las 3 etapas del enunciado:
- **Punto 4**: Envía 9 peticiones `POST /api/ordenes` y muestra cómo se balancean equitativamente entre las 3 réplicas (3 a cada una) y muestra los logs de cada instancia.
- **Punto 5**: Escala el servicio a 2 réplicas (`docker service scale laboratorio-tais_ordenes=2`). Envía 6 peticiones y valida que las 2 instancias restantes siguen operando normalmente (3 a cada una) y muestra los logs.
- **Punto 6**: Escala el servicio a 1 réplica (`docker service scale laboratorio-tais_ordenes=1`). Envía 4 peticiones y valida que la única instancia activa sigue respondiendo sin pérdida de solicitudes y muestra los logs.
- **Restauración**: Vuelve a escalar automáticamente a 3 réplicas para dejar el cluster preparado para la defensa.

### Comandos útiles para la Defensa en Clase
- Ver servicios activos y réplicas:
  ```bash
  docker service ls
  ```
- Ver contenedores de cada réplica:
  ```bash
  docker stack ps laboratorio-tais --filter "name=laboratorio-tais_ordenes" --filter "desired-state=running"
  ```
- Ver logs en vivo de todas las réplicas de órdenes:
  ```bash
  docker service logs -f laboratorio-tais_ordenes
  ```
- Escalar manualmente las réplicas:
  ```bash
  docker service scale laboratorio-tais_ordenes=2
  docker service scale laboratorio-tais_ordenes=1
  docker service scale laboratorio-tais_ordenes=3
  ```
- Detener el cluster por completo:
  ```bash
  bash levantar_cluster.sh --down
  ```
