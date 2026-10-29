--------------------------------------------------------------------------------
-- Prueba de ida y vuelta Java <-> Oracle: un procedimiento que inserta, una
-- funcion que devuelve un cursor, un procedimiento que borra. Pensado para
-- correrlo una vez y despues usar ConexionCheck (ver README-conexion.md,
-- seccion "Prueba de ida y vuelta").
--
-- Oracle Database Express Edition 11g Release 2 (11.2.0).
-- Ejecucion: SQL Developer, conectado como el usuario de la aplicacion
-- (NUNCA SYSTEM), F5 (Run Script). Re-ejecutable.
--------------------------------------------------------------------------------

SET DEFINE OFF;
SET SERVEROUTPUT ON;

--------------------------------------------------------------------------------
-- 1. LIMPIEZA RE-EJECUTABLE
--    Cada DROP en su propio bloque PL/SQL; si el objeto no existe, Oracle
--    lanza ORA-04043/ORA-02289/ORA-00942 y lo ignoramos con WHEN OTHERS NULL.
--------------------------------------------------------------------------------

BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE eliminarPrueba'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP FUNCTION listarPrueba'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP PROCEDURE insertarPrueba'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE PRUEBA_CONEXION CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_PRUEBA'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

--------------------------------------------------------------------------------
-- 2. TABLA Y SECUENCIA
--------------------------------------------------------------------------------

CREATE TABLE PRUEBA_CONEXION (
    ID     NUMBER       NOT NULL,
    TEXTO  VARCHAR2(50) NOT NULL,
    CREADO DATE         NOT NULL,
    CONSTRAINT pk_prueba_conexion PRIMARY KEY (ID)
);
/

CREATE SEQUENCE SEQ_PRUEBA START WITH 1 INCREMENT BY 1 NOCACHE;
/

--------------------------------------------------------------------------------
-- 3. PAQUETE DE TIPOS
--    Mismo nombre y mismo contenido que el del script principal
--    (script_reservas_oracle11g.sql): CREATE OR REPLACE no choca si ya
--    existe, porque es exactamente el mismo paquete.
--------------------------------------------------------------------------------

CREATE OR REPLACE PACKAGE types AS
    TYPE ref_cursor IS REF CURSOR;
END types;
/

--------------------------------------------------------------------------------
-- 4. PROCEDIMIENTOS Y FUNCION
--------------------------------------------------------------------------------

CREATE OR REPLACE PROCEDURE insertarPrueba (
    p_texto IN  PRUEBA_CONEXION.TEXTO%TYPE,
    p_id    OUT PRUEBA_CONEXION.ID%TYPE
) AS
BEGIN
    p_id := SEQ_PRUEBA.NEXTVAL;
    INSERT INTO PRUEBA_CONEXION (ID, TEXTO, CREADO)
    VALUES (p_id, p_texto, SYSDATE);
    COMMIT;
END insertarPrueba;
/

CREATE OR REPLACE FUNCTION listarPrueba RETURN types.ref_cursor AS
    v_cursor types.ref_cursor;
BEGIN
    OPEN v_cursor FOR
        SELECT ID, TEXTO, CREADO FROM PRUEBA_CONEXION ORDER BY ID;
    RETURN v_cursor;
END listarPrueba;
/

CREATE OR REPLACE PROCEDURE eliminarPrueba (
    p_id IN PRUEBA_CONEXION.ID%TYPE
) AS
BEGIN
    DELETE FROM PRUEBA_CONEXION WHERE ID = p_id;
    COMMIT;
END eliminarPrueba;
/

--------------------------------------------------------------------------------
-- 5. VERIFICACION FINAL
--------------------------------------------------------------------------------

SELECT object_name, object_type, status FROM user_objects WHERE status = 'INVALID';
SELECT name, type, line, position, text FROM user_errors ORDER BY name, sequence;
