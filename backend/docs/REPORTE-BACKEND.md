# Reporte del estado actual — Backend

Generado por auditoría de solo lectura (sin modificar código, scripts ni pruebas). Fecha: 2026-10-28.
Rama: `sebastian_ruiz_backend_dev`. Ámbito: exclusivamente `backend/`.

Convención: todo lo que no se pudo comprobar directamente dice **"no verificado"**, nunca se supone.

> **Actualización (misma fecha, trabajo posterior a esta auditoría):** varios puntos marcados abajo
> como pendientes ya se cerraron. No se reescribió el resto del reporte (sigue siendo la foto de ese
> momento), solo se deja esta nota para no inducir a error:
> - **Etapa 3 completa**: `Server`/`Worker` (sockets SYNC/ASYNC), las 23 operaciones restantes de
>   `IService` cableadas en `Worker`, y la alerta `DELIVER_MESSAGE` al crear una reserva (sección 7).
>   `reservaCreate` ahora resuelve id + recurso-por-categoría de forma atómica en el repositorio.
> - **Limpieza de XML completa** (sección 5 y riesgo #4 de la sección 10): se eliminaron
>   `Data.java`, `XMLHelper.java`, `data.xml`, `PersistenciaXMLTest.java` y las 2 dependencias JAXB
>   del `pom.xml`. `MemoriaRepositorio` ya no depende de `Data`: tiene sus propios campos en memoria.
> - **`CalendarioServiceTest` ya existe** (19 pruebas) — la "Calendarización: Parcial" de la sección 6
>   queda "Completa".
> - Ver `README.md` (nuevo, en `backend/`) para cómo levantar el servidor y correr las pruebas.

---

## 1. Estructura de `backend/` (3 niveles, con paquete por clase)

```
backend/
├── pom.xml
├── data.xml                              (semilla XML real: 1 admin, 4 funcionarios, 3 categorias, 3 recursos, 5 reservas)
├── config.properties.example
├── README-conexion.md                    (archivo de 3 bytes, sin contenido util)
├── docs/
│   └── BD-ESPECIFICACION.md
└── src/
    ├── main/
    │   ├── java/una/eif206/
    │   │   ├── data/      [paquete una.eif206.data]    18 tipos (ver detalle abajo)
    │   │   ├── service/   [paquete una.eif206.service] 10 clases (9 *Service + Service facade)
    │   │   └── util/      [paquete una.eif206.util]    1 interfaz (ReservaExtractorService)
    │   └── resources/db/                                4 scripts .sql
    └── test/
        └── java/una/eif206/
            ├── (raiz)      [paquete una.eif206]         ServiceTest, ServiceIntegrationIT
            ├── data/       [paquete una.eif206.data]    13 clases de prueba
            └── service/    [paquete una.eif206.service] 7 clases de prueba
```

**Paquete `una.eif206.data`** (main): `Data`, `XMLHelper`, `NoDataException`, `GlobalException`, `RepositorioException`, `ConexionProvider` (interfaz), `Config`, `ConexionCheck`, `Servicio` (abstracta), `ServicioUsuario`, `ServicioFuncionario`, `ServicioCategoria`, `ServicioRecurso`, `ServicioEstadisticas`, `ServicioReserva`, `Repositorio` (interfaz), `OracleRepositorio`, `MemoriaRepositorio`.

**Paquete `una.eif206.service`** (main): `UsuarioService`, `FuncionarioService`, `CategoriaService`, `RecursoService`, `ReservaService`, `EstadisticasService`, `ActividadesService`, `CalendarioService`, `ReservaIAService`, `Service` (facade que implementa `IService` del módulo `protocol`).

**Paquete `una.eif206.util`** (main): `ReservaExtractorService` (interfaz consumida por langchain4j `AiServices`).

Nota: las clases de modelo (`Usuario`, `Funcionario`, `Reserva`, etc.) **no viven en `backend/`**; están en el módulo `protocol/` (fuera del ámbito de esta auditoría, que fue limitada a `backend/`).

---

## 2. Capa de datos

### 2.1 Procedimientos/funciones llamados por cada ServicioX

| ServicioX | Procedimiento/función llamado | ¿Existe en `script_reservas_oracle11g.sql`? | ¿VALID en Oracle (user_objects)? |
|---|---|---|---|
| `ServicioUsuario` | `loginUsuario` (función, L306) | Sí | no verificado (sin conexión) |
| | `cambiarClave` (procedimiento, L321) | Sí | no verificado |
| `ServicioFuncionario` | `insertarFuncionario` (L227) | Sí | no verificado |
| | `modificarFuncionario` (L240) | Sí | no verificado |
| | `eliminarFuncionario` (L256) | Sí | no verificado |
| | `buscarFuncionario` (función, L264) | Sí | no verificado |
| | `listarFuncionario` (función, L278) | Sí | no verificado |
| | `listarFuncionarioTexto` (función, L290) | Sí | no verificado |
| `ServicioCategoria` | `insertarCategoria` (L332) | Sí | no verificado |
| | `modificarCategoria` (L342) | Sí | no verificado |
| | `eliminarCategoria` (L351) | Sí | no verificado |
| | `buscarCategoria` (función, L359) | Sí | no verificado |
| | `listarCategoria` (función, L370) | Sí | no verificado |
| | `listarCategoriaTexto` (función, L379) | Sí | no verificado |
| `ServicioRecurso` | `insertarRecurso` (L395) | Sí | no verificado |
| | `modificarRecurso` (L406) | Sí | no verificado |
| | `eliminarRecurso` (L419) | Sí | no verificado |
| | `buscarRecurso` (función, L427) | Sí | no verificado |
| | `listarRecurso` (función, L442) | Sí | no verificado |
| | `listarRecursoPorCategoria` (función, L455) | Sí | no verificado |
| `ServicioReserva` | `insertarReserva` (L479) | Sí | no verificado |
| | `insertarReservaRecurso` (L493) | Sí | no verificado |
| | `cancelarReserva` (L502) | Sí | no verificado |
| | `buscarReserva` (función, L510) | Sí | no verificado |
| | `listarReserva` (función, L523) | Sí | no verificado |
| | `listarReservaPorFuncionario` (función, L534) | Sí | no verificado |
| | `listarRecursoPorReserva` (función, L578) | Sí | no verificado |
| | `listarRecursosOcupados` (función, L598) | Sí | no verificado |
| | `siguienteIdReserva` (función, L473) | Sí | no verificado |
| | `buscarFuncionario` (función, L264, compartida) | Sí | no verificado |
| `ServicioEstadisticas` | `estadisticaRecursosCategoria` (función, L621) | Sí | no verificado |
| | `estadisticaActividadesSemana` (función, L643) | Sí | no verificado |

**Ningún procedimiento/función faltante**: los 30 nombres distintos invocados desde los 6 `ServicioX` existen todos, con ese nombre exacto, en `script_reservas_oracle11g.sql` (699 líneas). No se encontró ninguna discrepancia de nombre.

El script define además `formatearId` (L129, función auxiliar interna), `listarReservaPorFecha` (L548) y `listarReservaPorRango` (L562), que **no son llamadas por ningún `ServicioX` actual** — no es un error, pero quedan sin consumidor en el código Java de hoy.

La validez real en Oracle (`SELECT status FROM user_objects WHERE status='VALID'`) **no se pudo comprobar**: no hay conexión disponible en este entorno (ver sección 9).

### 2.2 Apertura de conexión y origen de `DB_URL`/`DB_USER`/`DB_PASSWORD`

`Servicio.conexionReal()` (privado, en `una.eif206.data.Servicio`):
```java
Class.forName("oracle.jdbc.OracleDriver");
String url = System.getenv().getOrDefault("DB_URL", "jdbc:oracle:thin:@localhost:1521:XE");
String usuario = System.getenv().getOrDefault("DB_USER", "system");
String clave = System.getenv("DB_PASSWORD");
if (clave == null || clave.isEmpty()) throw new SQLException("Defina DB_PASSWORD como variable de entorno");
return DriverManager.getConnection(url, usuario, clave);
```
Las tres variables salen de `System.getenv(...)` directo (no de `Config`, no de `config.properties`). Cada `ServicioX` recibe esta conexión a través de `ConexionProvider` (inyectable; en producción es `Servicio::conexionReal`, en pruebas se sustituye por un `Connection` simulado con Mockito).

Aparte, `ConexionCheck` (herramienta de diagnóstico manual) y la limpieza de `OracleRepositorioIT` siguen usando `Config.get("DB_URL"/"DB_USER"/"DB_PASSWORD")`, que sí cae a `backend/config.properties` (no versionado) si la variable de entorno no está definida. Es decir: hoy hay **dos rutas de resolución de credenciales distintas** dentro de `backend/` — los `ServicioX` reales exigen variables de entorno puras; `ConexionCheck`/limpieza de IT aceptan también `config.properties`.

---

## 3. Repositorio

`Repositorio` (interfaz) declara **29 métodos**. Tanto `OracleRepositorio` como `MemoriaRepositorio` tienen **29 `@Override`** cada una — cobertura 1:1 verificada en ambas, sin huecos.

- **`OracleRepositorio`**: delega cada método en el `ServicioX` correspondiente (`ServicioUsuario`, `ServicioFuncionario`, `ServicioCategoria`, `ServicioRecurso`, `ServicioReserva`, `ServicioEstadisticas`). Los métodos de solo lectura (`listarX`, `buscarXPorId`, `login`, estadísticas) envuelven `GlobalException`/`NoDataException` como `RepositorioException` (sin chequear); los de escritura declaran `throws GlobalException, NoDataException`.
- **`MemoriaRepositorio`**: implementa la misma interfaz operando sobre `Data` (listas en memoria), con un `ReentrantLock` para que `crearReserva` sea atómica (verificar disponibilidad + insertar) frente a llamadas concurrentes. Usada por los tests unitarios y como repositorio sin Oracle.

---

## 4. Services (9) — reglas de negocio y dependencia de Data/XML

| Service | Reglas de negocio que valida | ¿Depende de Data/XML? |
|---|---|---|
| `UsuarioService` | `cambiarClave`: exige que `claveActual` coincida con la clave real antes de delegar | No — solo usa `Repositorio` |
| `FuncionarioService` | `create`: ID y nombre obligatorios, anti-duplicado por ID, clave por defecto = ID si viene vacía, rol por defecto `FUNCIONARIO`; `update`/`delete`: exigen que el funcionario exista | No |
| `CategoriaService` | `delete`: bloquea si existen recursos asociados a la categoría; `update`/`delete` exigen existencia previa | No |
| `RecursoService` | `create`: exige categoría asignada y anti-duplicado por ID; `update`/`delete` exigen existencia previa | No |
| `ReservaService` | `create`: actividad obligatoria, al menos un recurso, horario válido (inicio antes que fin), anti-duplicado por ID; `asignarRecursosDisponibles`: asigna el primer recurso libre por categoría o reporta todas las categorías sin disponibilidad; `cancelar`: solo permite cancelar reservas con fecha futura | No |
| `EstadisticasService` | Sin reglas propias — delega directo en `Repositorio.estadisticasRecursos`/`estadisticasActividades` | No |
| `ActividadesService` | Construye la matriz semanal (día × hora) de actividades a partir de reservas activas en la semana de una fecha de referencia, ignorando canceladas | No |
| `CalendarioService` | Construye la matriz de disponibilidad hora × recurso para una fecha y categoría, marcando solo reservas con estado `ACTIVA` | No |
| `ReservaIAService` | Extrae datos de reserva de una frase en lenguaje natural vía LLM (Groq, con *fallback* silencioso al demo público de langchain4j); lee `groq.api.key` de `config.properties` (clave de API, no es `DB_PASSWORD`) | No (pero depende de red externa y de `config.properties` para la API key) |

Ninguno de los 9 `Service` ni la clase facade `Service` referencian `Data` o `XMLHelper`: todos reciben un `Repositorio` inyectado. Confirmado por lectura completa y por `grep` (sin coincidencias de `Data`/`XMLHelper`/`javax.xml`/`jakarta.xml` en `una.eif206.service`).

---

## 5. XML pendiente

Archivos/clases/dependencias relacionadas con XML/JAXB que **siguen presentes** en `backend/`:

| Elemento | Qué es | Quién lo usa hoy |
|---|---|---|
| `una.eif206.data.Data` | Clase anotada `@XmlRootElement`/`@XmlElement` (JAXB), con datos semilla embebidos en su constructor | `MemoriaRepositorio` (como almacén en memoria) y su propio test `PersistenciaXMLTest` |
| `una.eif206.data.XMLHelper` | Carga/guarda un `Data` contra un archivo vía `JAXBContext` | Solo `PersistenciaXMLTest` (7 pruebas) — ningún código de producción lo invoca |
| `backend/data.xml` | Archivo de datos semilla real (2697 bytes: 1 admin, 4 funcionarios, 3 categorías, 3 recursos, 5 reservas) | No referenciado por ningún código de producción (ni `OracleRepositorio` ni los `Service`); `XMLHelper.instance()` apuntaría a él pero nada lo llama en producción |
| `pom.xml` | Dependencias `jakarta.xml.bind-api:4.0.0` y `com.sun.xml.bind:jaxb-impl:4.0.0`, con el comentario explícito `<!-- Temporal: XML se elimina en la Etapa 2 -->` | Compilación de `Data`/`XMLHelper` |
| `PersistenciaXMLTest.java` | 7 pruebas unitarias de `XMLHelper` | — |

El camino de producción real (`Service` → `OracleRepositorio` → `ServicioX` → Oracle) **ya no toca XML en absoluto**. Lo único pendiente es una limpieza: borrar esos 4 archivos y las 2 dependencias del `pom.xml` cuando se decida cerrar formalmente la Etapa 2 (no se hizo en esta auditoría porque la tarea pedía solo leer y reportar).

---

## 6. Rúbrica

| Funcionalidad | Estado | Clase(s) que lo implementa | ¿Tiene pruebas? |
|---|---|---|---|
| Login | Completo | `UsuarioService.login`/`cambiarClave`, `ServicioUsuario` (`loginUsuario`, `cambiarClave`) | Sí — `UsuarioServiceTest` (10), `ServicioUsuarioTest` (4), más `ServiceTest`/`ServiceIntegrationIT` |
| Reservas | Completo | `ReservaService` (crear/cancelar/asignación automática), `ServicioReserva`, lock de concurrencia en `MemoriaRepositorio` | Sí — `ReservaServiceTest` (14), `ServicioReservaTest` (4), `ReservaConcurrenciaTest` (1), `ServiceIntegrationIT` |
| Funcionarios | Completo | `FuncionarioService` (CRUD), `ServicioFuncionario` | Sí — `FuncionarioServiceTest` (10), `ServicioFuncionarioTest` (4) |
| Categorías | Completo | `CategoriaService` (CRUD), `ServicioCategoria` | Sí — `CategoriaServiceTest` (8), `ServicioCategoriaTest` (4) |
| Recursos | Completo | `RecursoService` (CRUD), `ServicioRecurso` | Sí — `RecursoServiceTest` (7), `ServicioRecursoTest` (3) |
| Calendarización | Parcial* | `CalendarioService.getCalendario` | *Sin clase de prueba dedicada* (`CalendarioServiceTest` no existe); cubierto solo indirectamente dentro de `ServiceTest`/`ServiceIntegrationIT` |
| Actividades | Completo | `ActividadesService.getActividadesSemana` | Sí — `ActividadesServiceTest` (1), más `ServiceTest`/`ServiceIntegrationIT` |
| Estadísticas | Completo | `EstadisticasService`, `ServicioEstadisticas` (`estadisticaRecursosCategoria`, `estadisticaActividadesSemana`) | Sí — `EstadisticasServiceTest` (2), `ServicioEstadisticasTest` (2) |
| Alertas | Falta | Ninguna — no existe `AlertaReserva` ni mecanismo de notificación | No — 0 pruebas, 0 código (ver sección 7) |

*Calendarización está marcada "parcial" únicamente por falta de una clase de prueba unitaria dedicada al `Service`; la lógica en sí está completa y se ejecuta en los tests de integración existentes.

---

## 7. Red y alertas

Búsqueda en todo el repositorio (no solo `backend/`) de `Server`, `Worker`, socket `ASYNC` y `AlertaReserva`:

- **No existe** ninguna clase `Server`, `Worker` ni `AlertaReserva` en `backend/`, `frontend/` ni `protocol/`.
- **No existe** ningún uso de `ServerSocket` ni de un socket ASYNC real en el código.
- La única aparición de estos términos es:
  - `protocol/src/main/java/una/eif206/protocol/Protocol.java`: la constante `public static final int ASYNC = 2;` — es solo una constante de protocolo (un número), sin ninguna lógica de red asociada.
  - `docs/BACKEND-P2.md`: menciona `Server`, `Worker`, `AlertaReserva`/`deliver_message` y los sockets SYNC/ASYNC como **plan de la Etapa 3**, todavía no iniciada.

**Confirmado**: tal como se esperaba, Server/Worker/socket ASYNC/`AlertaReserva` **no están implementados todavía**. Es trabajo 100% pendiente de Etapa 3.

---

## 8. Pruebas — `mvn clean verify`

Ejecutado con Maven embebido de IntelliJ (`mvn` no está en el `PATH` del sistema) desde la raíz del repo, módulos `backend` + `protocol` (`-am`). **Sin `DB_URL`/`DB_USER`/`DB_PASSWORD` definidas** en este entorno (confirmado antes de correr: ninguna de las tres variables está presente) — ningún valor de conexión fue impreso ni usado.

```
BUILD SUCCESS
```

| Fase | Tests | Fallos | Errores | Omitidos |
|---|---|---|---|---|
| Unitarios (Surefire) | 131 | 0 | 0 | 0 |
| Integración (Failsafe) | 13 | 0 | 0 | 6 |
| **Total** | **144** | **0** | **0** | **6** |

Los 6 omitidos son los 6 `@Test` de `OracleRepositorioIT`, saltados por su `@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")` al no haber `DB_URL`. `ServiceIntegrationIT` (7 pruebas) sí corrió completa porque usa `MemoriaRepositorio`, no Oracle.

**Clases de prueba (22 en total):**

*Unitarias (20, fase Surefire):* `ConexionCheckTest`, `ConfigTest`, `PersistenciaXMLTest`, `RepositorioContractTest`, `ReservaConcurrenciaTest`, `ServicioCategoriaTest`, `ServicioEstadisticasTest`, `ServicioFuncionarioTest`, `ServicioRecursoTest`, `ServicioReservaTest`, `ServicioTest`, `ServicioUsuarioTest`, `ActividadesServiceTest`, `CategoriaServiceTest`, `EstadisticasServiceTest`, `FuncionarioServiceTest`, `RecursoServiceTest`, `ReservaServiceTest`, `UsuarioServiceTest`, `una.eif206.ServiceTest`.

*Integración (2, fase Failsafe):* `OracleRepositorioIT` (6 pruebas, omitidas), `una.eif206.ServiceIntegrationIT` (7 pruebas, ejecutadas).

No hay `CalendarioServiceTest` ni `ReservaIAServiceTest` — no existen esos archivos (ver sección 6 y 10).

---

## 9. Base de datos

**No verificado.** `DB_URL`, `DB_USER` y `DB_PASSWORD` no están definidas como variables de entorno en este proceso (se comprobó solo su *presencia*, nunca su valor, antes de intentar nada). Sin esas variables no hay forma de abrir una conexión real, por lo tanto no se pudo:

- Contar tablas, secuencias, procedimientos ni funciones.
- Consultar `user_objects` para objetos `INVALID`.
- Contar filas de las tablas semilla (`USUARIO`, `CATEGORIA_RECURSO`, `RECURSO`, `RESERVA`, `RESERVA_RECURSO`).

No se intentó ningún valor por defecto ni se asumió ningún dato de la base real: todo lo de esta sección queda explícitamente como **no verificado**.

---

## 10. Riesgos y pendientes (ordenados por prioridad)

1. **(Alta) Verificación contra Oracle real pendiente.** Sin `DB_URL`/`DB_USER`/`DB_PASSWORD` en este entorno no se pudo confirmar que los 30 procedimientos/funciones estén `VALID` en `user_objects`, ni contar filas/objetos (secciones 2 y 9). Bloquea el cierre formal de la Etapa 2.
2. **(Alta) Etapa 3 no iniciada.** `Server`, `Worker`, sockets SYNC/ASYNC, `AlertaReserva`/`deliver_message` — 0% hecho (sección 7). Es la pieza más grande pendiente del backend.
3. **(Media) Etapa 4** — no se encontró un plan explícito de alcance más allá de lo que menciona `docs/BACKEND-P2.md`; su contenido exacto queda **no verificado** en esta auditoría.
4. **(Media) Limpieza de XML pendiente.** `Data.java`, `XMLHelper.java`, `data.xml`, `PersistenciaXMLTest.java` y las 2 dependencias JAXB del `pom.xml` siguen en el repo aunque el camino de producción ya no las usa (sección 5).
5. **(Media) `README-conexion.md` está vacío** (3 bytes, sin contenido útil) — no documenta cómo levantar el backend ni conectar a Oracle.
6. **(Baja) Huecos de prueba puntuales:** no existe `CalendarioServiceTest` (la lógica de calendarización solo se prueba indirectamente) ni ninguna prueba para `ReservaIAService` (depende de una API externa).
7. **(Informativo, no bloqueante) Reestructuración de módulos sin commitear.** `git status` muestra ~111 archivos (todo el movimiento de `sistema-reservas/` hacia `backend/`+`frontend/`+`protocol/`) todavía sin commit. No se tocó nada de esto en esta auditoría (se pidió explícitamente no commitear), pero cualquier entrega final deberá commitearlo.
8. **(Informativo) Desviaciones de estilo del profe que quedan abiertas por decisión explícita** (no por descuido, ya discutidas y aceptadas en una conversación previa):
   - `Servicio.TIPO_CURSOR`/`registrarCursorSalida()` centralizados, en vez de `Types.REF_CURSOR` inline en cada `ServicioX` como hace el profe.
   - `conectar()`/`desconectar()` centralizados en `Servicio`, en vez de repetir el try/catch en cada método de cada `ServicioX`.
   - No se lanza `NoDataException` en listados/búsquedas vacías (se retorna lista vacía o `null`); el profe sí la lanza en ese caso.
   - Mensajes de error mapeados por código Oracle (`errorSql`/`mensajeError`) en vez del genérico `"Sentencia no valida"` del profe para cualquier `SQLException`.

---

## Resumen (10 líneas)

1. La capa de datos (`Servicio` + 6 `ServicioX`) llama a 30 procedimientos/funciones, y los 30 existen con el mismo nombre en `script_reservas_oracle11g.sql`; su validez real en Oracle queda **no verificada** por falta de conexión.
2. La conexión real se abre con `Class.forName` + `System.getenv` puro para `DB_URL`/`DB_USER`/`DB_PASSWORD`, igual que el ejemplo del profesor; `ConexionCheck` conserva además la ruta con `Config`/`config.properties`.
3. `Repositorio` tiene 29 métodos y tanto `OracleRepositorio` como `MemoriaRepositorio` los cubren al 100%, sin huecos.
4. Los 9 `Service` contienen toda la lógica de negocio (validaciones, anti-duplicados, horarios, disponibilidad) y ninguno depende de `Data`/XML — todos usan `Repositorio`.
5. XML sigue presente pero aislado (`Data`, `XMLHelper`, `data.xml`, 1 test, 2 dependencias del pom) y no lo usa ningún camino de producción; solo falta borrarlo.
6. De las 9 funcionalidades de la rúbrica, 8 están completas (Login, Reservas, Funcionarios, Categorías, Recursos, Actividades, Estadísticas) con pruebas dedicadas; Calendarización está completa en lógica pero sin test dedicado (parcial); Alertas no existe.
7. Confirmado: no hay `Server`, `Worker`, socket ASYNC real ni `AlertaReserva` en ningún módulo — es trabajo 100% de Etapa 3, sin empezar.
8. `mvn clean verify` dio `BUILD SUCCESS`: 144 tests totales (131 unitarios + 13 de integración), 0 fallos, 0 errores, 6 omitidos (los de `OracleRepositorioIT`, por falta de `DB_URL`).
9. No se pudo auditar la base de datos real (tablas, objetos `INVALID`, filas semilla) porque `DB_URL`/`DB_USER`/`DB_PASSWORD` no están definidas en este entorno — sección completa marcada "no verificado".
10. **Avance estimado del backend: ~55-60% si se mide solo la Etapa 2 (capa de datos/servicios, prácticamente lista salvo verificar contra Oracle real y limpiar XML); ~35-40% si se incluye la Etapa 3 completa (red/alertas), que está en 0%.**
