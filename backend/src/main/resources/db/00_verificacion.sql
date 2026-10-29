--------------------------------------------------------------------------------
-- OPCIONAL - sanity checks manuales de conectividad, para correr en SQL
-- Developer conectado como el usuario de la aplicacion (NUNCA como SYSTEM).
-- Es el equivalente en SQL a lo que hace una.eif206.data.ConexionCheck en Java;
-- util cuando se quiere verificar la conexion sin levantar la aplicacion.
--
-- No crea ni borra nada: son solo SELECT.
--------------------------------------------------------------------------------

-- 1) Quien soy y a que base de datos estoy conectado.
SELECT USER AS usuario_conectado FROM DUAL;
SELECT * FROM v$version WHERE banner LIKE 'Oracle%';

-- 2) Objetos invalidos en mi esquema (debe dar 0 filas despues de correr
--    script_reservas_oracle11g.sql sin errores).
SELECT object_name, object_type, status
  FROM user_objects
 WHERE status = 'INVALID';

-- 3) Que existan los procedimientos/funciones esperados (lista no exhaustiva,
--    solo para detectar rapido si el script no corrio o corrio a medias).
SELECT object_name, object_type
  FROM user_objects
 WHERE object_type IN ('PROCEDURE', 'FUNCTION', 'PACKAGE')
 ORDER BY object_type, object_name;

-- 4) Conteo rapido de filas por tabla (confirma que la semilla cargo).
SELECT 'USUARIO' AS tabla, COUNT(*) AS filas FROM USUARIO
UNION ALL
SELECT 'CATEGORIA_RECURSO', COUNT(*) FROM CATEGORIA_RECURSO
UNION ALL
SELECT 'RECURSO', COUNT(*) FROM RECURSO
UNION ALL
SELECT 'RESERVA', COUNT(*) FROM RESERVA
UNION ALL
SELECT 'RESERVA_RECURSO', COUNT(*) FROM RESERVA_RECURSO;
