package com.example.indoorview;

import java.util.Base64;

public class Utilidades {
    static String url_consulta = "";
    static String url_mantenimiento = ""; // CRUD Insertar, Actualizar, Borrar y Buscar

    static String user = "";
    static String passwd = "";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }
}
