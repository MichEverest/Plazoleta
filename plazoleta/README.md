# Microservicio Plazoleta — Plazoleta de comidas

Restaurantes, categorías, platos, empleados y pedidos. Puerto `8082`. Ver el [README general](../README.md).

## Ejecución

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
./mvnw test
```

En NetBeans 21+ usa *Run* (`nbactions.xml`). Swagger: http://localhost:8082/swagger-ui.html · Consola H2 (solo `local`): http://localhost:8082/h2-console (la conexión ya viene escrita: `jdbc:h2:file:./data/plazoleta;AUTO_SERVER=TRUE`, usuario `sa`, sin clave; solo pulsa *Conectar*).

Depende de Usuarios (8081), Mensajería (8084) y Trazabilidad (8083). Todas las llamadas se hacen por Feign y reenvían el header `Authorization`.

## Configuración

| Propiedad | Variable / defecto | Descripción |
|-----------|--------------------|-------------|
| `services.usuarios.url` | `USUARIOS_URL` / `http://localhost:8081` | URL de Usuarios |
| `services.mensajeria.url` | `MENSAJERIA_URL` / `http://localhost:8084` | URL de Mensajería |
| `services.trazabilidad.url` | `TRAZABILIDAD_URL` / `http://localhost:8083` | URL de Trazabilidad |
| `jwt.secret` | `JWT_SECRET` | Mismo secreto que Usuarios |
| `seed.enabled` | `true` | Siembra los datos de prueba (ver abajo) |
| `cors.allowed-origins` | puertos 8081 a 8084, 5500 y 8383 | Orígenes CORS |

Perfil `prod`: MySQL con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `DDL_AUTO` opcional.

## Datos iniciales (seeds)

Al arrancar, cada bloque se crea solo si aún no existe:

- **Categorías:** ENTRADAS, PLATOS FUERTES, POSTRES y BEBIDAS.
- **Restaurante 1:** *Sabores de Cartethyia* (NIT `900123456`), propietaria Cartethyia (id 2), con la empleada Mari (id 4) vinculada.

- **5 platos exóticos** (solo si el restaurante no tiene platos):

| Plato | Categoría | Precio |
|-------|-----------|--------|
| Wagyu A5 con trufa negra | PLATOS FUERTES | $185.000 |
| Ceviche de langosta del Pacífico | ENTRADAS | $68.000 |
| Hormigas culonas de Santander | ENTRADAS | $42.000 |
| Esfera de chocolate amazónico y oro | POSTRES | $55.000 |
| Cóctel de viche y lulo | BEBIDAS | $38.000 |

No se siembran pedidos: los crean los usuarios.

El teléfono del restaurante lleva 10 dígitos sin +57 (el sembrado es `3022249736`, el de la empresa; aparece en el SMS del PIN); los guardados con `+57` se corrigen al arrancar.

El logo del restaurante y las imágenes de los platos son SVG locales en `src/main/resources/front/img/`.

## Consola web

`src/main/resources/front/` (`index.html`, `styles.css` e `img/`), servida en http://localhost:8082.

## Endpoints

| Método | Ruta | Acceso | HU |
|--------|------|--------|----|
| POST | `/restaurantes` | ADMINISTRADOR | HU2. Valida por Feign que `idPropietario` exista y tenga rol PROPIETARIO |
| GET | `/restaurantes/mio` | PROPIETARIO | Restaurante del propietario del token (lo usa Trazabilidad) |
| GET | `/restaurantes?page&size` | CLIENTE | HU9. Orden alfabético; devuelve `id, nombre, urlLogo` |
| GET | `/categorias` | CLIENTE, PROPIETARIO | Categorías de platos |
| POST | `/platos` | PROPIETARIO dueño | HU3. Queda `activo=true` |
| PUT | `/platos/{id}` | PROPIETARIO dueño | HU4. Solo `precio` y `descripcion` |
| PATCH | `/platos/{id}/estado` | PROPIETARIO dueño | HU7. `{activo}`: borrado lógico |
| GET | `/restaurantes/{id}/platos?categoriaId&page&size` | CLIENTE, PROPIETARIO | HU10. El cliente solo ve platos activos; el propietario ve todos los de su restaurante |
| POST | `/empleados` | PROPIETARIO | HU6. Crea el usuario en Usuarios y lo vincula al restaurante del propietario |
| POST | `/pedidos` | CLIENTE | HU11. Platos activos del mismo restaurante; sin otro pedido en curso. Queda PENDIENTE |
| GET | `/pedidos/mis-pedidos?page&size` | CLIENTE | Pedidos del cliente con estado y platos |
| GET | `/pedidos?estado&page&size` | EMPLEADO | HU12. Solo los del restaurante del empleado |
| PATCH | `/pedidos/{id}/asignar` | EMPLEADO | HU13. PENDIENTE → EN_PREPARACION |
| PATCH | `/pedidos/{id}/listo?sinSms=false` | EMPLEADO | HU14. Envía el PIN por SMS (Mensajería) y solo si sale bien pasa EN_PREPARACION → LISTO. Con `sinSms=true` es el atajo sin SMS: no hay PIN y queda registrado (`notificacion: SIN_SMS` y `conSms=false` en trazabilidad) |
| PATCH | `/pedidos/{id}/reenviar-pin` | EMPLEADO | Reenvía por SMS el mismo PIN de un pedido LISTO (no cambia el estado) |
| PATCH | `/pedidos/{id}/entregar` | EMPLEADO | HU15. `{pin}`. LISTO → ENTREGADO. Si el código lo genera Twilio Verify se valida con Mensajería; si el pedido se marcó sin SMS, no se pide PIN |
| PATCH | `/pedidos/{id}/cancelar` | CLIENTE dueño | HU16. Solo si está PENDIENTE |

Cada cambio de estado de un pedido (creación, asignar, listo, entregar y cancelar) se registra en Trazabilidad. Las identidades (`idCliente`, `idEmpleado`, `idPropietario`) siempre se toman del token. Los listados responden `{content, page, size, totalElements, totalPages}`.

## Arquitectura

Hexagonal con la misma organización de Autorent; las dependencias van hacia adentro (`infrastructure → application → domain`):

```
com.mich.plazoleta
├── domain
│   ├── model                 modelos y enums (sin frameworks)
│   └── exception             ExcepcionesDominio: todas las excepciones anidadas en un solo archivo
├── application
│   ├── service               interfaces de entrada (XxxService)
│   ├── usecase               XxxUseCase: toda la lógica y las validaciones (sin frameworks)
│   └── port/output           puertos de salida (XxxRepositoryPort, ServiciosExternosPort para Usuarios, Mensajería y Trazabilidad)
└── infrastructure
    ├── adapter/input/rest    controllers, GlobalExceptionHandler y dto/ (un XxxDto por entidad con records anidados)
    ├── adapter/output/...    adaptadores de los puertos (mappers privados toDomain/toEntity dentro del adaptador)
    ├── security              JwtService (token y filtro)
    └── config                BeanConfiguration (registra los UseCase), SecurityConfiguration (JWT, CORS, 401/403), DataInitializer (seeds)
```

Los repositorios JPA están anidados en su adaptador de persistencia y los clientes Feign en `ServiciosExternosAdapter` (activados con `considerNestedRepositories` y `@EnableFeignClients(clients = ...)`). Los empleados de un restaurante se guardan en `restaurante_empleado` como `@ElementCollection` de `RestauranteEntity`. Las transacciones (`@Transactional`) se declaran en los controllers, porque el UseCase no depende de Spring.

