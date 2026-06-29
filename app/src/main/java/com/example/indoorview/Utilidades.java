package com.example.indoorview;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class Utilidades {

    /***
     * Clase encargada de utilidades minimas pero poderosas
     */


    static String url_consulta = "";
    static String url_mantenimiento = ""; // CRUD Insertar, Actualizar, Borrar y Buscar

    // Solamente para el uso de CouchDB
    static String user = "";
    static String passwd = "";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }

    public static String generarIdEvento(String nombreEvento) {
        // Limpiar nombre (sin espacios, sin caracteres especiales)
        String nombreLimpio = nombreEvento
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        // Limitar longitud del nombre (evitar IDs demasiado largos)
        if (nombreLimpio.length() > 30) {
            nombreLimpio = nombreLimpio.substring(0, 30);
        }

        // Timestamp con milisegundos para mayor unicidad
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());

        return timestamp + "_" + nombreLimpio;
        // Ejemplo: "20260516_143022_123_concierto_universitario"
    }


    /*
    Metodos para Hasheo de contraseñas unicamente Con BCrypt -> con licencia APACHE
     */

    // Hashear contraseña (costo 10 = balance seguridad/rendimiento)
    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(10, password.toCharArray());
    }

    // Verificar contraseña
    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified;
    }


}
