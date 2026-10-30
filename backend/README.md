# Backend — Sistema de Reservas

Servidor del sistema de reservas: capa de datos contra Oracle (`una.eif206.data`),
lógica de negocio (`una.eif206.service`) y el servidor de sockets (`una.eif206.net`)
que expone todo eso por red (`ServerMain`). No tiene interfaz gráfica; la UI vive
en `frontend/` y habla con este backend por el protocolo definido en `protocol/`.

## Requisitos

- **Java 21** (`maven.compiler.source`/`target` del `pom.xml` raíz).
- **Maven** 3.8+ (o el embebido de tu IDE; en este equipo se usó el de IntelliJ,
  ya que `mvn` no está en el `PATH` del sistema).
- **Oracle Database XE 11.2** (Express Edition 11g Release 2) accesible por red,
  o cualquier Oracle compatible con el driver `ojdbc8` fijado en `backend/pom.xml`
  (**no usar `ojdbc11`/23.x**: no es compatible con XE 11.2).
- Para la extracción de reservas por lenguaje natural (`ReservaIAService`, opcional):
  una API key de [Groq](https://console.groq.com/); si falta, el backend sigue
  funcionando normalmente y esa única funcionalidad cae automáticamente al
  demo público de langchain4j.

Nada de esto se necesita para **compilar y correr las pruebas unitarias**: solo
hace falta Java/Maven. Oracle solo es necesario para arrancar `ServerMain` en modo
real o para correr las pruebas de integración contra Oracle (que se saltan solas
si no hay conexión configurada — ver "Pruebas" más abajo).

## Variables de entorno

El backend resuelve su configuración en dos lugares **distintos**, cada uno para
una cosa, nunca mezclados:

| Para qué | Variables de entorno (prioridad siempre) | Archivo local de respaldo | Quién lo usa |
|---|---|---|---|
| Conexión real a Oracle (producción) | `DB_URL`, `DB_USER`, `DB_PASSWORD` | — (**no tiene respaldo**: si faltan, no conecta) | `Servicio.conexionReal()` — el camino que usan todos los `ServicioX` al atender una petición real |
| Puerto del servidor, y las mismas tres de arriba para herramientas de diagnóstico | `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD` | `backend/config.properties` (junto al `pom.xml`; copiar desde `config.properties.example`) | `ServerMain` (solo `PORT`), `ConexionCheck`, limpieza de `OracleRepositorioIT` |
| API key de Groq para `ReservaIAService` (opcional) | — (no se lee de entorno) | `backend/src/main/resources/config.properties`, clave `groq.api.key` (**en el classpath**, no junto al `pom.xml`) | `ReservaIAService` |

Puntos importantes:

- **La conexión real a Oracle exige las tres variables de entorno** `DB_URL`/`DB_USER`/
  `DB_PASSWORD` tal cual (`Servicio.conexionReal()` llama a `System.getenv(...)`
  directo, sin pasar por `config.properties`). `backend/config.properties` solo
  sirve de respaldo para el **puerto** y para las herramientas de diagnóstico
  (`ConexionCheck`, limpieza de `OracleRepositorioIT`), nunca para que el servidor
  real se conecte.
- Formato típico de `DB_URL` contra XE: `jdbc:oracle:thin:@localhost:1521:XE`
  (por SID) o `jdbc:oracle:thin:@//localhost:1521/XE` (por service name).
- **`DB_USER` nunca debe ser `SYSTEM`**: usar un esquema propio de la aplicación
  (ver "Crear la base de datos" abajo).
- Ninguna de estas tres variables se imprime nunca (ni en logs ni en mensajes de
  error): un `DB_PASSWORD` vacío o ausente hace fallar la conexión con un mensaje
  que nombra la variable, nunca su valor.
- `backend/config.properties` y `backend/src/main/resources/config.properties`
  están en `.gitignore`: **nunca se suben al repositorio**. Hay un
  `backend/config.properties.example` como plantilla (solo cubre `DB_URL`/`DB_USER`/
  `DB_PASSWORD`/`PORT`; la clave `groq.api.key` va en el otro archivo, bajo
  `src/main/resources/`, porque `ReservaIAService` la busca como recurso del
  classpath, no como archivo junto al `pom.xml`).

## Crear la base de datos

Los scripts están en `backend/src/main/resources/db/`. Conectar con **SQL Developer**
o **SQL\*Plus** (`sqlplus usuario@XE`) y correr, en este orden:

1. **`00_crear_esquema_ejemplo.sql`** (opcional, una sola vez, conectado como
   `SYS`/`SYSTEM` — ejemplo: `sqlplus sys/clave@XE AS SYSDBA`): crea un usuario
   propio de la aplicación (`reservas_app` en el ejemplo, con solo `CONNECT` y
   `RESOURCE`, sin privilegios de DBA) y pide la contraseña por variable de
   sustitución (`&password`), nunca queda escrita en el script. Si ya tienes un
   usuario/esquema propio para la aplicación, se puede omitir este paso.
2. **`script_reservas_oracle11g.sql`**, conectado como el usuario de la
   aplicación (**nunca como `SYSTEM`**): crea tablas, secuencias, procedimientos,
   funciones y la semilla de catálogo (1 admin, 2 funcionarios, 3 categorías,
   3 recursos — ver "Nota sobre la semilla" abajo). Es **re-ejecutable**: la
   primera sección borra todo lo que crean las siguientes (ignorando los errores
   de "no existe" con `WHEN OTHERS THEN NULL`), así que correrlo varias veces
   sobre el mismo esquema es seguro.
3. **`00_verificacion.sql`** (opcional, solo `SELECT`, no crea ni borra nada):
   confirma quién está conectado, cuenta objetos `INVALID` en `user_objects`
   (debe dar 0 filas) y cuenta filas por tabla.

### Nota sobre la semilla

La semilla de catálogo del script (sección 7, `COMMIT` único) viene del `data.xml`
original del P1: incluye el admin y los funcionarios que **sí** tenían `<rol>`
en ese XML (se omiten dos que no lo tenían), más las categorías/recursos de
catálogo. Las reservas transaccionales de ese XML **no** se migraron a propósito
(son datos de ejemplo, no catálogo). El propio script documenta esta procedencia
en un comentario junto al `INSERT` (sección 7).

### Probar que quedó limpio (0 `INVALID`)

Con un usuario de prueba desechable (nunca con `SYSTEM`/`SYS` para ejecutar el
script de la aplicación):

```sql
-- 1) Crear un usuario de prueba (conectado como SYS/SYSTEM):
CREATE USER reservas_test IDENTIFIED BY "una-clave-cualquiera"
  DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS;
GRANT CONNECT, RESOURCE TO reservas_test;
```

```sql
-- 2) Conectado YA como reservas_test, correr el script completo:
@script_reservas_oracle11g.sql

-- 3) Confirmar 0 filas (0 objetos invalidos):
SELECT object_name, object_type, status FROM user_objects WHERE status = 'INVALID';
-- (el propio script_reservas_oracle11g.sql ya termina con este mismo SELECT)
```

```sql
-- 4) Limpiar: conectado de nuevo como SYS/SYSTEM.
DROP USER reservas_test CASCADE;
```

Este flujo no se automatizó en este cambio (requiere credenciales de Oracle que
no se le piden al asistente por chat, para no arriesgar imprimir una contraseña);
los pasos de arriba son exactamente los que haría una corrida manual.

## Arrancar el servidor (`ServerMain`)

`ServerMain` no tiene interfaz: abre el `ServerSocket` en el puerto configurado
(`PORT`, o `7070` por defecto — `Protocol.PORT`) y queda escuchando hasta
`Ctrl+C`. Antes de arrancarlo, exportar `DB_URL`/`DB_USER`/`DB_PASSWORD` como
variables de entorno (ver arriba).

**Desde el IDE**: correr el método `main` de `una.eif206.ServerMain` (configurar
las variables de entorno en la configuración de ejecución).

**Desde la línea de comandos** (con Maven, sin necesidad de declarar ningún
plugin nuevo — mismo patrón que ya usa `ConexionCheck`):

```bash
# PowerShell
$env:DB_URL = "jdbc:oracle:thin:@localhost:1521:XE"
$env:DB_USER = "reservas_app"
$env:DB_PASSWORD = "..."   # nunca lo escribas en un script versionado
mvn -pl backend -am compile exec:java -Dexec.mainClass=una.eif206.ServerMain
```

```bash
# bash
export DB_URL="jdbc:oracle:thin:@localhost:1521:XE"
export DB_USER="reservas_app"
export DB_PASSWORD="..."
mvn -pl backend -am compile exec:java -Dexec.mainClass=una.eif206.ServerMain
```

## Extraer AI (Groq)

`ReservaIAService` lee la API key de Groq desde el classpath, no desde variables
de entorno. Para habilitarla:

1. Copie `backend/src/main/resources/config.properties.example` a
   `backend/src/main/resources/config.properties`.
2. Reemplace `PON_AQUI_TU_KEY` por su API key de [console.groq.com](https://console.groq.com/).
3. Reinicie `ServerMain`.

`backend/src/main/resources/config.properties` está en `.gitignore` y **nunca
se sube al repositorio**. `config.properties.example` sí se versiona y sirve
de plantilla para que cualquier integrante sepa qué clave necesita configurar.

Sin la key el backend arranca igual; Extraer AI puede no funcionar.

## Correr las pruebas

Desde la raíz del repositorio (compila `protocol` primero, del que depende `backend`):

```bash
mvn clean verify -pl backend -am
```

- **No requiere Oracle**: la gran mayoría de las pruebas usan `MemoriaRepositorio`
  (repositorio 100% en memoria, sin JDBC) o mocks de `Connection`/`CallableStatement`
  con Mockito.
- `OracleRepositorioIT` es la única clase que toca Oracle real, y solo corre si
  la variable de entorno `DB_URL` está definida
  (`@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")`); si no lo
  está, sus pruebas aparecen como **omitidas** (no como falladas).
- La fase `test` (Surefire) corre las unitarias; la fase `verify` además corre
  las `*IT` (Failsafe) — IT por socket real contra un `Server`/`Worker` levantado
  en un puerto efímero, con `MemoriaRepositorio` detrás (nunca Oracle).

Para correr solo una clase: `mvn -pl backend -am test -Dtest=NombreDeLaClase`.

## Decisiones de diseño

Tres decisiones que no son obvias leyendo el código aislado, documentadas aquí para
no perderlas de vista:

**a) `generarId`, `buscarRecursoDisponible` y `asignarRecursosDisponibles` no se
exponen por red.** Los tres métodos existen en `IService` (y `Protocol` tiene una
constante de operación para cada uno), pero `Worker` no les da un `case` en su
`switch`: son lógica interna, no operaciones que un cliente pueda invocar por
separado. La razón es que `RESERVA_CREATE` las resuelve todas **dentro del
servidor**, en una sola transacción y bajo un mismo candado (`CREAR_RESERVA_LOCK`
en `ServicioReserva` contra Oracle, `crearReservaLock` en `MemoriaRepositorio`):
si `Reserva.id` llega vacío, el id se genera ahí mismo; si un `Recurso` llega con
id vacío pero con categoría, se resuelve ahí mismo a un recurso libre de esa
categoría para esa fecha/franja. Exponer esos tres métodos por separado permitiría
a un cliente hacer esa misma resolución en pasos sueltos desde afuera ("busco
disponible" → "creo la reserva"), lo que reabre exactamente la ventana de carrera
que el candado/transacción existen para cerrar: dos clientes podrían ver el mismo
recurso libre en el paso de consulta y luego pelear por él en el paso de creación.
Mantenerlos fuera del `Worker` es, por eso, intencional.

**b) Calendario, actividades y estadísticas filtran las reservas en Java.**
`CalendarioService.getCalendario` y `ActividadesService.getActividadesSemana`
traen *todas* las reservas con `Repositorio.listarReservas()` y filtran por
fecha/semana ya en memoria, en vez de pedirle a Oracle solo las de un día o un
rango. El script SQL sí expone `listarReservaPorFecha` y `listarReservaPorRango`
pensadas justo para esto (ver `backend/docs/BD-ESPECIFICACION.md` §5.4), pero
ningún `Service` las llama hoy — son funciones sin consumidor. `EstadisticasService`
es la excepción: sus dos consultas (`estadisticaRecursosCategoria`,
`estadisticaActividadesSemana`) sí agregan por rango de fechas dentro del SQL,
sin traer las filas crudas a Java. Queda documentado como una inconsistencia
conocida entre los tres reportes, no como un olvido silencioso: con los volúmenes
de este proyecto no afecta el resultado ni el rendimiento de forma perceptible,
pero si `RESERVA` creciera mucho, Calendario/Actividades deberían migrar al mismo
patrón que ya usa Estadísticas.

**c) Driver `ojdbc8` 19.x en vez de `ojdbc11`.** El patrón de conexión del profe
especifica `ojdbc11:23.6.x`, pero esa versión del driver no es compatible con
Oracle Database Express Edition 11g Release 2 (11.2.0), que es el motor contra
el que corre este proyecto: `ojdbc11` apunta a versiones de Oracle mucho más
recientes y falla al conectar contra una instancia 11.2. `backend/pom.xml` fija
`com.oracle.database.jdbc:ojdbc8:19.23.0.0` en su lugar, con un comentario
explícito (`-- NO usar ojdbc11 23.x`) para que nadie lo "corrija" de vuelta sin
saber por qué. Verificado en esta sesión conectando contra una instancia Oracle
XE 11.2 real: login, CRUD completo y la transacción de `RESERVA_CREATE`
funcionaron sin errores de compatibilidad de driver.

## Estructura rápida

```
backend/
├── pom.xml
├── config.properties.example     (plantilla: copiar a config.properties, nunca commitear el real)
├── docs/                         (BD-ESPECIFICACION.md, REPORTE-BACKEND.md, BACKEND-P2.md)
└── src/
    ├── main/
    │   ├── java/una/eif206/
    │   │   ├── ServerMain.java            (punto de entrada del servidor)
    │   │   ├── data/      [una.eif206.data]     capa de acceso a datos (Oracle y en memoria)
    │   │   ├── service/   [una.eif206.service]  reglas de negocio (9 *Service + Service facade)
    │   │   ├── net/       [una.eif206.net]       Server/Worker (sockets SYNC/ASYNC, alertas)
    │   │   └── util/      [una.eif206.util]      interfaz consumida por la extracción con IA
    │   └── resources/
    │       ├── config.properties.example          (copiar a config.properties y rellenar groq.api.key)
    │       └── db/                                4 scripts .sql (ver "Crear la base de datos")
    └── test/java/una/eif206/        pruebas unitarias y de integración (Mockito + sockets reales)
```
