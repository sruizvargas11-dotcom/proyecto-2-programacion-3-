package una.eif206.util;

import una.eif206.model.Usuario;

public class Sesion {

    private static Usuario usuario;

    public static Usuario getUsuario() { return usuario; }
    public static void setUsuario(Usuario u) { usuario = u; }
    public static boolean isLoggedIn() { return usuario != null; }
    public static void logout() { usuario = null; }
}
