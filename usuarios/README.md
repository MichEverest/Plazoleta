# Microservicio Usuarios — Plazoleta de comidas

Microservicio de usuarios, roles y autenticación JWT del sistema *Plazoleta de comidas*. Puerto `8081`. Ver el [README general](../README.md).

## Stack

- Java 17, Maven (wrapper incluido, no requiere instalar Maven)
- Spring Boot 4.1.2 (Spring Framework 7, Spring Security 7, Jackson 3)
- Spring Data JPA, H2 (local) / MySQL (prod)
- springdoc-openapi 3 (Swagger UI), jjwt

## Ejecución

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
./mvnw test                     # pruebas unitarias
```

- **VS Code**: abrir la carpeta y ejecutar `UsuariosApplication` o usar los comandos anteriores en la terminal.
- **NetBeans 21+**: abrir como proyecto Maven y usar *Run* (`nbactions.xml` ejecuta `spring-boot:run`).

Puerto: `8081`.

| Recurso     | URL                                      |
|-------------|------------------------------------------|
| Consola web | http://localhost:8081                    |
| Swagger UI  | http://localhost:8081/swagger-ui.html    |
| OpenAPI     | http://localhost:8081/v3/api-docs        |
| Consola H2  | http://localhost:8081/h2-console (solo perfil `local`) |

Consola H2: la conexión ya viene escrita (`jdbc:h2:file:./data/usuarios;AUTO_SERVER=TRUE`, usuario `sa`, sin clave); solo pulsa *Conectar*.

## Perfiles

| Perfil  | Base de datos | Notas |
|---------|---------------|-------|
| `local` (por defecto) | H2 en archivo `./data/usuarios` (con `AUTO_SERVER=TRUE`), `ddl-auto=update` | Consola H2 habilitada |
| `prod`  | MySQL | Variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, opcional `DDL_AUTO` (por defecto `update`) |

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:mysql://localhost:3306/usuarios \
DB_USERNAME=root DB_PASSWORD=secreto ./mvnw spring-boot:run
```

## Configuración

| Propiedad | Por defecto | Descripción |
|-----------|-------------|-------------|
| `cors.allowed-origins` | puertos 8081 a 8084 (las 4 consolas), 5500 y 8383 | Orígenes CORS separados por coma |
| `seed.enabled` | `true` | Inserta roles y usuarios iniciales si no existen |
| `seed.celular-para-clientes` | `CELULAR_CLIENTES` o `3023066544` (solo `local`) | Celular que se asigna a todos los clientes en cada arranque, para que los PIN de prueba lleguen a un solo teléfono. Vacío = cada cliente conserva el suyo. El personal sembrado usa `3022249736` |
| `jwt.secret` | `JWT_SECRET` (con valor por defecto solo en `local`) | Secreto HMAC compartido por los 4 servicios |
| `jwt.expiration-minutes` | `JWT_EXPIRATION_MINUTES` o `60` | Vigencia del token |

## Usuarios iniciales (seeds)

Se insertan al arrancar si no existen. Se buscan por número de documento: si el usuario ya existe con otro nombre o correo (por ejemplo, de una versión anterior con `@plazoleta.com`), se corrige. Todos con clave `Clave123*` y fecha de nacimiento `1995-01-01`.

| Id | Nombre | Rol | Correo | Documento | Celular | Clave |
|----|--------|-----|--------|-----------|---------|-------|
| 1 | Mich Everest | ADMINISTRADOR | mich@gmail.com | 1000000001 | 3022249736 | Clave123* |
| 2 | Cartethyia Plazoleta | PROPIETARIO | cartethyia@gmail.com | 1000000002 | 3022249736 | Clave123* |
| 3 | Jean Plazoleta | CLIENTE | jean@gmail.com | 1000000003 | 3023066544 | Clave123* |
| 4 | Mari Plazoleta | EMPLEADO | mari@gmail.com | 1000000004 | 3022249736 | Clave123* |

Los ids 1 a 4 se garantizan sobre una base de datos vacía. Plazoleta usa el id 2 (Cartethyia) como dueña del restaurante sembrado y el 4 (Mari) como su empleada.

## Endpoints

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| POST | `/auth/login` | Público | HU5. Recibe `{correo, clave}` y responde `{token, id, rol}`. Claims: `sub`=correo, `id`, `rol` |
| POST | `/usuarios/propietario` | ADMINISTRADOR | HU1. Crea un propietario mayor de edad. Responde `201 {"id": n}` |
| POST | `/usuarios/empleado` | PROPIETARIO | HU6. Crea un empleado (rol asignado por el sistema). Lo invoca Plazoleta |
| POST | `/usuarios/cliente` | Público | HU8. Registro de cliente (rol asignado por el sistema) |
| GET | `/usuarios` | ADMINISTRADOR | Lista todos los usuarios ordenados por id (la usa la consola web) |
| GET | `/usuarios/{id}` | Autenticado | `{id, nombre, correo, celular, rol}`. Lo usan los otros servicios |

El celular es colombiano: 10 dígitos que empiezan por 3, **sin +57** (ej. `3023066544`). Los celulares guardados con `+57` por versiones anteriores se corrigen solos al arrancar.

Ejemplo de cuerpo de `/usuarios/propietario` (empleado y cliente usan los mismos campos sin `fechaNacimiento`):

```json
{
  "nombre": "Ana",
  "apellido": "Gomez",
  "numeroDocumento": "1234567890",
  "celular": "3005698325",
  "fechaNacimiento": "1990-05-20",
  "correo": "ana@gmail.com",
  "clave": "Clave123*"
}
```

## Consola web

`src/main/resources/front/` (`index.html` y `styles.css`), servida en http://localhost:8081:

- Sin sesión: login, botones *Entrar* con los usuarios de prueba y registro de cliente (HU8).
- Con sesión: datos del token (con botón para copiarlo y pegarlo en Swagger) y consulta de usuario por id.
- ADMINISTRADOR: tabla de usuarios registrados y creación de propietarios (HU1).
- PROPIETARIO: creación directa de un usuario empleado (sin vincularlo a un restaurante; para eso está la consola de Plazoleta).

## Errores

Todas las respuestas de error tienen la forma:

```json
{ "mensaje": "El correo no tiene una estructura válida", "codigo": 400, "fecha": "2026-11-08T19:58:43.220" }
```

| Código | Causa |
|--------|-------|
| 400 | Validación de datos o cuerpo inválido |
| 401 | No autenticado, token inválido o credenciales incorrectas |
| 403 | Sin permisos |
| 404 | Recurso no encontrado |
| 409 | Correo o documento ya registrado |

## Arquitectura

Hexagonal con la misma organización de Autorent; las dependencias van hacia adentro (`infrastructure → application → domain`):

```
com.mich.usuarios
├── domain
│   ├── model                 modelos y enums (sin frameworks)
│   └── exception             ExcepcionesDominio: todas las excepciones anidadas en un solo archivo
├── application
│   ├── service               interfaces de entrada (XxxService)
│   ├── usecase               XxxUseCase: toda la lógica y las validaciones (sin frameworks)
│   └── port/output           puertos de salida (XxxRepositoryPort, SeguridadPort)
└── infrastructure
    ├── adapter/input/rest    controllers, GlobalExceptionHandler y dto/ (un XxxDto por entidad con records anidados)
    ├── adapter/output/...    adaptadores de los puertos (mappers privados toDomain/toEntity dentro del adaptador)
    ├── security              JwtService (token y filtro)
    └── config                BeanConfiguration (registra los UseCase), SecurityConfiguration (JWT, CORS, 401/403), DataInitializer (seeds)
```

