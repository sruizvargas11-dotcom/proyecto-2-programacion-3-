# BACKEND — Proyecto #2 (EIF206) · Guía completa para Claude Code

> **Cómo usar este archivo:** ponelo en el repo como `docs/BACKEND-P2.md` (y agregá a `.gitignore` solo si no querés entregarlo). Después, en Claude Code, escribí:
> **"Lee docs/BACKEND-P2.md y ejecuta la ETAPA 1"** (y luego 2, 3 y 4, una por vez).
> Ya trae el patrón del profe adentro: **no hace falta copiar ninguna carpeta de referencia**.

---

## 1. Qué pide el enunciado del Backend

- Corresponde a las capas **Lógica y de Datos**.
- Es un **servidor sin interfaz de usuario** (solo consola).
- **Escucha en un puerto** las peticiones de los Frontend, las atiende y responde.
- Es **el único que se conecta a la base de datos**.
- Envía **notificaciones asincrónicas** a todos los **demás** Frontend activos cada vez que un usuario registra una reserva.
- Frontend y Backend deben poder correr en **máquinas separadas**; varios Frontend con un Backend.
- Persistencia en **base de datos** (el enunciado dice MySQL por escrito; el profe trabaja con **Oracle y procedimientos almacenados**, y este proyecto usa Oracle). Todos los datos se validan.
- Tests **JUnit 5**: unidad (Surefire) e integración (Failsafe). Se entrega el script de la BD.
- **El XML desaparece por completo.**

## 2. Arquitectura (monorepo, 3 módulos)

```
├── pom.xml        ← padre
├── protocol/      ← IService + Protocol + entidades Serializable
├── frontend/      ← pantallas (Thomas)
└── backend/       ← esto
    └── src/main/java/una/eif206/
        ├── ServerMain.java        ← arranque por consola
        ├── net/    Server, Worker ← sockets e hilos
        ├── service/ Service implements IService + services de lógica (validaciones, reglas)
        └── data/   Servicio (base), ServicioX, Repositorio, OracleRepositorio, MemoriaRepositorio
```

Flujo: `Frontend → Proxy → socket SYNC → Worker → Service (lógica) → Repositorio → Oracle (procedimientos almacenados)`.

## 3. Diseño de red (PDFs 13 y 14 del profe) — NO se usa JSON

- Un `Server` con `ServerSocket.accept()` en un hilo. Por cada cliente, un `Worker` (hilo).
- **Dos sockets por Frontend:** SYNC (peticiones/respuestas) y ASYNC (notificaciones).
- Protocolo con `ObjectOutputStream` / `ObjectInputStream`:
  - Petición: `writeInt(operación)` + `writeObject(argumentos)` + `flush()`.
  - Respuesta: `writeInt(status)` (`ERROR_NO_ERROR` o `ERROR_ERROR`) + `writeObject(resultado)`.
- Handshake: el cliente manda `Protocol.SYNC` → el Server crea el Worker y devuelve el `sid` (String). Luego abre el segundo socket: `Protocol.ASYNC` + `sid` → `join(...)` le asigna ese socket al Worker.
- Notificación: `aos.writeInt(Protocol.DELIVER_MESSAGE)` + `aos.writeObject(reserva)`.
- `srv.deliver_message(from, ...)` recorre los Workers y **salta al que creó la reserva** (`w != from`).
- Trampas: crear primero el `ObjectOutputStream` y hacer `flush()` antes del `ObjectInputStream`; `flush()` tras cada escritura; mismo orden de lectura y escritura; `reset()` si se reenvía un objeto modificado; lista de Workers segura para hilos (`CopyOnWriteArrayList`).

## 4. Patrón de acceso a datos del profe (ya corregido)

El material del profe es un ejemplo de **otro dominio** (libro, perfil, contactos). Se copia el **patrón**. Su código tiene errores que **no** se deben copiar: usa el texto literal `"id"` en lugar del parámetro, los nombres de procedimientos no siempre coinciden con el script, y trata `executeUpdate` de un `call` como si fuera un conteo de filas.

### 4.1 Clases base

```java
public class NoDataException extends Exception {
    public NoDataException(String msg) { super(msg); }
}
public class GlobalException extends Exception {
    public GlobalException(String msg) { super(msg); }
}

public class Servicio {
    protected Connection conexion;

    protected void conectar() throws GlobalException, NoDataException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            String url  = Config.get("DB_URL");       // env var; si no, backend/config.properties
            String user = Config.get("DB_USER");
            String pass = Config.get("DB_PASSWORD");   // NUNCA en el repo ni en logs
            conexion = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException e) {
            throw new GlobalException("No se ha localizado el driver");
        } catch (SQLException e) {
            throw new NoDataException("La base de datos no se encuentra disponible");
        }
    }

    protected void desconectar() throws GlobalException {
        try { if (conexion != null && !conexion.isClosed()) conexion.close(); }
        catch (SQLException e) { throw new GlobalException("Error al cerrar la conexión"); }
    }
}
```

`Config.get(clave)`: primero variable de entorno; si no existe, `backend/config.properties` (no se sube; se sube `config.properties.example`).

### 4.2 Un `ServicioX` por entidad (ejemplo corregido)

```java
public class ServicioCategoria extends Servicio {
    private static final String INSERTAR   = "{call insertarCategoria (?,?,?)}";
    private static final String MODIFICAR  = "{call modificarCategoria (?,?,?)}";
    private static final String ELIMINAR   = "{call eliminarCategoria (?)}";
    private static final String BUSCAR     = "{?=call buscarCategoria (?)}";
    private static final String LISTAR     = "{?=call listarCategoria ()}";

    public Collection<CategoriaRecurso> listar() throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LISTAR)) {
            cs.registerOutParameter(1, Types.REF_CURSOR);
            cs.execute();
            List<CategoriaRecurso> lista = new ArrayList<>();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) lista.add(new CategoriaRecurso(rs.getString("id"),
                        rs.getString("nombre"), rs.getString("descripcion")));
            }
            return lista;
        } catch (SQLException e) {
            throw new GlobalException("Sentencia no válida");
        } finally { desconectar(); }
    }

    public void insertar(CategoriaRecurso c) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(INSERTAR)) {
            cs.setString(1, c.getId());           // parámetro REAL, no un literal
            cs.setString(2, c.getNombre());
            cs.setString(3, c.getDescripcion());
            cs.execute();                         // execute() para procedimientos
        } catch (SQLException e) {
            throw new GlobalException("Llave duplicada o datos inválidos");
        } finally { desconectar(); }
    }
    // modificar, eliminar y buscar siguen exactamente el mismo molde
}
```

Reglas del patrón: `{call proc(?,?)}` para procedimientos; `{?=call funcion(?)}` con `registerOutParameter(1, Types.REF_CURSOR)` para consultas; leer columnas por nombre; cerrar todo con try-with-resources; mensajes de error claros para el cliente (nunca `printStackTrace` hacia afuera).

### 4.3 Patrón del script SQL (Oracle 11g)

```sql
SET DEFINE OFF;
CREATE OR REPLACE PACKAGE types AS
    TYPE ref_cursor IS REF CURSOR;
END types;
/
CREATE OR REPLACE PROCEDURE insertarCategoria (
    p_id IN categoria.id%TYPE, p_nombre IN categoria.nombre%TYPE,
    p_descripcion IN categoria.descripcion%TYPE
) AS
BEGIN
    INSERT INTO categoria (id, nombre, descripcion) VALUES (p_id, p_nombre, p_descripcion);
END;
/
CREATE OR REPLACE FUNCTION buscarCategoria (p_id IN categoria.id%TYPE)
RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR SELECT id, nombre, descripcion FROM categoria WHERE id = p_id;
    RETURN v_cursor;
END;
/
CREATE OR REPLACE FUNCTION listarCategoria RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR SELECT id, nombre, descripcion FROM categoria;
    RETURN v_cursor;
END;
/
CREATE OR REPLACE PROCEDURE eliminarCategoria (p_id IN categoria.id%TYPE) AS
BEGIN
    DELETE FROM categoria WHERE id = p_id;
END;
/
-- login: devuelve el usuario (o ningún registro) para que Java construya el objeto
-- Verificación al final:
SELECT object_name, object_type, status FROM user_objects WHERE status = 'INVALID';
SELECT name, type, line, position, text FROM user_errors ORDER BY name, sequence;
```

Oracle 11g **no** tiene `IDENTITY`, ni `FETCH FIRST`/`OFFSET`, ni `BOOLEAN` en SQL: usar **secuencias**, `ROWNUM` si hace falta, y `NUMBER(1)` para banderas.

### 4.4 Dependencia y conexión

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc11</artifactId>
  <version>23.6.0.24.10</version>
</dependency>
```
Variables de entorno: `DB_URL` (ej. `jdbc:oracle:thin:@localhost:1521:XE`), `DB_USER`, `DB_PASSWORD`. Usar un esquema propio, no `SYSTEM`. El profe advierte que contra Oracle XE 11g puede hacer falta otro driver: si no conecta, probar una versión más antigua de `ojdbc8`.

## 5. Reglas generales para Claude Code

- No hacer commit; al terminar cada etapa, resumir qué cambió y cómo probarlo.
- No tocar `frontend/` ni `protocol/` después de la Etapa 1. Si hay que cambiar `IService`, avisar antes.
- Java: mantener la versión actual del proyecto.
- Sin contraseñas, claves de API (la de la IA va en configuración fuera del repo) ni reportes `.txt` en el repo.

---

## ETAPA 1 — Partir el repo en 3 módulos

1. Crear `pom.xml` padre (packaging pom) con módulos `protocol`, `frontend`, `backend`.
2. Usar `git mv` para conservar historial:
   - `protocol/`: paquete `model/`. Quitar anotaciones JAXB; entidades `Serializable` con `serialVersionUID` fijo.
   - `frontend/`: `view/`, `controller/`, `util/` (lo de UI), `ApplicationLogin`, recursos e íconos.
   - `backend/`: `service/`, `data/` y `data.xml`.
3. En `protocol/` crear:
   - `IService`: una sola interfaz con todos los métodos públicos de los 9 services del P1 (mismos nombres y firmas; escrituras devuelven `String` error o `null`).
   - `Protocol`: `HOST`, `PORT`, `SYNC`, `ASYNC`, `DELIVER_MESSAGE`, `ERROR_NO_ERROR`, `ERROR_ERROR` y una constante `int` por cada método de `IService`.
4. En `backend/`: `Service implements IService` delegando a los services del P1. El XML queda **solo temporalmente** para que compile; se elimina en la Etapa 2. Sin Server/Worker todavía.
5. En `frontend/`: `client/FakeService implements IService` con datos en memoria (1 admin, 1 funcionario, 2 categorías, 3 recursos). Los controllers reciben `IService` por constructor y `main()` usa `FakeService`. `frontend` depende de `protocol`, nunca de `backend`.
6. Mover cada test al módulo de su código; ajustar imports y poms.

**Verificar:** `mvn clean verify` desde la raíz; `frontend` no importa `data`, `XMLHelper` ni `java.sql`.
✅ Después: probar la app, commit, push y avisar a Thomas.

## ETAPA 2 — Base de datos Oracle + capa de datos (sin XML)

**A) Script** `backend/src/main/resources/db/script_reservas_oracle11g.sql`
- Compatible con Oracle XE 11g (ver 4.3). Re-ejecutable: `DROP` protegidos con `EXECUTE IMMEDIATE` y manejo de "no existe".
- Tablas derivadas de las entidades de `protocol/model` (Usuario/Admin/Funcionario, CategoriaRecurso, Recurso, Reserva y la relación reserva-recurso) con PK, FK, UNIQUE y CHECK. Fechas `DATE`; horas `VARCHAR2(5)` en formato `HH:mm`.
- Mantener los formatos de id del P1 (ej. `CAT-000001`) con secuencias. Funcionario: id = cédula y clave inicial = id.
- Paquete `types` con `ref_cursor`.
- **Todo el acceso a datos por procedimientos y funciones almacenados:** `insertarX`/`modificarX`/`eliminarX` como PROCEDURE; `buscarX`/`listarX` como FUNCTION con `types.ref_cursor`; además: `login`, `cambiarClave`, búsqueda por texto de funcionarios y categorías, recursos por categoría, reservas por funcionario, recursos ocupados en un rango fecha-hora, y reservas por rango de fechas (para calendario, actividades de la semana y estadísticas).
- Datos semilla tomados de `data.xml` (1 admin y los funcionarios). **Omitir** los usuarios `504670161` y `50467` (no tienen rol).
- Al final, consultas de verificación de objetos `INVALID`. Sin `COMMIT` automático salvo en la semilla.

**B) Capa Java** en `una.eif206.data` (patrón de la sección 4)
- `Servicio` base, `NoDataException`, `GlobalException`, un `ServicioX` por entidad, `Config`.
- Dependencia `ojdbc11` en `backend/pom.xml`.
- Interfaz `Repositorio` con todas las operaciones de persistencia. Implementaciones: `OracleRepositorio` (delegando en los `ServicioX`) y `MemoriaRepositorio` (solo en memoria, reutilizando la lógica del P1) para los tests.
- Los services reciben `Repositorio` por constructor. Se mantienen todas las reglas y validaciones del P1.
- Operaciones de varios pasos (crear reserva: validar disponibilidad, asignar recursos, insertar reserva y relación) en **una conexión** con `autoCommit(false)` y `commit`/`rollback`. La creación de reservas se protege contra concurrencia dentro del proceso (`synchronized` o `Lock`) para que dos clientes no reserven el mismo recurso.
- **Eliminar por completo el XML:** borrar `XMLHelper`, la dependencia JAXB del pom, anotaciones e imports de `jakarta.xml.bind` y toda lectura/escritura de XML. `data.xml` se usa **una sola vez** para generar la semilla y luego se borra.

**C) Prueba:** `OracleSmokeIT` (Failsafe), habilitado solo si existe `DB_URL` (`@EnabledIfEnvironmentVariable`): login y crear/consultar/eliminar una categoría.

**Verificar:** `mvn clean verify`; grep de `xml`, `JAXB`, `XMLHelper` en `backend/` sin resultados. Al final, explicar cómo ejecutar el script y con qué variables correr el IT.

## ETAPA 3 — Servidor, Workers y notificaciones

Seguir la sección 3. Concretamente:
1. `ServerMain` y `Server`: puerto desde `backend/config.properties` (por defecto `Protocol.PORT`); escucha en todas las interfaces. Lee el `int` inicial: `SYNC` → crea `sid`, `Worker`, lo agrega a la lista, lo inicia y devuelve el `sid`; `ASYNC` → lee el `sid` y llama `join(...)`.
2. `Worker`: ciclo `readInt()` + `switch` por cada operación de `IService`; lee argumentos, llama al `Service`, responde `writeInt(status)` + `writeObject(resultado)` con `flush`; si falla, `ERROR_ERROR`. Al desconectarse o `LOGOUT`, se quita de la lista y cierra sockets.
3. Tras `RESERVA_CREATE` exitoso, `srv.deliver_message(this, reserva)` a los demás Workers por su socket ASYNC (nunca al creador). Un fallo con un Worker no afecta a los demás.
4. Errores al cliente como `String` claros, sin stacktraces. Log simple en consola con hora, `sid` y operación.
5. `Service` usa `OracleRepositorio` por defecto. **Sin Swing ni AWT** en el backend.

**Verificar:** `mvn clean verify`; indicar cómo arrancar el servidor.
✅ Listo cuando: un cliente de prueba hace SYNC, login y una operación; con 2 clientes, el segundo recibe la notificación cuando el primero crea una reserva.

## ETAPA 4 — Tests, limpieza y verificación final

1. **Unidad (Surefire):** services con `MemoriaRepositorio`: login, cambio de clave, CRUD de funcionarios/categorías/recursos, validaciones, disponibilidad y conflicto de reservas, cancelación (solo futuras), calendario, actividades y estadísticas.
2. **Integración (Failsafe)** con sockets reales y `MemoriaRepositorio`: levantar `Server` en un puerto libre, conectar clientes de prueba y verificar SYNC, ASYNC con `sid`, una operación completa, que dos clientes reciban la notificación y que el creador **no** reciba la suya, y la desconexión de un cliente. `OracleSmokeIT` sigue condicionado a `DB_URL`.
3. `backend/README.md`: cómo ejecutar el script, variables de entorno, arrancar el servidor y correr los tests.
4. Limpieza: `.gitignore` con `config.properties`, `target/`, `.idea/`; sin contraseñas ni claves en el repo; sin reportes `.txt`.
5. **Verificación de arquitectura con grep** (mostrar el resultado de cada una):
   - `backend/` sin `javax.swing` ni `java.awt` (servidor sin interfaz).
   - `frontend/` y `protocol/` sin `java.sql`, `oracle` ni `ojdbc` (solo el Backend toca la BD).
   - `backend/` sin `xml`, `JAXB` ni `XMLHelper`.

**Verificar:** `mvn clean verify` desde la raíz, sin Oracle (los tests usan memoria).
