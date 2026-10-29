--------------------------------------------------------------------------------
-- Sistema de Reservas - Script de base de datos
-- Oracle Database Express Edition 11g Release 2 (11.2.0)
--
-- Ejecucion:
--   SQL Developer : abrir este archivo con el usuario de la aplicacion
--                    (NUNCA con SYSTEM, ver db/00_crear_esquema_ejemplo.sql)
--                    conectado y presionar F5 (Run Script).
--   SQL*Plus      : sqlplus usuario_app/clave@XE
--                    SQL> @script_reservas_oracle11g.sql
--
-- Re-ejecutable: la seccion 1 borra todo lo que crea la seccion 2 en adelante,
-- por lo que el script puede correrse varias veces sobre el mismo esquema.
--------------------------------------------------------------------------------

SET DEFINE OFF;
SET SERVEROUTPUT ON;

--------------------------------------------------------------------------------
-- 1. LIMPIEZA RE-EJECUTABLE
--    Cada DROP va en su propio bloque PL/SQL: si el objeto no existe, Oracle
--    lanza ORA-04043/ORA-02289/ORA-00942 y lo ignoramos con WHEN OTHERS NULL.
--------------------------------------------------------------------------------

-- 1.1 Procedimientos y funciones (orden: cualquiera, no tienen FK entre si)
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION estadisticaActividadesSemana'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION estadisticaRecursosCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarRecursosOcupados'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarRecursoPorReserva'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarReservaPorRango'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarReservaPorFecha'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarReservaPorFuncionario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarReserva'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION buscarReserva'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE cancelarReserva'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE insertarReservaRecurso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE insertarReserva'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION siguienteIdReserva'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarRecursoPorCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarRecurso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION buscarRecurso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE eliminarRecurso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE modificarRecurso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE insertarRecurso'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarCategoriaTexto'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION buscarCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE eliminarCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE modificarCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE insertarCategoria'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarFuncionarioTexto'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarFuncionario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION buscarFuncionario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE eliminarFuncionario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE modificarFuncionario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE insertarFuncionario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE cambiarClave'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION loginUsuario'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION formatearId'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 1.2 Paquete de tipos
BEGIN EXECUTE IMMEDIATE 'DROP PACKAGE types'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 1.3 Tablas (CASCADE CONSTRAINTS por si quedaron FKs cruzadas de una version anterior)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RESERVA_RECURSO CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RESERVA CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RECURSO CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CATEGORIA_RECURSO CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE USUARIO CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- 1.4 Secuencias
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CATEGORIA'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_RESERVA'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

--------------------------------------------------------------------------------
-- 2. SECUENCIAS Y GENERADOR DE IDS (CAT-000001 / RES-000001)
--------------------------------------------------------------------------------

-- Arranca en 4 porque la semilla (seccion 7) inserta CAT-000001..CAT-000003
-- con id literal (no via secuencia); el primer insertarCategoria() real debe
-- generar CAT-000004.
CREATE SEQUENCE SEQ_CATEGORIA START WITH 4 INCREMENT BY 1 NOCACHE;
/
CREATE SEQUENCE SEQ_RESERVA START WITH 1 INCREMENT BY 1 NOCACHE;
/

CREATE OR REPLACE FUNCTION formatearId (
    p_prefijo IN VARCHAR2,
    p_numero  IN NUMBER
) RETURN VARCHAR2 AS
BEGIN
    RETURN p_prefijo || '-' || LPAD(TO_CHAR(p_numero), 6, '0');
END formatearId;
/

--------------------------------------------------------------------------------
-- 3. TABLAS (PK, FK, UNIQUE, CHECK nombrados)
--------------------------------------------------------------------------------

-- 3.1 USUARIO (Persona + Usuario + Admin + Funcionario, discriminado por ROL)
CREATE TABLE USUARIO (
    ID            VARCHAR2(20)  NOT NULL,
    NOMBRE        VARCHAR2(150) NOT NULL,
    CLAVE         VARCHAR2(100) NOT NULL,
    ROL           VARCHAR2(20)  NOT NULL,
    DEPARTAMENTO  VARCHAR2(100),
    TELEFONO      VARCHAR2(20),
    CONSTRAINT pk_usuario PRIMARY KEY (ID),
    CONSTRAINT ck_usuario_rol CHECK (ROL IN ('ADMIN', 'FUNCIONARIO')),
    CONSTRAINT ck_usuario_rol_datos CHECK (
        (ROL = 'FUNCIONARIO') OR (DEPARTAMENTO IS NULL AND TELEFONO IS NULL)
    )
);
/

-- 3.2 CATEGORIA_RECURSO
CREATE TABLE CATEGORIA_RECURSO (
    ID          VARCHAR2(12)  NOT NULL,
    DESCRIPCION VARCHAR2(200) NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (ID)
);
/

-- 3.3 RECURSO
CREATE TABLE RECURSO (
    ID           VARCHAR2(20)  NOT NULL,
    DESCRIPCION  VARCHAR2(200) NOT NULL,
    CATEGORIA_ID VARCHAR2(12)  NOT NULL,
    DISPONIBLE   NUMBER(1)     DEFAULT 1 NOT NULL,
    CONSTRAINT pk_recurso PRIMARY KEY (ID),
    CONSTRAINT fk_recurso_categoria FOREIGN KEY (CATEGORIA_ID)
        REFERENCES CATEGORIA_RECURSO (ID),
    CONSTRAINT ck_recurso_disponible CHECK (DISPONIBLE IN (0, 1))
);
/

-- 3.4 RESERVA
CREATE TABLE RESERVA (
    ID             VARCHAR2(12)  NOT NULL,
    ACTIVIDAD      VARCHAR2(200) NOT NULL,
    FECHA          DATE          NOT NULL,
    HORA_INICIO    VARCHAR2(5)   NOT NULL,
    HORA_FIN       VARCHAR2(5)   NOT NULL,
    FUNCIONARIO_ID VARCHAR2(20)  NOT NULL,
    ESTADO         VARCHAR2(12)  DEFAULT 'ACTIVA' NOT NULL,
    CONSTRAINT pk_reserva PRIMARY KEY (ID),
    CONSTRAINT fk_reserva_funcionario FOREIGN KEY (FUNCIONARIO_ID)
        REFERENCES USUARIO (ID),
    CONSTRAINT ck_reserva_estado CHECK (ESTADO IN ('ACTIVA', 'CANCELADA', 'COMPLETADA')),
    CONSTRAINT ck_reserva_hora_inicio CHECK (
        REGEXP_LIKE(HORA_INICIO, '^([01][0-9]|2[0-3]):[0-5][0-9]$')
    ),
    CONSTRAINT ck_reserva_hora_fin CHECK (
        REGEXP_LIKE(HORA_FIN, '^([01][0-9]|2[0-3]):[0-5][0-9]$')
    ),
    CONSTRAINT ck_reserva_horario CHECK (HORA_INICIO < HORA_FIN)
);
/

-- 3.5 RESERVA_RECURSO (N:M)
CREATE TABLE RESERVA_RECURSO (
    RESERVA_ID VARCHAR2(12) NOT NULL,
    RECURSO_ID VARCHAR2(20) NOT NULL,
    CONSTRAINT pk_reserva_recurso PRIMARY KEY (RESERVA_ID, RECURSO_ID),
    CONSTRAINT fk_resre_reserva FOREIGN KEY (RESERVA_ID) REFERENCES RESERVA (ID),
    CONSTRAINT fk_resre_recurso FOREIGN KEY (RECURSO_ID) REFERENCES RECURSO (ID)
);
/

--------------------------------------------------------------------------------
-- 4. PAQUETE DE TIPOS (ref_cursor)
--------------------------------------------------------------------------------

CREATE OR REPLACE PACKAGE types AS
    TYPE ref_cursor IS REF CURSOR;
END types;
/

--------------------------------------------------------------------------------
-- 5. CRUD POR ENTIDAD
--------------------------------------------------------------------------------

-- 5.1 Usuario / Funcionario ----------------------------------------------------

CREATE OR REPLACE PROCEDURE insertarFuncionario (
    p_id           IN USUARIO.ID%TYPE,
    p_nombre       IN USUARIO.NOMBRE%TYPE,
    p_clave        IN USUARIO.CLAVE%TYPE,
    p_departamento IN USUARIO.DEPARTAMENTO%TYPE,
    p_telefono     IN USUARIO.TELEFONO%TYPE
) AS
BEGIN
    INSERT INTO USUARIO (ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO)
    VALUES (p_id, p_nombre, p_clave, 'FUNCIONARIO', p_departamento, p_telefono);
END insertarFuncionario;
/

CREATE OR REPLACE PROCEDURE modificarFuncionario (
    p_id           IN USUARIO.ID%TYPE,
    p_nombre       IN USUARIO.NOMBRE%TYPE,
    p_departamento IN USUARIO.DEPARTAMENTO%TYPE,
    p_telefono     IN USUARIO.TELEFONO%TYPE
) AS
BEGIN
    UPDATE USUARIO
       SET NOMBRE = p_nombre,
           DEPARTAMENTO = p_departamento,
           TELEFONO = p_telefono
     WHERE ID = p_id
       AND ROL = 'FUNCIONARIO';
END modificarFuncionario;
/

CREATE OR REPLACE PROCEDURE eliminarFuncionario (
    p_id IN USUARIO.ID%TYPE
) AS
BEGIN
    DELETE FROM USUARIO WHERE ID = p_id AND ROL = 'FUNCIONARIO';
END eliminarFuncionario;
/

CREATE OR REPLACE FUNCTION buscarFuncionario (
    p_id IN USUARIO.ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO
          FROM USUARIO
         WHERE ID = p_id
           AND ROL = 'FUNCIONARIO';
    RETURN v_cursor;
END buscarFuncionario;
/

CREATE OR REPLACE FUNCTION listarFuncionario RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO
          FROM USUARIO
         WHERE ROL = 'FUNCIONARIO'
         ORDER BY NOMBRE;
    RETURN v_cursor;
END listarFuncionario;
/

CREATE OR REPLACE FUNCTION listarFuncionarioTexto (
    p_texto IN VARCHAR2
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO
          FROM USUARIO
         WHERE ROL = 'FUNCIONARIO'
           AND (UPPER(ID) LIKE UPPER('%' || p_texto || '%')
                OR UPPER(NOMBRE) LIKE UPPER('%' || p_texto || '%'))
         ORDER BY NOMBRE;
    RETURN v_cursor;
END listarFuncionarioTexto;
/

CREATE OR REPLACE FUNCTION loginUsuario (
    p_id    IN USUARIO.ID%TYPE,
    p_clave IN USUARIO.CLAVE%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO
          FROM USUARIO
         WHERE ID = p_id
           AND CLAVE = p_clave;
    RETURN v_cursor;
END loginUsuario;
/

CREATE OR REPLACE PROCEDURE cambiarClave (
    p_id          IN USUARIO.ID%TYPE,
    p_clave_nueva IN USUARIO.CLAVE%TYPE
) AS
BEGIN
    UPDATE USUARIO SET CLAVE = p_clave_nueva WHERE ID = p_id;
END cambiarClave;
/

-- 5.2 Categoria -----------------------------------------------------------------

CREATE OR REPLACE PROCEDURE insertarCategoria (
    p_descripcion IN CATEGORIA_RECURSO.DESCRIPCION%TYPE,
    p_id          OUT CATEGORIA_RECURSO.ID%TYPE
) AS
BEGIN
    p_id := formatearId('CAT', SEQ_CATEGORIA.NEXTVAL);
    INSERT INTO CATEGORIA_RECURSO (ID, DESCRIPCION) VALUES (p_id, p_descripcion);
END insertarCategoria;
/

CREATE OR REPLACE PROCEDURE modificarCategoria (
    p_id          IN CATEGORIA_RECURSO.ID%TYPE,
    p_descripcion IN CATEGORIA_RECURSO.DESCRIPCION%TYPE
) AS
BEGIN
    UPDATE CATEGORIA_RECURSO SET DESCRIPCION = p_descripcion WHERE ID = p_id;
END modificarCategoria;
/

CREATE OR REPLACE PROCEDURE eliminarCategoria (
    p_id IN CATEGORIA_RECURSO.ID%TYPE
) AS
BEGIN
    DELETE FROM CATEGORIA_RECURSO WHERE ID = p_id;
END eliminarCategoria;
/

CREATE OR REPLACE FUNCTION buscarCategoria (
    p_id IN CATEGORIA_RECURSO.ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, DESCRIPCION FROM CATEGORIA_RECURSO WHERE ID = p_id;
    RETURN v_cursor;
END buscarCategoria;
/

CREATE OR REPLACE FUNCTION listarCategoria RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, DESCRIPCION FROM CATEGORIA_RECURSO ORDER BY DESCRIPCION;
    RETURN v_cursor;
END listarCategoria;
/

CREATE OR REPLACE FUNCTION listarCategoriaTexto (
    p_texto IN VARCHAR2
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, DESCRIPCION
          FROM CATEGORIA_RECURSO
         WHERE UPPER(DESCRIPCION) LIKE UPPER('%' || p_texto || '%')
         ORDER BY DESCRIPCION;
    RETURN v_cursor;
END listarCategoriaTexto;
/

-- 5.3 Recurso ---------------------------------------------------------------

CREATE OR REPLACE PROCEDURE insertarRecurso (
    p_id          IN RECURSO.ID%TYPE,
    p_descripcion IN RECURSO.DESCRIPCION%TYPE,
    p_categoria_id IN RECURSO.CATEGORIA_ID%TYPE
) AS
BEGIN
    INSERT INTO RECURSO (ID, DESCRIPCION, CATEGORIA_ID, DISPONIBLE)
    VALUES (p_id, p_descripcion, p_categoria_id, 1);
END insertarRecurso;
/

CREATE OR REPLACE PROCEDURE modificarRecurso (
    p_id          IN RECURSO.ID%TYPE,
    p_descripcion IN RECURSO.DESCRIPCION%TYPE,
    p_categoria_id IN RECURSO.CATEGORIA_ID%TYPE
) AS
BEGIN
    UPDATE RECURSO
       SET DESCRIPCION = p_descripcion,
           CATEGORIA_ID = p_categoria_id
     WHERE ID = p_id;
END modificarRecurso;
/

CREATE OR REPLACE PROCEDURE eliminarRecurso (
    p_id IN RECURSO.ID%TYPE
) AS
BEGIN
    DELETE FROM RECURSO WHERE ID = p_id;
END eliminarRecurso;
/

CREATE OR REPLACE FUNCTION buscarRecurso (
    p_id IN RECURSO.ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT R.ID, R.DESCRIPCION, R.CATEGORIA_ID, R.DISPONIBLE,
               C.DESCRIPCION AS CATEGORIA_DESCRIPCION
          FROM RECURSO R
          JOIN CATEGORIA_RECURSO C ON C.ID = R.CATEGORIA_ID
         WHERE R.ID = p_id;
    RETURN v_cursor;
END buscarRecurso;
/

CREATE OR REPLACE FUNCTION listarRecurso RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT R.ID, R.DESCRIPCION, R.CATEGORIA_ID, R.DISPONIBLE,
               C.DESCRIPCION AS CATEGORIA_DESCRIPCION
          FROM RECURSO R
          JOIN CATEGORIA_RECURSO C ON C.ID = R.CATEGORIA_ID
         ORDER BY R.ID;
    RETURN v_cursor;
END listarRecurso;
/

CREATE OR REPLACE FUNCTION listarRecursoPorCategoria (
    p_categoria_id IN RECURSO.CATEGORIA_ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT R.ID, R.DESCRIPCION, R.CATEGORIA_ID, R.DISPONIBLE,
               C.DESCRIPCION AS CATEGORIA_DESCRIPCION
          FROM RECURSO R
          JOIN CATEGORIA_RECURSO C ON C.ID = R.CATEGORIA_ID
         WHERE R.CATEGORIA_ID = p_categoria_id
         ORDER BY R.ID;
    RETURN v_cursor;
END listarRecursoPorCategoria;
/

-- 5.4 Reserva / Reserva_Recurso ------------------------------------------------

CREATE OR REPLACE FUNCTION siguienteIdReserva RETURN VARCHAR2 AS
BEGIN
    RETURN formatearId('RES', SEQ_RESERVA.NEXTVAL);
END siguienteIdReserva;
/

CREATE OR REPLACE PROCEDURE insertarReserva (
    p_id             IN RESERVA.ID%TYPE,
    p_actividad      IN RESERVA.ACTIVIDAD%TYPE,
    p_fecha          IN RESERVA.FECHA%TYPE,
    p_hora_inicio    IN RESERVA.HORA_INICIO%TYPE,
    p_hora_fin       IN RESERVA.HORA_FIN%TYPE,
    p_funcionario_id IN RESERVA.FUNCIONARIO_ID%TYPE
) AS
BEGIN
    INSERT INTO RESERVA (ID, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN, FUNCIONARIO_ID, ESTADO)
    VALUES (p_id, p_actividad, p_fecha, p_hora_inicio, p_hora_fin, p_funcionario_id, 'ACTIVA');
END insertarReserva;
/

CREATE OR REPLACE PROCEDURE insertarReservaRecurso (
    p_reserva_id IN RESERVA_RECURSO.RESERVA_ID%TYPE,
    p_recurso_id IN RESERVA_RECURSO.RECURSO_ID%TYPE
) AS
BEGIN
    INSERT INTO RESERVA_RECURSO (RESERVA_ID, RECURSO_ID) VALUES (p_reserva_id, p_recurso_id);
END insertarReservaRecurso;
/

CREATE OR REPLACE PROCEDURE cancelarReserva (
    p_id IN RESERVA.ID%TYPE
) AS
BEGIN
    UPDATE RESERVA SET ESTADO = 'CANCELADA' WHERE ID = p_id;
END cancelarReserva;
/

CREATE OR REPLACE FUNCTION buscarReserva (
    p_id IN RESERVA.ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN, FUNCIONARIO_ID, ESTADO
          FROM RESERVA
         WHERE ID = p_id;
    RETURN v_cursor;
END buscarReserva;
/

CREATE OR REPLACE FUNCTION listarReserva RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN, FUNCIONARIO_ID, ESTADO
          FROM RESERVA
         ORDER BY FECHA, HORA_INICIO;
    RETURN v_cursor;
END listarReserva;
/

CREATE OR REPLACE FUNCTION listarReservaPorFuncionario (
    p_funcionario_id IN RESERVA.FUNCIONARIO_ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN, FUNCIONARIO_ID, ESTADO
          FROM RESERVA
         WHERE FUNCIONARIO_ID = p_funcionario_id
         ORDER BY FECHA, HORA_INICIO;
    RETURN v_cursor;
END listarReservaPorFuncionario;
/

CREATE OR REPLACE FUNCTION listarReservaPorFecha (
    p_fecha IN RESERVA.FECHA%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN, FUNCIONARIO_ID, ESTADO
          FROM RESERVA
         WHERE FECHA = p_fecha
         ORDER BY HORA_INICIO;
    RETURN v_cursor;
END listarReservaPorFecha;
/

CREATE OR REPLACE FUNCTION listarReservaPorRango (
    p_desde IN DATE,
    p_hasta IN DATE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, ACTIVIDAD, FECHA, HORA_INICIO, HORA_FIN, FUNCIONARIO_ID, ESTADO
          FROM RESERVA
         WHERE FECHA BETWEEN p_desde AND p_hasta
           AND ESTADO <> 'CANCELADA'
         ORDER BY FECHA, HORA_INICIO;
    RETURN v_cursor;
END listarReservaPorRango;
/

CREATE OR REPLACE FUNCTION listarRecursoPorReserva (
    p_reserva_id IN RESERVA_RECURSO.RESERVA_ID%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT R.ID, R.DESCRIPCION, R.CATEGORIA_ID, R.DISPONIBLE,
               C.DESCRIPCION AS CATEGORIA_DESCRIPCION
          FROM RESERVA_RECURSO RR
          JOIN RECURSO R ON R.ID = RR.RECURSO_ID
          JOIN CATEGORIA_RECURSO C ON C.ID = R.CATEGORIA_ID
         WHERE RR.RESERVA_ID = p_reserva_id
         ORDER BY R.ID;
    RETURN v_cursor;
END listarRecursoPorReserva;
/

-- Traslape: p_hora_inicio < HORA_FIN existente AND p_hora_fin > HORA_INICIO existente.
-- Rangos contiguos (termina 10:00 / empieza 10:00) NO se traslapan, porque la
-- comparacion es estricta (<, >), no <=, >=.
CREATE OR REPLACE FUNCTION listarRecursosOcupados (
    p_fecha       IN RESERVA.FECHA%TYPE,
    p_hora_inicio IN RESERVA.HORA_INICIO%TYPE,
    p_hora_fin    IN RESERVA.HORA_FIN%TYPE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT DISTINCT RR.RECURSO_ID
          FROM RESERVA R
          JOIN RESERVA_RECURSO RR ON RR.RESERVA_ID = R.ID
         WHERE R.FECHA = p_fecha
           AND R.ESTADO = 'ACTIVA'
           AND p_hora_inicio < R.HORA_FIN
           AND p_hora_fin > R.HORA_INICIO;
    RETURN v_cursor;
END listarRecursosOcupados;
/

--------------------------------------------------------------------------------
-- 6. ESTADISTICAS
--------------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION estadisticaRecursosCategoria (
    p_desde IN DATE,
    p_hasta IN DATE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT C.DESCRIPCION AS CATEGORIA_DESCRIPCION,
               COUNT(*)      AS CANTIDAD
          FROM RESERVA R
          JOIN RESERVA_RECURSO RR ON RR.RESERVA_ID = R.ID
          JOIN RECURSO REC ON REC.ID = RR.RECURSO_ID
          JOIN CATEGORIA_RECURSO C ON C.ID = REC.CATEGORIA_ID
         WHERE R.ESTADO <> 'CANCELADA'
           AND R.FECHA BETWEEN p_desde AND p_hasta
         GROUP BY C.DESCRIPCION
         ORDER BY C.DESCRIPCION;
    RETURN v_cursor;
END estadisticaRecursosCategoria;
/

-- Semana ISO (empieza lunes), igual que DayOfWeek.MONDAY en el P1.
CREATE OR REPLACE FUNCTION estadisticaActividadesSemana (
    p_desde IN DATE,
    p_hasta IN DATE
) RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT TRUNC(R.FECHA, 'IW') AS SEMANA_INICIO,
               COUNT(*)             AS CANTIDAD
          FROM RESERVA R
         WHERE R.ESTADO <> 'CANCELADA'
           AND R.FECHA BETWEEN p_desde AND p_hasta
         GROUP BY TRUNC(R.FECHA, 'IW')
         ORDER BY SEMANA_INICIO;
    RETURN v_cursor;
END estadisticaActividadesSemana;
/

--------------------------------------------------------------------------------
-- 7. DATOS SEMILLA (desde data.xml) - unico COMMIT del script
--
-- Tomado de data.xml: 1 admin y los funcionarios que SI tienen rol.
-- Se OMITEN "504670161" y "50467" (no tienen <rol> en el XML original).
-- Clave inicial de cada funcionario = su propio id, igual que el P1.
-- Tambien se cargan las categorias/recursos de catalogo (coinciden con el
-- Data() del P1 cuando no hay data.xml) para que la aplicacion tenga algo
-- que mostrar; NO se migran las reservas transaccionales del XML.
--------------------------------------------------------------------------------

INSERT INTO USUARIO (ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO)
VALUES ('admin', 'admin', 'admin123', 'ADMIN', NULL, NULL);

INSERT INTO USUARIO (ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO)
VALUES ('222', 'Maria Lopez', '222', 'FUNCIONARIO', 'RRHH', '4444');

INSERT INTO USUARIO (ID, NOMBRE, CLAVE, ROL, DEPARTAMENTO, TELEFONO)
VALUES ('504', 'alain', '504', 'FUNCIONARIO', NULL, '8484');

INSERT INTO CATEGORIA_RECURSO (ID, DESCRIPCION) VALUES ('CAT-000001', 'Laptop windows');
INSERT INTO CATEGORIA_RECURSO (ID, DESCRIPCION) VALUES ('CAT-000002', 'Sala de Juntas');
INSERT INTO CATEGORIA_RECURSO (ID, DESCRIPCION) VALUES ('CAT-000003', 'Sala para 10 personas');

INSERT INTO RECURSO (ID, DESCRIPCION, CATEGORIA_ID, DISPONIBLE)
VALUES ('238715', 'Laptop #238715', 'CAT-000001', 1);
INSERT INTO RECURSO (ID, DESCRIPCION, CATEGORIA_ID, DISPONIBLE)
VALUES ('45238', 'Laptop #45238', 'CAT-000001', 1);
INSERT INTO RECURSO (ID, DESCRIPCION, CATEGORIA_ID, DISPONIBLE)
VALUES ('34343', 'Sala 1 primer piso', 'CAT-000002', 1);

COMMIT;

--------------------------------------------------------------------------------
-- 8. VERIFICACION FINAL
--------------------------------------------------------------------------------

SELECT object_name, object_type, status FROM user_objects WHERE status = 'INVALID';
SELECT name, type, line, position, text FROM user_errors ORDER BY name, sequence;
