# Plazoleta de comidas

Sistema de pedidos para una plazoleta de comidas: 4 microservicios Spring Boot con arquitectura hexagonal y un frontend de prueba en HTML, CSS y JS puro.

## Ejecutar todo con un solo comando

Desde esta carpeta (la raíz, donde está el `pom.xml` padre):

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

Arranca los 4 microservicios a la vez (en paralelo, configurado en `.mvn/maven.config`) y `Ctrl+C` los detiene todos. Cuando veas los 4 mensajes `Started ...Application`, abre **http://localhost:8082** en el navegador: ahí está el frontend.

- **NetBeans 21+:** abre esta carpeta como proyecto Maven y pulsa *Run*.
- **VS Code:** abre esta carpeta y ejecuta el comando en la terminal integrada.
- **Pruebas de los 4 servicios:** `./mvnw test`.

Cada servicio también puede ejecutarse solo, entrando a su carpeta y usando el mismo comando.

| Carpeta | Servicio | Puerto | Base de datos (`local`) |
|---------|----------|--------|-------------------------|
| [`usuarios/`](usuarios/README.md) | Usuarios, roles y login JWT | 8081 | H2 `usuarios/data` |
| [`plazoleta/`](plazoleta/README.md) | Restaurantes, platos, empleados, pedidos **y el frontend** | 8082 | H2 `plazoleta/data` |
| [`trazabilidad/`](trazabilidad/README.md) | Historial de estados y eficiencia | 8083 | H2 `trazabilidad/data` |
| [`mensajeria/`](mensajeria/README.md) | Envío de SMS (en `local` se imprimen en la consola) | 8084 | Sin base de datos |

**Stack:** Java 17, Maven Wrapper, Spring Boot 4.1.2 (Spring Framework 7, Spring Security 7, Jackson 3), Spring Data JPA, H2/MySQL, Spring Cloud OpenFeign 2025.1, jjwt 0.13 y springdoc-openapi 3.2. No hace falta instalar nada aparte del JDK 17 o superior.

Swagger con esquema Bearer JWT: `http://localhost:{puerto}/swagger-ui.html`. Para probar desde Swagger, haz login en Usuarios, copia el `token` y pégalo en *Authorize*.

## Usuarios sembrados

Se crean al arrancar `usuarios` (idempotente, se verifica por correo; desactivable con `seed.enabled=false`). Todos tienen clave `Clave123*` y fecha de nacimiento `1995-01-01`.

| Id | Nombre | Rol | Correo | Clave |
|----|--------|-----|--------|-------|
| 1 | Mich Everest | ADMINISTRADOR | mich@plazoleta.com | Clave123* |
| 2 | Cartheyia Plazoleta | PROPIETARIO | cartheyia@plazoleta.com | Clave123* |
| 3 | Jean Plazoleta | CLIENTE | jean@plazoleta.com | Clave123* |

`plazoleta` siembra solo las categorías ENTRADAS, PLATOS FUERTES, POSTRES y BEBIDAS. No se siembran restaurantes, platos, pedidos ni empleados.

## Frontend

Está dentro del microservicio plazoleta, en `plazoleta/src/main/resources/front/`. Son solo dos archivos: `index.html` (con su JavaScript dentro) y `styles.css`. Plazoleta los sirve en **http://localhost:8082**, así que no hace falta Live Server ni ningún otro programa.

Es una sola página que muestra la vista según el rol del token: login, registro de cliente, administrador, propietario, empleado y cliente.

### Flujo de prueba sugerido

1. **Mich** (admin) crea el restaurante con `idPropietario = 2`.
2. **Cartheyia** (propietario) crea platos y un empleado (por ejemplo `laura@plazoleta.com`).
3. **Jean** (cliente) hace un pedido.
4. **Laura** (empleado) se asigna el pedido y lo marca listo. El PIN aparece en la **terminal donde ejecutaste el comando** (línea de mensajeria) (`SMS para +573000000003: ... PIN ...`). Después entrega el pedido con ese PIN.
5. **Jean** consulta la trazabilidad y **Cartheyia** la eficiencia.

## Seguridad

- **Token:** `POST /auth/login` (Usuarios) emite un JWT HS384 con `sub`=correo, `id` y `rol`, válido por 1 hora (`JWT_EXPIRATION_MINUTES`).
- **Validación:** los 4 servicios validan el token con el mismo `JWT_SECRET`, sin consultar a Usuarios. En `local` tiene un valor por defecto; en `prod` es obligatorio.
- **Identidad:** `idCliente`, `idEmpleado` e `idPropietario` siempre se toman del token. Feign reenvía el header `Authorization` entre servicios.
- **Rutas públicas:** `POST /auth/login`, `POST /usuarios/cliente`, Swagger y `/h2-console` (esta última solo en el perfil `local`).
- **Errores:** todas las respuestas de error tienen la forma `{mensaje, codigo, fecha}`.

## Perfil `prod`

```bash
SPRING_PROFILES_ACTIVE=prod JWT_SECRET=<secreto-de-32+-caracteres> \
DB_URL=jdbc:mysql://host:3306/usuarios DB_USERNAME=... DB_PASSWORD=... ./mvnw spring-boot:run
```

Mensajería en `prod` usa Twilio (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`). Las URLs entre servicios se configuran con `USUARIOS_URL`, `MENSAJERIA_URL`, `TRAZABILIDAD_URL` y `PLAZOLETA_URL`.

## Arquitectura

Hexagonal en cada servicio, con la organización del proyecto Autorent: `infrastructure → application → domain`.

- **`domain`:** modelos y un único `ExcepcionesDominio.java`.
- **`application`:** interfaces `XxxService`, implementaciones `XxxUseCase` con toda la lógica y las validaciones, y puertos de salida `XxxRepositoryPort`.
- **`infrastructure`:** `adapter/input/rest` (controllers y un `XxxDto` por entidad con records anidados), `adapter/output` (persistencia y clientes externos con sus mappers privados), `security` y `config`.

`domain` y `application` no importan Spring, JPA, Feign ni Jackson. Los controllers llaman directamente al `XxxService`.

## Cobertura de historias de usuario

| HU | Historia | Servicio y endpoint | Estado |
|----|----------|---------------------|--------|
| HU1 | Crear propietario | Usuarios `POST /usuarios/propietario` | Completa |
| HU2 | Crear restaurante | Plazoleta `POST /restaurantes` | Completa (*) |
| HU3 | Crear plato | Plazoleta `POST /platos` | Completa |
| HU4 | Modificar plato | Plazoleta `PUT /platos/{id}` | Completa |
| HU5 | Autenticación | Usuarios `POST /auth/login` + autorización por rol | Completa |
| HU6 | Crear empleado | Plazoleta `POST /empleados` → Usuarios `POST /usuarios/empleado` | Completa |
| HU7 | Habilitar/deshabilitar plato | Plazoleta `PATCH /platos/{id}/estado` | Completa |
| HU8 | Crear cuenta cliente | Usuarios `POST /usuarios/cliente` | Completa |
| HU9 | Listar restaurantes | Plazoleta `GET /restaurantes` | Completa |
| HU10 | Listar platos de un restaurante | Plazoleta `GET /restaurantes/{id}/platos` | Completa |
| HU11 | Realizar pedido | Plazoleta `POST /pedidos` | Completa |
| HU12 | Listar pedidos por estado | Plazoleta `GET /pedidos` | Completa |
| HU13 | Asignarse a un pedido | Plazoleta `PATCH /pedidos/{id}/asignar` | Completa (*) |
| HU14 | Notificar pedido listo | Plazoleta `PATCH /pedidos/{id}/listo` → Mensajería | Completa |
| HU15 | Entregar pedido | Plazoleta `PATCH /pedidos/{id}/entregar` | Completa |
| HU16 | Cancelar pedido | Plazoleta `PATCH /pedidos/{id}/cancelar` | Completa |
| HU17 | Consultar trazabilidad | Trazabilidad `GET /trazabilidad/pedidos/{id}` | Completa |
| HU18 | Eficiencia de pedidos | Trazabilidad `GET /trazabilidad/eficiencia/pedidos` y `/empleados` | Completa |

(*) Diferencias de criterio:

- **HU2:** un propietario solo puede tener un restaurante, necesario para que `GET /restaurantes/mio` no sea ambiguo.
- **HU13:** el empleado se asigna un pedido por petición, no un lote de pedidos.
