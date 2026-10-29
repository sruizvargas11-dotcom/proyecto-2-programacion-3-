# Auditoría de `backend/` — Proyecto 2 (EIF206)

Auditoría de solo lectura (sin modificar código, scripts ni pruebas). Fecha: 2026-10-28.
Rama: `backend-etapa3`. Ámbito: `backend/`, con verificaciones cruzadas en `frontend/` y `protocol/`
donde el enunciado lo exige (sin `java.sql`/Swing fuera del backend).

Hecha contra `docs/BACKEND-P2.md` (que resume el enunciado del Proyecto 2, los PDFs de sockets
del profe y su patrón de acceso a datos) más `backend/docs/BD-ESPECIFICACION.md` y una relectura
directa del código actual. Los PDFs y el zip originales del profe no están en el repo; se audita
contra su resumen ya internalizado en esos documentos.

---

## 1) Rúbrica de 9 módulos

| Módulo | Estado | Evidencia |
|---|---|---|
| Login | **Cumple** | `UsuarioService`/`ServicioUsuario`; `UsuarioServiceTest`(10), `ServicioUsuarioTest`(4), `UsuarioWorkerIT`(2), `ServerIT` |
| Reservas | **Cumple** | Creación atómica (id + recurso-por-categoría + disponibilidad, un solo candado/transacción); `ReservaServiceTest`(14), `ReservaServiceMockitoTest`(6), `ServicioReservaTest`(7), `ReservaConcurrenciaTest`(1), `ReservaConcurrenciaPorCategoriaTest`(1), `ReservaWorkerIT`(5) |
| Funcionarios | **Cumple** | CRUD completo; `FuncionarioServiceTest`(10), `ServicioFuncionarioTest`(4), `FuncionarioWorkerIT`(7) |
| Categorías | **Cumple** | CRUD + bloqueo si hay recursos asociados; `CategoriaServiceTest`(8), `ServicioCategoriaTest`(4), `CategoriaWorkerIT`(6) |
| Recursos | **Cumple** | `RecursoServiceTest`(7), `ServicioRecursoTest`(3), `RecursoWorkerIT`(5) |
| Calendarización | **Cumple** | Antes "parcial" (sin test dedicado); ahora `CalendarioServiceTest`(19: válidos, fechas inválidas, solapamientos, límites) |
| Actividades | **Cumple**, un poco delgado | `ActividadesServiceTest` solo tiene **1** prueba propia; el resto de cobertura es indirecta vía `ReportesWorkerIT`/`ServiceIntegrationIT` |
| Estadísticas | **Cumple** | `EstadisticasServiceTest`(2), `ServicioEstadisticasTest`(2), `ReportesWorkerIT` |
| Alertas | **Cumple** | Antes "falta" (0% hecho); ahora `DELIVER_MESSAGE` + `AlertaReservaWorkerIT`(4: 2 clientes, 3 clientes, desconexión abrupta, dos reservas simultáneas) |

## 2) Diseño de red del profe (PDFs de sockets)

| Punto | Estado | Evidencia |
|---|---|---|
| `Protocol` con `HOST/PORT/SYNC/ASYNC/DELIVER_MESSAGE/ERROR_*` + una constante `int` por método de `IService` | **Cumple** | 32 métodos en `IService`, 32 constantes de operación en `Protocol` |
| `Server` con `ServerSocket.accept()` en un hilo; un `Worker` por cliente | **Cumple** | `Server.aceptarClientes()` / `atenderSync()` |
| Dos sockets por Frontend (SYNC + ASYNC) | **Cumple** | `manejarConexion` distingue `Protocol.SYNC`/`Protocol.ASYNC` |
| Handshake: `SYNC` → `sid`; `ASYNC`+`sid` → `join(...)` | **Cumple** | `atenderSync`/`atenderAsync`/`Worker.join` |
| Petición `writeInt(op)+writeObject(args)+flush()`, respuesta `writeInt(status)+writeObject(resultado)` | **Cumple** | `Worker.responder(...)` |
| `Worker`: switch por **cada** operación de `IService` | **Parcial** | 29/32 cableadas. **Faltan a propósito**: `RESERVA_GENERAR_ID`, `RESERVA_BUSCAR_RECURSO_DISPONIBLE`, `RESERVA_ASIGNAR_RECURSOS_DISPONIBLES` — quedaron como lógica interna porque `RESERVA_CREATE` ahora resuelve id + recurso-por-categoría del lado del servidor de forma atómica (decisión explícita tomada en esta conversación). Contradice la letra del diseño del profe ("switch por cada operación"); si el entregable se revisa literal contra eso, hay que decidir si se documenta la excepción o se cablean igual (aunque exponerlas rompería la atomicidad). |
| `deliver_message` que **omite al emisor** | **Cumple** | `Server.broadcastReservaCreada` salta `w == creador`; nombre interno distinto (`broadcastReservaCreada` vs `deliver_message` del profe) pero la constante de protocolo en el cable es exactamente `Protocol.DELIVER_MESSAGE` |
| Un fallo en un Worker no tumba a los demás | **Cumple** | `broadcastReservaCreada` atrapa `IOException` por Worker y sigue |
| Trampas: OOS antes de OIS + flush en ambos extremos; `flush()` tras cada escritura; mismo orden lectura/escritura; `reset()` si se reenvía un objeto modificado; lista de Workers segura para hilos | **Cumple** | `CopyOnWriteArrayList<Worker>`; `reset()` en `responder()` y `enviarAlertaReserva()`; argumentos leídos en el orden exacto de `IService` |
| Hilo aceptador no debe colgarse de un hilo que ya retornó (`ServerMain.main()`) | **Cumple** | Corregido en esta sesión: hilo aceptador no-daemon, hilos de conexión/Worker daemon a propósito; `stop()` limpio con `join`; prueba de regresión en `ServerIT` |

## 3) Patrón de acceso a datos del profe

| Punto | Estado | Evidencia |
|---|---|---|
| `Servicio` base (`conectar`/`desconectar`, `Class.forName`, `DriverManager`, `Config.get`) | **Cumple** | Centraliza además `TIPO_CURSOR`/`registrarCursorSalida` — desviación ya discutida y aceptada, no es descuido |
| `GlobalException`/`NoDataException` | **Cumple** | — |
| Un `ServicioX` por entidad, `{call proc(?,?)}` / `{?=call funcion(?)}` con `REF_CURSOR`, parámetros *bind* reales (nunca literales), `execute()` para procedimientos, columnas por nombre, try-with-resources | **Cumple** | Verificado en los 6 `ServicioX` |
| `Repositorio` + `OracleRepositorio` + `MemoriaRepositorio` | **Cumple** | 29 métodos, cobertura 1:1 en ambas implementaciones |
| Operación multi-paso (crear reserva) en una sola conexión, `autoCommit(false)`+commit/rollback, protegida contra concurrencia con `Lock` | **Cumple y excede** | Ahora también generación de id y resolución de recurso-por-categoría dentro del mismo candado/transacción (no solo "verificar disponibilidad + insertar") |
| Script re-ejecutable, paquete `types.ref_cursor`, verificación `user_objects`/`user_errors` al final | **Cumple** | Corrido contra Oracle real en esta sesión: 0 `INVALID` |
| Driver Oracle | **Desviación correcta documentada** | El PDF pide `ojdbc11:23.6.x`; el pom usa `ojdbc8:19.23.0.0` con comentario explícito de por qué (incompatible con XE 11.2) — es una corrección, no un descuido |
| Procedimientos "para calendario/actividades/estadísticas" por rango de fechas | **Parcial** | `listarReservaPorFecha`/`listarReservaPorRango` existen en el script (documentados en `BD-ESPECIFICACION.md §5.4` como pensados justo para esto) pero **ningún código Java los llama hoy**: `CalendarioService`/`ActividadesService` traen *todas* las reservas y filtran en Java, en vez de filtrar en SQL. Funciona y está probado, pero no sigue el patrón previsto. |

## 4) Requisitos transversales

| Punto | Estado | Evidencia |
|---|---|---|
| Pruebas unitarias (Surefire) | **Cumple** | 180 tests, 0 fallos |
| Pruebas de integración (Failsafe) | **Cumple** | 53 tests (47 ejecutados + 6 `OracleRepositorioIT` cuando hay `DB_URL`), 0 fallos |
| XML eliminado de `backend/` | **Cumple** | `Data`/`XMLHelper`/`backend/data.xml`/`PersistenciaXMLTest`/dependencias JAXB borrados; grep sin resultados |
| XML eliminado del repo completo | **Falta (fuera de `backend/`)** | `data.xml` en la **raíz** del repo sigue existiendo; está fuera del alcance de `backend/` y ya se señaló antes — sigue pendiente una decisión |
| Sin `javax.swing`/`java.awt` en `backend/` | **Cumple** | grep sin resultados |
| Sin `java.sql`/`oracle`/`ojdbc` en `frontend/`/`protocol/` | **Cumple** | grep sin resultados en ambos |
| Sin `printStackTrace` | **Cumple** | única aparición es un comentario, no una llamada real |
| Sin contraseñas/claves en el repo | **Cumple** | `.gitignore` cubre ambos `config.properties`; sin valores reales en el árbol de trabajo |
| Sin reportes `.txt` en `backend/` | **Cumple** | — |
| Script SQL | **Cumple** | Corrido limpio desde cero contra Oracle real: 6 tablas, 3 secuencias, 15 procedimientos, 22 funciones, 0 `INVALID`, filas semilla exactas (3/3/3) |
| `backend/README.md` | **Cumple** | requisitos, variables de entorno, crear BD, arrancar `ServerMain`, correr pruebas |
| `mvn clean verify` sin Oracle | **Cumple** | `BUILD SUCCESS`, 180+47 tests, sin tocar Oracle |

---

## Lo que falta para la entrega (priorizado)

1. **Nada está comiteado.** El último commit es un merge anterior a esta sesión; la rama
   `backend-etapa3` tiene archivos sin comitear que cubren *todo* el trabajo de Etapas 2, 3 y 4
   (capa de datos sin XML, Server/Worker/Alertas, atomicidad de reservas, el fix del hilo daemon,
   todas las pruebas nuevas, el README). Sin commit + push no hay entrega.
2. **Frontend no tiene un cliente de red real todavía** — solo existe `FakeService` (datos en
   memoria). El backend queda listo para recibir conexiones, pero la integración Frontend↔Backend
   real (el `Proxy`/sockets del lado del frontend) no está, y eso es parte del "Frontend y Backend
   deben poder correr en máquinas separadas" del enunciado. Está fuera de `backend/`, pero bloquea
   una demo end-to-end real.
3. **3 operaciones de `IService` no están cableadas en `Worker`** (`generarId`,
   `buscarRecursoDisponible`, `asignarRecursosDisponibles`) — desviación deliberada de esta
   conversación, pero contradice literalmente "switch por cada operación" del diseño del profe.
   Vale documentarlo explícitamente en la entrega o decidir cablearlas igual.
4. **`data.xml` de la raíz** (fuera de `backend/`) sigue sin borrarse — pendiente una decisión,
   ya señalada antes.
5. **Sin prueba dedicada para `ReservaIAService`** (depende de API externa) y
   **`ActividadesServiceTest` con una sola prueba** — gaps menores, no bloqueantes.
6. **Calendario/Actividades no usan las funciones SQL por rango de fechas** que el propio script
   ya expone para eso — funciona, pero no es el patrón previsto; cosmético, no bloqueante.

No se modificó ningún archivo de código, script ni prueba durante esta auditoría.
