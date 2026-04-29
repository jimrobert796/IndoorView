package com.example.indoorview;

import java.util.Base64;

public class Utilidades {
    static String url_consulta = "http://192.168.1.8:5984/indoor_view/_design/lugar_espacio/_view/lugar_espacio";
    static String url_mantenimiento = "http://192.168.1.8:5984/indoor_view"; // CRUD Insertar, Actualizar, Borrar y Buscar

    static String user = "jimbo";
    static String passwd = "070906";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }
}
