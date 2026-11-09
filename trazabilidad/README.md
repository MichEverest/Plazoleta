# Microservicio Trazabilidad — Plazoleta de comidas

Historial de cambios de estado de los pedidos y reportes de eficiencia. Puerto `8083`. Ver el [README general](../README.md).

## Ejecución

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
./mvnw test
```

En NetBeans 21+ usa *Run* (`nbactions.xml`). Swagger: http://localhost:8083/swagger-ui.html · Consola H2 (solo `local`): http://localhost:8083/h2-console con JDBC URL `jdbc:h2:file:./data/trazabilidad`, usuario `sa`.

Configuración: `services.plazoleta.url` (`PLAZOLETA_URL`, por defecto `http://localhost:8082`) y `JWT_SECRET`. Perfil `prod`: MySQL con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Endpoints

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| POST | `/trazabilidad` | Autenticado | Uso interno de Plazoleta. Guarda una fila por cada cambio de estado |
| GET | `/trazabilidad/pedidos/{idPedido}` | CLIENTE dueño | HU17. Cambios en orden cronológico con `duracionSegundos` en cada estado (`null` en un estado final) |
| GET | `/trazabilidad/eficiencia/pedidos?page&size` | PROPIETARIO | HU18. Tiempo entre PENDIENTE y ENTREGADO por pedido, `promedioSegundos` del restaurante y `superaPromedio` |
| GET | `/trazabilidad/eficiencia/empleados?page&size` | PROPIETARIO | HU18. Ranking por tiempo medio entre EN_PREPARACION y LISTO (solo pedidos entregados) |

El restaurante del propietario se obtiene por Feign con `GET /restaurantes/mio` de Plazoleta, reenviando el token.

## Persistencia

Tabla `trazabilidad`: `id, id_pedido, id_cliente, correo_cliente, id_restaurante, fecha, estado_anterior, estado_nuevo, id_empleado, correo_empleado`. El caso de uso depende solo del puerto `TrazabilidadRepositoryPort`. Para usar MongoDB basta con crear otro adaptador que implemente ese puerto (por ejemplo `TrazabilidadMongoAdapter` con `spring-boot-starter-data-mongodb`), sin tocar `application` ni `domain`.

## Arquitectura

Hexagonal con la misma organización de Autorent; las dependencias van hacia adentro (`infrastructure → application → domain`):

```
com.mich.trazabilidad
├── domain
│   ├── model                 modelos y enums (sin frameworks)
│   └── exception             ExcepcionesDominio: todas las excepciones anidadas en un solo archivo
├── application
│   ├── service               interfaces de entrada (XxxService)
│   ├── usecase               XxxUseCase: toda la lógica y las validaciones (sin frameworks)
│   └── port/output           puertos de salida (XxxRepositoryPort, PlazoletaPort)
└── infrastructure
    ├── adapter/input/rest    controllers, GlobalExceptionHandler y dto/ (un XxxDto por entidad con records anidados)
    ├── adapter/output/...    adaptadores de los puertos (mappers privados toDomain/toEntity dentro del adaptador)
    ├── security              JwtService (token y filtro)
    └── config                BeanConfiguration (registra los UseCase), SecurityConfiguration (JWT, CORS, 401/403)
```
