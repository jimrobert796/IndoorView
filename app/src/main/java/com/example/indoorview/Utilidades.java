package com.example.indoorview;

import java.util.Base64;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class Utilidades {

    /***
     * Clase encargada de utilidades minimas pero poderosas
     */


    static String url_consulta = "";
    static String url_mantenimiento = ""; // CRUD Insertar, Actualizar, Borrar y Buscar

    static String user = "jimbo";
    static String passwd = "070906";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }

    /*
    Metodos para Hasheo de contraseñas unicamente Con BCrypt -> con licencia APACHE
     */

    // Hashear contraseña (costo 10 = balance seguridad/rendimiento)
    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(10, password.toCharArray());
    }

    // Verificar contraseña
    public boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified;
    }


}
