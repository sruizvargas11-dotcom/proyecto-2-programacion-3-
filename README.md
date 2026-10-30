# Sistema de Reserva de Recursos

Proyecto #2 del curso **EIF206 – Programación III** de la Universidad Nacional (UNA).

Es una aplicación de escritorio para que los funcionarios de una organización reserven recursos compartidos (laptops, salas de reuniones, proyectores…) y para que un administrador gestione el catálogo. En el Proyecto 1 todo vivía en un solo programa que guardaba los datos en XML. En este proyecto lo partimos en dos: un **Frontend** con las pantallas y un **Backend** que tiene la lógica y la base de datos. Los dos se comunican por **sockets**. Así pueden correr en computadoras distintas, y varios usuarios pueden trabajar al mismo tiempo contra el mismo servidor.

Lo que más nos gusta del resultado: cuando alguien crea una reserva, **a los demás usuarios conectados les aparece una alerta al instante**, sin tener que refrescar nada.

**Integrantes**
- Thomas Sandí Aguilar
- Sebastián Ruiz Vargas
- Michael Sauceda Díaz

---

## Contenido

1. [Qué se puede hacer](#qué-se-puede-hacer)
2. [Cómo está armado](#cómo-está-armado)
3. [Requisitos](#requisitos)
4. [Puesta en marcha](#puesta-en-marcha)
5. [Usuarios de prueba](#usuarios-de-prueba)
6. [Usar dos computadoras](#usar-dos-computadoras)
7. [Pruebas](#pruebas)
8. [Estructura del repositorio](#estructura-del-repositorio)
9. [Problemas comunes](#problemas-comunes)
10. [Más documentación](#más-documentación)

---

## Qué se puede hacer

Al iniciar sesión, la ventana muestra pestañas distintas según el rol. El título indica quién entró, por ejemplo `SISTEMA DE RESERVAS - 222 (FUNCIONARIO)`.

**Administrador**
- **Funcionarios:** crear, modificar, buscar y borrar. Al crear un funcionario, su clave inicial es su propio id.
- **Categorías** y **Recursos:** mantener el catálogo. No se puede borrar una categoría que todavía tiene recursos.
- **Calendarización, Actividades y Estadísticas:** ver la ocupación de los recursos y gráficos por rango de fechas.

**Funcionario**
- **Reservas:** reservar una o varias categorías para una fecha y una franja horaria. El sistema elige solo un recurso libre de cada categoría. También se pueden cancelar reservas futuras.
- **Extraer con IA:** escribir algo como *"reunión con clientes el viernes de 8 a 10 con una laptop"* y dejar que la IA llene el formulario.
- **Calendarización, Actividades y Estadísticas**, igual que el administrador.

**Para todos**
- **Alertas:** un panel a la derecha, visible en todas las pestañas, que muestra las reservas que *otros* usuarios van creando durante la sesión. Cada alerta se puede **ver** (encargado, fecha, horario y recursos) o **descartar**. Descartar solo la quita de tu lista, no afecta a nadie más.
- **Reportes en PDF** desde cada pantalla.
- **Cambiar la clave.**

---

## Cómo está armado

El proyecto es un *monorepo* de Maven con **tres módulos**:

```
proyecto-2-programacion-3-/
├── pom.xml        ← proyecto padre (une los tres módulos)
├── protocol/      ← lo que comparten los dos lados
├── backend/       ← el servidor
└── frontend/      ← la aplicación de escritorio
```

- **`protocol`** tiene las entidades (`Funcionario`, `Reserva`, `Recurso`…), la interfaz `IService` con todas las operaciones del sistema y la clase `Protocol` con las constantes de red (puerto, códigos de operación). Los dos lados dependen de este módulo, y **el Frontend nunca depende del Backend**.
- **`backend`** escucha conexiones, atiende a cada cliente en su propio hilo y es el único que habla con la base de datos.
- **`frontend`** son las pantallas en Swing con el patrón MVC del curso. No sabe nada de bases de datos ni de SQL.

### El camino de un pedido

```
 FRONTEND                                                     BACKEND
 ┌────────────────────────────────┐                ┌──────────────────────────────┐
 │ Vista ⇄ Controller ⇄ Service   │══ socket SYNC ═▶│ Server → Worker → Service    │
 │                    (Proxy)     │◀═══════════════│              │                │
 │                                │                │              ▼                │
 │ AlertaController ◀ SocketListener ◀ socket ASYNC═│  avisa a los demás clientes  │
 └────────────────────────────────┘                │              │                │
                                                   │              ▼                │
                                                   │        Oracle (JDBC)          │
                                                   └──────────────────────────────┘
```

**Patrón Proxy.** Los controllers del Frontend reciben un `IService` y llaman a sus métodos como si todo fuera local (`service.findAllCategorias()`, `service.create(reserva)`…). Detrás hay una clase `Service` que implementa esa misma interfaz, pero en lugar de resolver la operación la **envía por el socket** y devuelve lo que responde el servidor. Existe también un `FakeService` con datos en memoria que implementa la misma interfaz. Gracias a eso pudimos construir y probar todas las pantallas antes de que existiera el servidor, cambiando una sola línea.

**Dos sockets por cliente.** Siguiendo el diseño visto en clase (Sincrónico / Asincrónico):
- El **socket SYNC** sirve para pedidos y respuestas. Al conectarse, el cliente manda `SYNC` y el servidor le asigna un identificador de sesión, el **`sid`**. Cada operación viaja como `writeInt(operación)` + `writeObject(argumentos)`, y la respuesta vuelve como `readInt(estado)` + `readObject(resultado)`. Se usan `ObjectStreams`, no JSON.
- El **socket ASYNC** sirve solo para *recibir* alertas. El cliente lo abre mandando `ASYNC` y su `sid`, así el servidor sabe a quién pertenece. Un hilo aparte (`SocketListener`) se queda esperando y entrega cada alerta a la pantalla con `SwingUtilities.invokeLater`, porque Swing solo permite tocar la interfaz desde su propio hilo.

**Reservas sin choques.** Crear una reserva es **una sola operación** en el servidor: el Frontend manda la reserva con el id vacío y, por cada categoría elegida, un recurso "comodín" con esa categoría. El servidor genera el id y elige los recursos libres dentro de la misma transacción y bajo un candado. Así, si dos personas reservan la última laptop al mismo tiempo, una la obtiene y la otra recibe un mensaje claro, en lugar de que las dos queden con la misma.

**La ventana no se congela.** Las operaciones que pueden tardar (iniciar sesión, estadísticas, IA) se ejecutan con `SwingWorker`. Si el servidor se apaga mientras la aplicación está abierta, el cliente lo detecta, avisa y se cierra de forma ordenada.

---

## Requisitos

- **Java 21** o superior.
- **Maven 3.8+**. Sirve el que viene incluido en IntelliJ IDEA.
- **Oracle Database XE 11g** (Express Edition 11.2), con **SQL Developer** o SQL\*Plus para ejecutar los scripts.
- *(Opcional)* Una **API key de Groq**, gratuita en [console.groq.com](https://console.groq.com/), para la extracción con IA. Sin ella, todo lo demás funciona normalmente.

---

## Puesta en marcha

Los pasos 1 y 2 se hacen **una sola vez**.

### 1. Crear el usuario de la base de datos

La aplicación no usa el usuario `SYSTEM`: trabaja con un esquema propio. En SQL Developer, conectado como **`SYSTEM`**, ejecute lo siguiente (cambie la clave por una suya):

```sql
CREATE USER reservas_app IDENTIFIED BY SuClave123
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;

GRANT CONNECT, RESOURCE TO reservas_app;
```

### 2. Crear las tablas y los datos iniciales

1. En SQL Developer, cree una **conexión nueva** con el usuario `reservas_app` (host `localhost`, puerto `1521`, SID `XE`).
2. Abra `backend/src/main/resources/db/script_reservas_oracle11g.sql`.
3. **Antes de ejecutar**, revise que en la esquina superior derecha de la pestaña esté seleccionada la conexión **`reservas_app`**, no `SYSTEM`.
4. Ejecútelo como script con **F5**. Al inicio pueden aparecer errores de *"does not exist"*: son normales, porque el script intenta borrar objetos que todavía no existen para poder ejecutarse varias veces. Al final, la consulta de objetos `INVALID` debe salir **sin filas**.

Para comprobarlo:
```sql
SELECT id, nombre, rol FROM usuario;   -- deben aparecer admin, 222 y 504
```

### 3. Abrir el proyecto

Abra en IntelliJ **la carpeta raíz** del repositorio y, si no lo hace solo, agregue como proyecto Maven el **`pom.xml` de la raíz** (clic derecho → *Add as Maven Project*). En el panel Maven deben aparecer `sistema-reservas` con los módulos `protocol`, `backend` y `frontend`.

### 4. Arrancar el Backend

1. Abra `backend/src/main/java/una/eif206/ServerMain.java` y ejecútelo una vez para que IntelliJ cree la configuración.
2. En **Run → Edit Configurations… → ServerMain → Environment variables**, agregue:

   | Variable | Valor |
   |---|---|
   | `DB_URL` | `jdbc:oracle:thin:@localhost:1521:XE` |
   | `DB_USER` | `reservas_app` |
   | `DB_PASSWORD` | la clave del paso 1 |

3. Ejecute `ServerMain` de nuevo. La consola debe mostrar:
   ```
   [ServerMain] backend escuchando en el puerto 7070. Ctrl+C para detener.
   ```

Las credenciales van en variables de entorno a propósito: así nunca quedan escritas en el código ni se suben al repositorio.

**IA (opcional):** cree el archivo `backend/src/main/resources/config.properties` con la línea `groq.api.key=SU_API_KEY` y reinicie `ServerMain`. Ese archivo está en el `.gitignore` y **nunca debe subirse**.

### 5. Arrancar el Frontend

Con el Backend corriendo, ejecute `frontend/src/main/java/una/eif206/ApplicationLogin.java` e inicie sesión con alguno de los [usuarios de prueba](#usuarios-de-prueba).

El Frontend lee su configuración de `frontend/src/main/resources/servidor.properties`:

```properties
host=localhost   # IP de la computadora donde corre el Backend
puerto=7070
modo=servidor    # "prueba" = usa datos en memoria, sin Backend
```

Con `modo=prueba` la aplicación funciona sin servidor ni base de datos. Es útil para revisar las pantallas rápidamente, aunque en ese modo no hay alertas.

**Para ver las alertas** hacen falta dos ventanas. En *Edit Configurations → ApplicationLogin → Modify options*, active **Allow multiple instances** y ejecute `ApplicationLogin` dos veces. Entre con `222` en una y con `504` en la otra. Al crear una reserva en una, la alerta aparece en la otra.

---

## Usuarios de prueba

El script de la base de datos crea estos usuarios:

| Usuario | Clave | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `222` | `222` | Funcionario (Maria Lopez) |
| `504` | `504` | Funcionario (alain) |

También crea 3 categorías (*Laptop windows*, *Sala de Juntas*, *Sala para 10 personas*) y 3 recursos para empezar a reservar.

---

## Usar dos computadoras

El sistema está pensado para que el Backend corra en una máquina y los Frontend en otras de la misma red:

1. En la computadora del **Backend**, averigüe su IP (`ipconfig` en Windows) y permita conexiones entrantes al **puerto 7070** en el Firewall de Windows.
2. En cada computadora con **Frontend**, cambie `host=localhost` por esa IP en `servidor.properties`, por ejemplo `host=192.168.1.15`.
3. Arranque primero el Backend y después los Frontend.

---

## Pruebas

Las pruebas usan **JUnit 5** y están separadas como pide el enunciado:

- **De unidad** (clases que terminan en `Test`, las ejecuta **Surefire**): prueban piezas sueltas sin red. Por ejemplo, los controllers del Frontend contra `FakeService`, o los servicios del Backend contra un repositorio en memoria.
- **De integración** (clases que terminan en `IT`, las ejecuta **Failsafe**): prueban varias piezas juntas con **sockets reales**. Por ejemplo, el Proxy contra un servidor, o dos clientes donde uno crea una reserva y se comprueba que al otro le llega la alerta y al creador no.

Para ejecutar todo, desde la raíz:

```bash
mvn clean verify
```

En IntelliJ: panel Maven → `sistema-reservas` → Lifecycle → doble clic en **`verify`**. Debe terminar en **BUILD SUCCESS**.

Ninguna prueba necesita que el Backend esté encendido. Las únicas que usan Oracle real (`OracleRepositorioIT`) **se saltan solas** si no están configuradas las variables de la base de datos, por eso Maven puede mostrar algunas como *Skipped*.

---

## Estructura del repositorio

```
proyecto-2-programacion-3-/
├── pom.xml
├── protocol/src/main/java/una/eif206/
│   ├── model/           Entidades Serializable (Usuario, Funcionario, Reserva, Recurso…)
│   └── protocol/        IService (operaciones) y Protocol (puerto y códigos)
├── backend/
│   ├── src/main/java/una/eif206/
│   │   ├── ServerMain.java   Arranque del servidor
│   │   ├── net/              Server (acepta clientes) y Worker (un hilo por cliente)
│   │   ├── service/          Lógica de negocio y validaciones
│   │   └── data/             Acceso a Oracle con procedimientos almacenados
│   ├── src/main/resources/db/  Scripts SQL (script_reservas_oracle11g.sql)
│   └── docs/                 Especificación de la BD y reporte del Backend
└── frontend/
    ├── src/main/java/una/eif206/
    │   ├── ApplicationLogin.java  Ventana principal y arranque del cliente
    │   ├── client/   Service (Proxy), SocketListener, ThreadListener, FakeService
    │   ├── controller/  Un controller por pantalla + AlertaController
    │   ├── view/        Pantallas Swing + AlertaPanel
    │   └── util/        PDF, íconos, sesión, configuración
    ├── src/main/resources/   servidor.properties e íconos
    └── src/test/             Pruebas de unidad e integración del cliente
```

---

## Problemas comunes

| Lo que pasa | Por qué | Qué hacer |
|---|---|---|
| *"No se pudo conectar al servidor localhost:7070"* al abrir el Frontend | El Backend no está corriendo, o el host o el puerto no coinciden | Arranque `ServerMain` primero y revise `servidor.properties` |
| *"Error de base de datos"* al iniciar sesión, y en la consola del Backend `PLS-00201: identifier 'LOGINUSUARIO' must be declared` | El script SQL se ejecutó con `SYSTEM` en lugar de `reservas_app` | Vuelva a ejecutar el script con la conexión `reservas_app` seleccionada |
| *"La base de datos no se encuentra disponible"* o `ORA-01017` | Faltan las variables de entorno o la clave no coincide | Revise `DB_URL`, `DB_USER` y `DB_PASSWORD` en la configuración de `ServerMain` y reinícielo |
| `ORA-12505` o problemas con el *listener* | La instancia de Oracle usa *service name* en lugar de SID | Pruebe `DB_URL=jdbc:oracle:thin:@//localhost:1521/XE` |
| *"No se pudo procesar la frase con IA"* | No hay una API key de Groq válida en el Backend | Cree `backend/src/main/resources/config.properties` con `groq.api.key` |
| *"Unresolved dependency: protocol"* en IntelliJ | Maven todavía no compiló el módulo `protocol` | Ejecute `install` del proyecto padre (`sistema-reservas`) o importe el `pom.xml` de la raíz |
| *"Address already in use"* al arrancar `ServerMain` | Ya hay otro `ServerMain` corriendo | Deténgalo antes de volver a ejecutarlo |

---

## Más documentación

- [`backend/README.md`](backend/README.md): detalle del Backend, variables de entorno, pruebas y decisiones de diseño.
- [`backend/docs/BD-ESPECIFICACION.md`](backend/docs/BD-ESPECIFICACION.md): tablas, secuencias y procedimientos de la base de datos.
- [`docs/BACKEND-P2.md`](docs/BACKEND-P2.md): diseño de red y patrón de acceso a datos.
