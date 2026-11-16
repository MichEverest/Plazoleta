# 📖 Guía de uso · Plazoleta de comidas

Guía corta para usar el sistema una vez que está corriendo. Para instalarlo y arrancarlo, mira el [README](README.md).

> Abre **http://localhost:8082**. En la pantalla de inicio hay un botón por cada usuario de prueba: pulsa uno y entras sin escribir nada.
> Todos los usuarios tienen la clave **`Clave123*`**.

---

## 🎭 Los roles

| Rol | Usuario | Qué hace |
|-----|---------|----------|
| 🛡️ **Administrador** | `mich@gmail.com` | Crea propietarios y restaurantes |
| 👩‍🍳 **Propietaria** | `cartethyia@gmail.com` | Maneja su menú, crea empleados y mira la eficiencia de su restaurante |
| 🧑‍🍳 **Empleada** | `mari@gmail.com` | Atiende los pedidos: los asigna, los marca listos y los entrega |
| 🧑 **Cliente** | `jean@gmail.com` | Pide comida, cancela y consulta el historial de sus pedidos |

Cada rol ve una pantalla distinta en http://localhost:8082. Para cambiar de usuario, pulsa **Salir**.

---

## 🍔 El recorrido de un pedido

```
 PENDIENTE  ──▶  EN PREPARACIÓN  ──▶  LISTO  ──▶  ENTREGADO
  (Jean pide)     (Mari se lo asigna)   (llega el PIN)   (Mari pide el PIN)
      │
      └──▶ CANCELADO  (solo mientras está PENDIENTE)
```

### 1. 🧑 Jean hace el pedido

1. Entra como **Jean**.
2. En **Restaurantes** pulsa **Ver menú** en *Sabores de Cartethyia*.
3. Elige la cantidad de cada plato y pulsa **Agregar**. Puedes filtrar por categoría.
4. Revisa **Tu pedido** (el carrito) y pulsa **Enviar pedido**.
5. El pedido aparece en **Mis pedidos** como `PENDIENTE`. Mientras siga así, lo puedes **Cancelar**.

> Un cliente solo puede tener **un pedido en curso** a la vez.

### 2. 🧑‍🍳 Mari lo atiende

1. Entra como **Mari**. Ve los pedidos de su restaurante y puede filtrarlos por estado.
2. Pulsa **Asignarme**: el pedido pasa a `EN PREPARACIÓN`.
3. Cuando la comida está lista, elige una opción:
   - **Marcar listo** → se envía un **PIN por SMS** al celular del cliente. La columna **Aviso** dice `SMS`.
   - **Listo sin SMS** → atajo para avanzar rápido: no se envía nada. La columna **Aviso** dice `Sin SMS`.

### 3. 📱 El PIN

- El cliente recibe un SMS con un código de 6 dígitos.
- Al recoger el pedido, se lo **dice a Mari**.
- Mari lo escribe junto al pedido y pulsa **Entregar**:
  - si coincide → `ENTREGADO` ✅
  - si no coincide o ya venció → error, y el pedido sigue `LISTO`.
- ¿No llegó el SMS o venció el código? Mari pulsa **Reenviar PIN**.
- Si se usó **Listo sin SMS**, el botón es **Entregar sin PIN**. No se pide código y queda registrado que fue sin SMS.

> ¿Los SMS no están configurados? Mira [📱 PIN por SMS en el README](README.md#-pin-por-sms). Mientras tanto, la pantalla de Mari muestra un aviso en rojo y puedes usar **Listo sin SMS**.

### 4. 🧭 Seguimiento

- **Jean** pulsa **Trazabilidad** en un pedido para ver cada cambio de estado: hora, cuánto duró cada paso, quién lo atendió y si el PIN fue **por SMS** o **sin SMS**. También lo ve en http://localhost:8083.
- **Cartethyia** ve en su pantalla (o en http://localhost:8083) el **tiempo promedio** de los pedidos entregados y el **ranking de empleados** por rapidez.

---

## 👩‍🍳 Lo que hace la propietaria

Entra como **Cartethyia**:

- **Crear plato:** nombre, precio, categoría, imagen (URL) y descripción.
- **Mis platos:** cambiar precio y descripción (**Guardar**) o **Desactivar** un plato para que los clientes no lo vean.
- **Crear empleado:** queda vinculado a su restaurante automáticamente. El celular se escribe con 10 dígitos, **sin +57**.
- **Eficiencia de pedidos:** tiempos y ranking de su equipo.

## 🛡️ Lo que hace el administrador

Entra como **Mich**:

- **Crear propietario**, que debe ser mayor de edad.
- **Crear restaurante** para un propietario, usando su id. Cada propietario tiene un solo restaurante.
- En http://localhost:8081 ve la **tabla de todos los usuarios**.
- En http://localhost:8084 ve el **historial de SMS** enviados.

---

## 🖥️ Las otras consolas

| URL | Para qué |
|-----|----------|
| http://localhost:8081 | **Usuarios:** ver tu token JWT (para Swagger), registrar clientes, buscar usuarios por id |
| http://localhost:8083 | **Trazabilidad:** historial de pedidos (cliente), eficiencia y ranking (propietaria) |
| http://localhost:8084 | **Mensajería:** ver si los SMS están configurados, enviar un SMS de prueba, historial (administrador) |

---

## 🔧 Para curiosos: Swagger y la base de datos

### Swagger

Es una página que lista **todas las operaciones de la API** de cada servicio y deja probarlas desde el navegador.

1. Abre http://localhost:8081/swagger-ui.html.
2. Busca `POST /auth/login`, pulsa **Try it out**, escribe `{"correo": "jean@gmail.com", "clave": "Clave123*"}` y pulsa **Execute**.
3. Copia el `token` de la respuesta. También lo encuentras en http://localhost:8081, botón *Copiar token*.
4. Pulsa **Authorize** (arriba a la derecha), pégalo y confirma.
5. Ya puedes probar endpoints. Cada servicio tiene su propio Swagger: cambia el puerto a 8082, 8083 u 8084.

### Base de datos

Abre `http://localhost:8082/h2-console`, o los puertos 8081 y 8083. La conexión ya viene escrita: pulsa **Conectar** y prueba, por ejemplo:

```sql
SELECT ID, ESTADO, SIN_SMS, FECHA FROM PEDIDOS;
```

---

<div align="center">

¿Dudas o ideas? Escríbeme en
[TikTok @Mitsnightw](https://www.tiktok.com/@Mitsnightw) ·
[YouTube @Mitsnightw](https://www.youtube.com/@Mitsnightw) ·
[GitHub MichEverest](https://github.com/MichEverest)

</div>
