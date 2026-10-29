# Especificación de base de datos — Sistema de Reservas (Backend, Etapa 2)

Motor objetivo: **Oracle Database Express Edition 11g Release 2 (11.2.0)**, Windows 64 bits, administrado con SQL Developer.
Todo el SQL de este documento y del script es compatible con 11.2: sin `IDENTITY`, sin `FETCH FIRST`/`OFFSET`, sin `BOOLEAN` nativo, sin JSON. Se usan secuencias, `NUMBER(1)` para banderas y, si hiciera falta, `ROWNUM`.

Fuente de verdad para esta especificación: las entidades en `protocol/src/main/java/una/eif206/model/*` y las reglas en `backend/src/main/java/una/eif206/service/*Service.java` (lógica del P1, aún sobre `Data`/XML en el momento de escribir este documento).

---

## 1. Resumen de tablas

| Tabla | Rol | De qué entidad sale |
|---|---|---|
| `USUARIO` | Admin + Funcionario en una sola tabla, discriminada por `ROL` | `Persona` + `Usuario` + `Admin` + `Funcionario` |
| `CATEGORIA_RECURSO` | Catálogo de categorías | `CategoriaRecurso` |
| `RECURSO` | Recursos reservables | `Recurso` |
| `RESERVA` | Reservas | `Reserva` (excepto la lista de recursos) |
| `RESERVA_RECURSO` | Relación N:M reserva↔recurso | `Reserva.recursos` |

### 1.1 Por qué una sola tabla `USUARIO`

`Persona` (abstracta) solo aporta `id`/`nombre`; `Usuario` agrega `clave`/`rol`; `Admin` y `Funcionario` son especializaciones de `Usuario` sin tabla propia en el P1 (viven en listas separadas de `Data`, pero comparten 100% de su forma salvo `departamento`/`telefono`, propios de `Funcionario`). Modelar esto como **tabla única con columna discriminadora `ROL`** (en vez de herencia tabla-por-subclase) es lo recomendado porque:

- `UsuarioService.login(id, clave)` necesita buscar en "todos los usuarios" sin saber de antemano si es Admin o Funcionario — con una tabla es un solo `SELECT`; con dos tablas serían dos consultas o un `UNION`.
- `Reserva.funcionario` es FK a un usuario con rol `FUNCIONARIO`; con una tabla única la FK apunta a `USUARIO(ID)` sin necesidad de una tabla `FUNCIONARIO` separada.
- `departamento`/`telefono` son NULL para `ADMIN` (no aplican), lo cual se modela con columnas nulables + un `CHECK` que exige que solo tengan valor cuando el rol es `FUNCIONARIO` (ver 2.1).

---

## 2. Tablas en detalle

### 2.1 `USUARIO`

| Columna | Tipo Oracle | Nulo | De dónde sale |
|---|---|---|---|
| `ID` | `VARCHAR2(20)` | NOT NULL (PK) | `Persona.id` — para Admin es `"admin"`; para Funcionario es la cédula (ids vistos: `111`, `222`, `504670161`, `50467`, `504`) |
| `NOMBRE` | `VARCHAR2(150)` | NOT NULL | `Persona.nombre` |
| `CLAVE` | `VARCHAR2(100)` | NOT NULL | `Usuario.clave` (el P1 la guarda en texto plano; no se introduce hashing en esta etapa — ver Limitación §6) |
| `ROL` | `VARCHAR2(20)` | NOT NULL | `Usuario.rol` (`UsuarioRol`: `ADMIN`, `FUNCIONARIO`) |
| `DEPARTAMENTO` | `VARCHAR2(100)` | NULL | `Funcionario.departamento` (NULL si `ROL='ADMIN'`) |
| `TELEFONO` | `VARCHAR2(20)` | NULL | `Funcionario.telefono` (NULL si `ROL='ADMIN'`) |

Restricciones:
- `PK_USUARIO (ID)`
- `CK_USUARIO_ROL CHECK (ROL IN ('ADMIN','FUNCIONARIO'))`
- `CK_USUARIO_ROL_DATOS CHECK ((ROL = 'FUNCIONARIO') OR (DEPARTAMENTO IS NULL AND TELEFONO IS NULL))` — un ADMIN no puede tener departamento/teléfono; un FUNCIONARIO puede tenerlos o no (en el `data.xml` real hay funcionarios con `departamento` vacío).

No se crea una tabla `ADMIN` separada: el único admin de la semilla (`admin`) es una fila de `USUARIO` con `ROL='ADMIN'`.

### 2.2 `CATEGORIA_RECURSO`

| Columna | Tipo | Nulo | De dónde sale |
|---|---|---|---|
| `ID` | `VARCHAR2(12)` | NOT NULL (PK) | `CategoriaRecurso.id`, formato `CAT-000001` (generado, ver §4) |
| `DESCRIPCION` | `VARCHAR2(200)` | NOT NULL | `CategoriaRecurso.descripcion` |

- `PK_CATEGORIA (ID)`
- Sin `UNIQUE` sobre `DESCRIPCION`: `CategoriaServiceTest.crearCategoriaDuplicada` espera que dos categorías con la misma descripción puedan coexistir con ids distintos.

### 2.3 `RECURSO`

| Columna | Tipo | Nulo | De dónde sale |
|---|---|---|---|
| `ID` | `VARCHAR2(20)` | NOT NULL (PK) | `Recurso.id` (lo asigna quien llama, ej. `238715`, `R-900`) |
| `DESCRIPCION` | `VARCHAR2(200)` | NOT NULL | `Recurso.descripcion` |
| `CATEGORIA_ID` | `VARCHAR2(12)` | NOT NULL (FK) | `Recurso.categoria.id` |
| `DISPONIBLE` | `NUMBER(1)` | NOT NULL DEFAULT 1 | `Recurso.disponible` (boolean → 0/1) |

- `PK_RECURSO (ID)`
- `FK_RECURSO_CATEGORIA (CATEGORIA_ID) REFERENCES CATEGORIA_RECURSO(ID)` — sin `ON DELETE CASCADE`: si hay recursos asociados, borrar la categoría debe fallar (ver §3).
- `CK_RECURSO_DISPONIBLE CHECK (DISPONIBLE IN (0,1))`

### 2.4 `RESERVA`

| Columna | Tipo | Nulo | De dónde sale |
|---|---|---|---|
| `ID` | `VARCHAR2(12)` | NOT NULL (PK) | `Reserva.id`, formato `RES-000001` (generado, ver §4) |
| `ACTIVIDAD` | `VARCHAR2(200)` | NOT NULL | `Reserva.actividad` |
| `FECHA` | `DATE` | NOT NULL | `Reserva.fecha` (`String` ISO `yyyy-MM-dd` en Java → `DATE` en Oracle, sin componente de hora) |
| `HORA_INICIO` | `VARCHAR2(5)` | NOT NULL | `Reserva.horaInicio`, formato `HH:mm` |
| `HORA_FIN` | `VARCHAR2(5)` | NOT NULL | `Reserva.horaFin`, formato `HH:mm` |
| `FUNCIONARIO_ID` | `VARCHAR2(20)` | NOT NULL (FK) | `Reserva.funcionario.id` |
| `ESTADO` | `VARCHAR2(12)` | NOT NULL DEFAULT 'ACTIVA' | `Reserva.estado` (`EstadoReserva`: `ACTIVA`, `CANCELADA`, `COMPLETADA`) |

- `PK_RESERVA (ID)`
- `FK_RESERVA_FUNCIONARIO (FUNCIONARIO_ID) REFERENCES USUARIO(ID)`
- `CK_RESERVA_ESTADO CHECK (ESTADO IN ('ACTIVA','CANCELADA','COMPLETADA'))`
- `CK_RESERVA_HORA_INICIO CHECK (REGEXP_LIKE(HORA_INICIO, '^([01][0-9]|2[0-3]):[0-5][0-9]$'))`
- `CK_RESERVA_HORA_FIN` — mismo patrón para `HORA_FIN`.
- `CK_RESERVA_HORARIO CHECK (HORA_INICIO < HORA_FIN)` — como ambas son `VARCHAR2(5)` con formato `HH:mm` de ancho fijo y cero-rellenado, la comparación lexicográfica de cadenas coincide con la comparación de horas (`"09:00" < "10:00"`), así que el `CHECK` reproduce `ReservaService.horarioValido(...)` a nivel de fila.

**Cancelación:** el P1 modela "cancelar" como **cambio de estado** (`EstadoReserva.CANCELADA`), nunca como borrado físico — se reproduce igual aquí: `cancelarReserva` es un `UPDATE ... SET ESTADO = 'CANCELADA'`, nunca un `DELETE`. Por eso tampoco hay un `eliminarReserva` en el script.

### 2.5 `RESERVA_RECURSO` (relación N:M)

| Columna | Tipo | Nulo | De dónde sale |
|---|---|---|---|
| `RESERVA_ID` | `VARCHAR2(12)` | NOT NULL | `Reserva.recursos` (lado reserva) |
| `RECURSO_ID` | `VARCHAR2(20)` | NOT NULL | `Reserva.recursos` (lado recurso) |

- `PK_RESERVA_RECURSO (RESERVA_ID, RECURSO_ID)`
- `FK_RESRE_RESERVA (RESERVA_ID) REFERENCES RESERVA(ID)`
- `FK_RESRE_RECURSO (RECURSO_ID) REFERENCES RECURSO(ID)`

No lleva `ON DELETE CASCADE` porque el P1 nunca borra una `RESERVA` (solo la cancela), así que no hay escenario de borrado en cascada que programar.

---

## 3. Qué queda en la BD y qué queda en Java

**En la base de datos** (estructural, válido sin importar quién llame):
- `PK_*` — unicidad de identificadores.
- `FK_RECURSO_CATEGORIA`, `FK_RESERVA_FUNCIONARIO`, `FK_RESRE_*` — integridad referencial. En particular, `FK_RECURSO_CATEGORIA` sin cascada es lo que hace que Oracle rechace (`ORA-02292`) borrar una categoría con recursos asociados.
- `CK_USUARIO_ROL`, `CK_USUARIO_ROL_DATOS`, `CK_RECURSO_DISPONIBLE`, `CK_RESERVA_ESTADO` — dominios de valores válidos.
- `CK_RESERVA_HORA_INICIO/FIN`, `CK_RESERVA_HORARIO` — formato y orden de horas (regla de una sola fila).

**En Java** (reglas de negocio que cruzan filas o dependen de "ahora", y que el P1 ya implementaba así — se conservan igual en los `*Service` del backend, ahora recibiendo un `Repositorio` en vez de `Data`):
- Campos obligatorios de alto nivel con mensaje propio (`"El ID y el nombre son obligatorios"`, `"La actividad es obligatoria"`, `"Debe asignar al menos un recurso a la reserva"`, `"Debe asignar una categoria al recurso"`).
- **Disponibilidad de recursos**: que un recurso no tenga otra reserva `ACTIVA` que se traslape en fecha/hora (`ReservaService.create`, `buscarRecursoDisponible`, `asignarRecursosDisponibles`). Se apoya en la función SQL `listarRecursosOcupados` para no traer todas las reservas a Java, pero la decisión ("¿hay traslape?", "¿cuál recurso libre elijo primero?") la toma el service.
- **Eliminar categoría con recursos asociados**: el service pre-chequea con `listarRecursoPorCategoria` y devuelve el mensaje de negocio (`"No se puede eliminar: existen recursos asociados."`) *antes* de llegar a la BD; la FK es la segunda línea de defensa si, por una carrera, el chequeo previo quedó desactualizado.
- **Cancelar solo reservas futuras** (`fecha.isAfter(LocalDate.now())`) — depende de la fecha actual, no es expresable como `CHECK` fijo.
- **Cambiar clave**: la clave *actual* se valida en Java contra el objeto `Usuario` ya autenticado (`UsuarioService.cambiarClave`); la BD solo ejecuta el `UPDATE` de la clave nueva.
- **Concurrencia dentro del proceso**: dos hilos creando una reserva para el mismo recurso/horario — se protege con un `Lock` en `ReservaService.create`, no con un `CHECK` (ver `backend/docs/ETAPA-2-RESUMEN.md` para el detalle de implementación).

---

## 4. Generación de IDs

Solo dos entidades generan su id en el servidor (igual que en el P1, donde `Data.generarIdCategoria()`/`generarIdReserva()` existen pero no hay un `generarIdFuncionario()` ni `generarIdRecurso()` — esos los asigna quien llama):

| Entidad | Secuencia | Formato | Quién lo pide |
|---|---|---|---|
| `CategoriaRecurso` | `SEQ_CATEGORIA` | `CAT-000001` (prefijo + `LPAD` a 6 dígitos) | Lo genera **dentro** de `insertarCategoria` (procedimiento), igual que `CategoriaService.create` generaba el id y lo insertaba en el mismo paso. |
| `Reserva` | `SEQ_RESERVA` | `RES-000001` | `IService.generarId()` ya existe como paso **separado** de `create(Reserva)` (el `ReservasController` genera el id antes de construir el objeto) — así que hay una función `siguienteIdReserva()` que solo devuelve el próximo id formateado, y `insertarReserva` lo recibe como parámetro `IN`, no lo genera. |

Ambos casos usan una única función auxiliar `formatearId(prefijo, numero)` con `LPAD(numero, 6, '0')`.

`Funcionario.id` (cédula) y `Recurso.id` lo asigna siempre quien llama (el usuario de la UI); si ya existe, la base lo rechaza por `PK` (`ORA-00001`) y el mensaje mapeado es `"Ya existe un registro con ese identificador"`.

---

## 5. Procedimientos y funciones

Convención: `insertar<Entidad>` / `modificar<Entidad>` / `eliminar<Entidad>` son `PROCEDURE`; `buscar<Entidad>` / `listar<Entidad>` son `FUNCTION` que retornan `types.ref_cursor`. Todos los parámetros de entrada son *bind* (nunca se concatena SQL).

### 5.1 Usuario / Funcionario

| Nombre | Parámetros | Retorna |
|---|---|---|
| `loginUsuario` (FUNCTION) | `p_id IN VARCHAR2`, `p_clave IN VARCHAR2` | `types.ref_cursor` — 0 o 1 fila (`ID,NOMBRE,CLAVE,ROL,DEPARTAMENTO,TELEFONO`) |
| `cambiarClave` (PROCEDURE) | `p_id IN VARCHAR2`, `p_clave_nueva IN VARCHAR2` | — |
| `insertarFuncionario` (PROCEDURE) | `p_id IN`, `p_nombre IN`, `p_clave IN`, `p_departamento IN`, `p_telefono IN` | — (rol fijo `'FUNCIONARIO'`) |
| `modificarFuncionario` (PROCEDURE) | `p_id IN`, `p_nombre IN`, `p_departamento IN`, `p_telefono IN` | — (no toca `CLAVE`, igual que `FuncionarioService.update`) |
| `eliminarFuncionario` (PROCEDURE) | `p_id IN VARCHAR2` | — |
| `buscarFuncionario` (FUNCTION) | `p_id IN VARCHAR2` | `types.ref_cursor`, 0 o 1 fila, solo `ROL='FUNCIONARIO'` |
| `listarFuncionario` (FUNCTION) | — | `types.ref_cursor`, todas las filas con `ROL='FUNCIONARIO'` |
| `listarFuncionarioTexto` (FUNCTION) | `p_texto IN VARCHAR2` | `types.ref_cursor` — `UPPER(ID) LIKE UPPER('%'\|\|p_texto\|\|'%') OR UPPER(NOMBRE) LIKE UPPER('%'\|\|p_texto\|\|'%')` |

### 5.2 Categoria

| Nombre | Parámetros | Retorna |
|---|---|---|
| `insertarCategoria` | `p_descripcion IN`, `p_id OUT VARCHAR2` | genera id con `SEQ_CATEGORIA` + `formatearId` |
| `modificarCategoria` | `p_id IN`, `p_descripcion IN` | — |
| `eliminarCategoria` | `p_id IN` | — |
| `buscarCategoria` (FUNCTION) | `p_id IN` | `types.ref_cursor` |
| `listarCategoria` (FUNCTION) | — | `types.ref_cursor` |
| `listarCategoriaTexto` (FUNCTION) | `p_texto IN` | `types.ref_cursor` — `UPPER(DESCRIPCION) LIKE UPPER('%'\|\|p_texto\|\|'%')` |

### 5.3 Recurso

| Nombre | Parámetros | Retorna |
|---|---|---|
| `insertarRecurso` | `p_id IN`, `p_descripcion IN`, `p_categoria_id IN` | — (`DISPONIBLE` default 1) |
| `modificarRecurso` | `p_id IN`, `p_descripcion IN`, `p_categoria_id IN` | — |
| `eliminarRecurso` | `p_id IN` | — |
| `buscarRecurso` (FUNCTION) | `p_id IN` | `types.ref_cursor` |
| `listarRecurso` (FUNCTION) | — | `types.ref_cursor` |
| `listarRecursoPorCategoria` (FUNCTION) | `p_categoria_id IN` | `types.ref_cursor` |

### 5.4 Reserva y Reserva_Recurso

| Nombre | Parámetros | Retorna |
|---|---|---|
| `siguienteIdReserva` (FUNCTION) | — | `VARCHAR2` — próximo id formateado (no inserta nada) |
| `insertarReserva` | `p_id IN`, `p_actividad IN`, `p_fecha IN DATE`, `p_hora_inicio IN`, `p_hora_fin IN`, `p_funcionario_id IN` | — |
| `insertarReservaRecurso` | `p_reserva_id IN`, `p_recurso_id IN` | — (una fila por recurso asignado) |
| `cancelarReserva` | `p_id IN` | — (`UPDATE ... SET ESTADO='CANCELADA'`) |
| `buscarReserva` (FUNCTION) | `p_id IN` | `types.ref_cursor`, 0 o 1 fila (sin recursos) |
| `listarReserva` (FUNCTION) | — | `types.ref_cursor`, todas las reservas |
| `listarReservaPorFuncionario` (FUNCTION) | `p_funcionario_id IN` | `types.ref_cursor` |
| `listarReservaPorFecha` (FUNCTION) | `p_fecha IN DATE` | `types.ref_cursor` — todas las reservas de esa fecha (cualquier estado; el filtro `ESTADO='ACTIVA'` para calendarización lo aplica Java, igual que el P1) |
| `listarReservaPorRango` (FUNCTION) | `p_desde IN DATE`, `p_hasta IN DATE` | `types.ref_cursor` — reservas con `FECHA BETWEEN p_desde AND p_hasta` y `ESTADO <> 'CANCELADA'` (ambos consumidores del P1, Actividades y Estadísticas, descartan canceladas) |
| `listarRecursoPorReserva` (FUNCTION) | `p_reserva_id IN` | `types.ref_cursor` — recursos de una reserva (para reconstruir `Reserva.recursos` en Java) |
| `listarRecursosOcupados` (FUNCTION) | `p_fecha IN DATE`, `p_hora_inicio IN`, `p_hora_fin IN` | `types.ref_cursor` — ids de recursos con una reserva `ACTIVA` ese día cuyo horario se traslapa: `p_hora_inicio < HORA_FIN AND p_hora_fin > HORA_INICIO` (rangos contiguos, ej. termina a las 10:00 y la otra empieza a las 10:00, **no** se traslapan) |

### 5.5 Estadísticas

| Nombre | Parámetros | Retorna |
|---|---|---|
| `estadisticaRecursosCategoria` (FUNCTION) | `p_desde IN DATE`, `p_hasta IN DATE` | `types.ref_cursor` de `(CATEGORIA_DESCRIPCION, CANTIDAD)`, agrupando por categoría, excluyendo `CANCELADA`, `FECHA BETWEEN p_desde AND p_hasta` |
| `estadisticaActividadesSemana` (FUNCTION) | `p_desde IN DATE`, `p_hasta IN DATE` | `types.ref_cursor` de `(SEMANA_INICIO, CANTIDAD)`, agrupando con `TRUNC(FECHA,'IW')` (semana ISO, empieza lunes — igual que `DayOfWeek.MONDAY` en el P1), excluyendo `CANCELADA` |

---

## 6. Mapa de errores Oracle → mensaje de usuario

Centralizado en `Servicio.mensajeError(SQLException)`, usado por todos los `ServicioX` para nunca propagar un `SQLException`/stacktrace al cliente:

| `SQLException.getErrorCode()` | Causa típica | Mensaje al usuario |
|---|---|---|
| `1` (`ORA-00001`) | Violación de `PK`/`UNIQUE` | "Ya existe un registro con ese identificador" |
| `2291` (`ORA-02291`) | `FK` apunta a un padre que no existe | "Referencia inválida (el registro relacionado no existe)" |
| `2292` (`ORA-02292`) | Hay hijos que dependen del registro a borrar | "No se puede eliminar: tiene registros asociados" |
| `12899` (`ORA-12899`) | Valor más largo que la columna | "Dato demasiado largo" |
| `1017` (`ORA-01017`) | Usuario/clave de conexión inválidos | "Credenciales de base de datos inválidas" |
| `12541` / `17002` | Listener/BD no disponible | "La base de datos no se encuentra disponible" |
| cualquier otro | — | "Error de base de datos" |

La contraseña de conexión (`DB_PASSWORD`) nunca se incluye en estos mensajes ni en ningún log.

## 7. Limitaciones conocidas (fuera de alcance de esta etapa)

- Las claves se guardan en texto plano, igual que en el P1 original (`Usuario.clave` sin hash). No se introduce `bcrypt`/`SHA` aquí porque el enunciado del P1 no lo pedía y cambiarlo ahora alteraría `UsuarioService.cambiarClave`/`login` de forma no solicitada.
- `data.xml` se usa una única vez para redactar la semilla del script (§8 del script SQL) y luego se borra (Etapa 2, paso F); no queda ninguna lectura de XML en `backend/`.
