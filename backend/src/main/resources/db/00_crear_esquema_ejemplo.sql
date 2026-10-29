--------------------------------------------------------------------------------
-- OPCIONAL - solo para quien administra la instancia Oracle XE 11.2 (el DBA).
-- Crea el esquema/usuario de la aplicacion. Se ejecuta UNA vez, conectado
-- como SYS o SYSTEM (ejemplo: sqlplus sys/clave@XE AS SYSDBA).
--
-- IMPORTANTE: la aplicacion NUNCA debe correr con el usuario SYSTEM. Crear un
-- usuario propio (ej. RESERVAS_APP) con solo los privilegios que necesita.
--
-- La contraseña se pide por variable de sustitucion (&password) para no
-- dejar NINGUNA contraseña real en el repositorio. Al ejecutar este script,
-- SQL Developer / SQL*Plus van a preguntarla por consola.
--------------------------------------------------------------------------------

SET DEFINE ON;

CREATE USER reservas_app IDENTIFIED BY &password
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;

GRANT CONNECT, RESOURCE TO reservas_app;

-- CONNECT  : permite iniciar sesion.
-- RESOURCE : permite crear tablas, secuencias, procedimientos, funciones,
--            paquetes y triggers dentro de su propio esquema.
--
-- A proposito NO se otorga DBA ni privilegios sobre otros esquemas.
-- Despues de correr este script, conectarse como reservas_app (NO como
-- SYSTEM/SYS) y ejecutar script_reservas_oracle11g.sql.

SET DEFINE OFF;
