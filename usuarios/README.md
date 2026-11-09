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
| Swagger UI  | http://localhost:8081/swagger-ui.html    |
| OpenAPI     | http://localhost:8081/v3/api-docs        |
| Consola H2  | http://localhost:8081/h2-console (solo perfil `local`) |

Consola H2: JDBC URL `jdbc:h2:file:./data/usuarios`, usuario `sa`, sin clave.

## Perfiles

| Perfil  | Base de datos | Notas |
|---------|---------------|-------|
| `local` (por defecto) | H2 en archivo `./data/usuarios`, `ddl-auto=update` | Consola H2 habilitada |
| `prod`  | MySQL | Variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, opcional `DDL_AUTO` (por defecto `update`) |

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:mysql://localhost:3306/usuarios \
DB_USERNAME=root DB_PASSWORD=secreto ./mvnw spring-boot:run
```

## Configuración

| Propiedad | Por defecto | Descripción |
|-----------|-------------|-------------|
| `cors.allowed-origins` | `http://localhost:5500,http://127.0.0.1:5500,http://localhost:8383,http://127.0.0.1:8383` | Orígenes CORS separados por coma |
| `seed.enabled` | `true` | Inserta roles y usuarios iniciales si no existen |
| `jwt.secret` | `JWT_SECRET` (con valor por defecto solo en `local`) | Secreto HMAC compartido por los 4 servicios |
| `jwt.expiration-minutes` | `JWT_EXPIRATION_MINUTES` o `60` | Vigencia del token |

## Usuarios iniciales (seeds)

Se insertan al arrancar si no existen (se verifica por correo). Todos con clave `Clave123*` y fecha de nacimiento `1995-01-01`.

| Id | Nombre | Rol | Correo | Documento | Celular | Clave |
|----|--------|-----|--------|-----------|---------|-------|
| 1 | Mich Everest | ADMINISTRADOR | mich@plazoleta.com | 1000000001 | +573000000001 | Clave123* |
| 2 | Cartheyia Plazoleta | PROPIETARIO | cartheyia@plazoleta.com | 1000000002 | +573000000002 | Clave123* |
| 3 | Jean Plazoleta | CLIENTE | jean@plazoleta.com | 1000000003 | +573000000003 | Clave123* |

Los ids 1, 2 y 3 se garantizan sobre una base de datos vacía. Roles: `ADMINISTRADOR` (1), `PROPIETARIO` (2), `EMPLEADO` (3), `CLIENTE` (4).

## Endpoints

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| POST | `/auth/login` | Público | HU5. Recibe `{correo, clave}` y responde `{token, id, rol}`. Claims: `sub`=correo, `id`, `rol` |
| POST | `/usuarios/propietario` | ADMINISTRADOR | HU1. Crea un propietario mayor de edad. Responde `201 {"id": n}` |
| POST | `/usuarios/empleado` | PROPIETARIO | HU6. Crea un empleado (rol asignado por el sistema). Lo invoca Plazoleta |
| POST | `/usuarios/cliente` | Público | HU8. Registro de cliente (rol asignado por el sistema) |
| GET | `/usuarios/{id}` | Autenticado | `{id, nombre, correo, celular, rol}`. Lo usan los otros servicios |

Ejemplo de cuerpo de `/usuarios/propietario` (empleado y cliente usan los mismos campos sin `fechaNacimiento`):

```json
{
  "nombre": "Ana",
  "apellido": "Gomez",
  "numeroDocumento": "1234567890",
  "celular": "+573005698325",
  "fechaNacimiento": "1990-05-20",
  "correo": "ana@correo.com",
  "clave": "Clave123*"
}
```

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

