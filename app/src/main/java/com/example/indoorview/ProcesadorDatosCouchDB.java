package com.example.indoorview;

import android.util.Log;
import android.widget.Toast;
import android.content.Context;

import com.example.indoorview.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PROCESADOR DE DATOS DE COUCHDB
 * Convierte respuesta JSON → SQLite Local
 * ═══════════════════════════════════════════════════════════════════════
 */
public class ProcesadorDatosCouchDB {

    private static final String TAG = "COUCHDB_PARSER";
    private Gson gson;
    private Database db;
    private Context context;

    public ProcesadorDatosCouchDB(Database db, Context context) {
        this.db = db;
        this.context = context;
        // Crear Gson con formateo bonito para debugging
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * PASO 1: Parsear JSON a objetos Java
     * @param jsonResponse JSON crudo del servidor
     * @return CouchDBResponse parseado
     */
    public CouchDBResponse parsearRespuesta(String jsonResponse) {
        try {
            Log.d(TAG, "════════════════════════════════════════════");
            Log.d(TAG, "PARSEANDO RESPUESTA DE COUCHDB");
            Log.d(TAG, "════════════════════════════════════════════");

            CouchDBResponse response = gson.fromJson(jsonResponse, CouchDBResponse.class);

            Log.d(TAG, "✓ Parseado correctamente");
            Log.d(TAG, "Total filas: " + response.total_rows);
            Log.d(TAG, "Offset: " + response.offset);
            Log.d(TAG, "Rows recibidas: " + response.rows.size());

            return response;

        } catch (Exception e) {
            Log.e(TAG, "✗ Error parseando JSON: " + e.getMessage());
            mostrarToast("Error parseando datos: " + e.getMessage());
            return null;
        }
    }

    /**
     * PASO 2: Procesar una respuesta completa
     * Inserta todos los lugares, pisos, espacios y geometrías
     * @param response Respuesta parseada
     * @return true si fue exitoso
     */
    public boolean procesarRespuestaCompleta(CouchDBResponse response) {
        if (response == null || response.rows == null || response.rows.isEmpty()) {
            Log.e(TAG, "Respuesta vacía o nula");
            return false;
        }

        Log.d(TAG, "════════════════════════════════════════════");
        Log.d(TAG, "PROCESANDO RESPUESTA COMPLETA");
        Log.d(TAG, "════════════════════════════════════════════");

        int lugaresInsertados = 0;
        int pisosInsertados = 0;
        int espaciosInsertados = 0;
        int geometriasInsertadas = 0;

        try {
            // Iterar por cada lugar en la respuesta
            for (CouchDBRow row : response.rows) {
                if (row.value == null) {
                    Log.w(TAG, "Row sin valor, ignorando");
                    continue;
                }

                LugarCouchDB lugarCouchDB = row.value;

                // ═══════════════════════════════════════════════════════════════
                // INSERTAR LUGAR
                // ═══════════════════════════════════════════════════════════════
                int idLugarInsertado = insertarLugar(lugarCouchDB);
                if (idLugarInsertado != -1) {
                    lugaresInsertados++;

                    // ═══════════════════════════════════════════════════════════════
                    // INSERTAR PISOS Y SUS ESPACIOS
                    // ═══════════════════════════════════════════════════════════════
                    if (lugarCouchDB.pisos != null && !lugarCouchDB.pisos.isEmpty()) {
                        for (PisoCouchDB pisoCouchDB : lugarCouchDB.pisos) {
                            int idPisoInsertado = insertarPiso(pisoCouchDB, idLugarInsertado);
                            if (idPisoInsertado != -1) {
                                pisosInsertados++;

                                // ═══════════════════════════════════════════════════════════════
                                // INSERTAR ESPACIOS Y GEOMETRÍAS
                                // ═══════════════════════════════════════════════════════════════
                                if (pisoCouchDB.espacios != null && !pisoCouchDB.espacios.isEmpty()) {
                                    for (EspacioCouchDB espacioCouchDB : pisoCouchDB.espacios) {
                                        int idEspacioInsertado = insertarEspacio(
                                                espacioCouchDB,
                                                idLugarInsertado,
                                                idPisoInsertado
                                        );

                                        if (idEspacioInsertado != -1) {
                                            espaciosInsertados++;

                                            // Insertar geometría
                                            if (espacioCouchDB.geometria != null) {
                                                boolean geoInsertada = insertarGeometria(
                                                        espacioCouchDB.geometria,
                                                        idEspacioInsertado,
                                                        idLugarInsertado,
                                                        idPisoInsertado
                                                );
                                                if (geoInsertada) {
                                                    geometriasInsertadas++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mostrar resumen
            mostrarResumenInsercion(lugaresInsertados, pisosInsertados, espaciosInsertados, geometriasInsertadas);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "✗ Error procesando respuesta: " + e.getMessage());
            e.printStackTrace();
            mostrarToast("Error procesando datos");
            return false;
        }
    }

    /**
     * Insertar un lugar en la BD local
     */
    private int insertarLugar(LugarCouchDB lugarCouchDB) {
        try {
            Log.d(TAG, "╔════════════════════════════════════════");
            Log.d(TAG, "║ INSERTANDO LUGAR");
            Log.d(TAG, "╠════════════════════════════════════════");
            Log.d(TAG, "  Nombre: " + lugarCouchDB.nombre);
            Log.d(TAG, "  ID CouchDB: " + lugarCouchDB._id);
            Log.d(TAG, "  Estado: " + lugarCouchDB.estado);
            Log.d(TAG, "  Color: " + lugarCouchDB.color);

            // Preparar datos
            int id_l = lugarCouchDB.id_lugar;
            String nombre = lugarCouchDB.nombre;
            String descripcion = lugarCouchDB.descripcion != null ? lugarCouchDB.descripcion : "";
            String url_imagenes = lugarCouchDB.url_imagenes != null ? lugarCouchDB.url_imagenes : "";
            String geojson = lugarCouchDB.getGeojsonString();
            String color = lugarCouchDB.color != null ? lugarCouchDB.color : "#2196F3";
            int estado = lugarCouchDB.estado;

            Log.d(TAG, "  GeoJSON: " + geojson);

            // Insertar en BD (el método retorna el ID insertado o -1)
            long idInsertado = db.insertOrUpdateLugarSync(
                    id_l,
                    nombre,
                    descripcion,
                    url_imagenes,
                    geojson,
                    color,
                    estado
            );

            if (idInsertado != -1) {
                Log.d(TAG, "  id_lugar: " + idInsertado);
                Log.d(TAG, "  nombre: " + nombre);
                Log.d(TAG, "  descripcion: " + descripcion);
                Log.d(TAG, "╚════════════════════════════════════════");
                return (int) idInsertado;
            } else {
                Log.e(TAG, "  ✗ Error insertando lugar");
                Log.d(TAG, "╚════════════════════════════════════════");
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Error en insertarLugar: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Insertar un piso en la BD local
     */
    private int insertarPiso(PisoCouchDB pisoCouchDB, int idLugar) {
        try {
            Log.d(TAG, "  ┌─ INSERTANDO PISO");
            Log.d(TAG, "  │  Nombre: " + pisoCouchDB.nombre);
            Log.d(TAG, "  │  Número: " + pisoCouchDB.numero);
            Log.d(TAG, "  │  Activo: " + pisoCouchDB.activo);

            long idInsertado = db.insertOrUpdatePisoSync(
                    pisoCouchDB.id_piso,
                    idLugar,
                    pisoCouchDB.numero,
                    pisoCouchDB.nombre
            );

            if (idInsertado != -1) {
                Log.d(TAG, "  │  ✓ id_lugar: " + idLugar);
                Log.d(TAG, "  │  ✓ Id_Piso: " + idInsertado);
                Log.d(TAG, "  └─ FIN PISO");
                return (int) idInsertado;
            } else {
                Log.e(TAG, "  │  ✗ Error insertando piso");
                Log.d(TAG, "  └─ FIN PISO");
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Error en insertarPiso: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Insertar un espacio en la BD local
     */
    private int insertarEspacio(EspacioCouchDB espacioCouchDB, int idLugar, int idPiso) {
        try {
            Log.d(TAG, "    ├─ INSERTANDO ESPACIO");
            Log.d(TAG, "    │  Nombre: " + espacioCouchDB.nombre);
            Log.d(TAG, "    │  ID Lugar: " + idLugar + " | ID Piso: " + idPiso);

            long idInsertado = db.insertOrUpdateEspacio(
                    espacioCouchDB.id_espacio,
                    idLugar,
                    idPiso,
                    espacioCouchDB.nombre,
                    espacioCouchDB.descripcion != null ? espacioCouchDB.descripcion : "",
                    espacioCouchDB.url_imagenes != null ? espacioCouchDB.url_imagenes : "",
                    espacioCouchDB.estado
            );

            if (idInsertado != -1) {
                Log.d(TAG, "    │  ✓ id_espacio: " + idInsertado);
                Log.d(TAG, "    └─ FIN ESPACIO");
                return (int) idInsertado;
            } else {
                Log.e(TAG, "    │  ✗ Error insertando espacio");
                Log.d(TAG, "    └─ FIN ESPACIO");
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Error en insertarEspacio: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Insertar una geometría en la BD local
     */
    private boolean insertarGeometria(GeometriaCouchDB geometriaCouchDB, int idEspacio, int idLugar, int idPiso) {
        try {
            Log.d(TAG, "      ├─ INSERTANDO GEOMETRÍA");
            Log.d(TAG, "      │  Tipo: " + geometriaCouchDB.tipo);
            Log.d(TAG, "      │  Color: " + geometriaCouchDB.color);

            String vertices = geometriaCouchDB.getVerticesString();
            Log.d(TAG, "      │  Vértices: " + vertices);

            long idInsertado = db.insertOrUpdateGeometria(
                    geometriaCouchDB.id_geometria,
                    idEspacio,
                    idLugar,
                    idPiso,
                    vertices,
                    geometriaCouchDB.color
            );

            if (idInsertado != -1) {
                Log.d(TAG, "      │ id_geometria: " + idInsertado);
                Log.d(TAG, "      │ id_espacio: " + idEspacio);
                Log.d(TAG, "      │ id_lugar: " + idLugar);
                Log.d(TAG, "      │ id_piso: " + idPiso);
                Log.d(TAG, "      └─ FIN GEOMETRÍA");
                return true;
            } else {
                Log.e(TAG, "      │  ✗ Error insertando geometría");
                Log.d(TAG, "      └─ FIN GEOMETRÍA");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Error en insertarGeometria: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mostrar resumen de inserción
     */
    private void mostrarResumenInsercion(int lugares, int pisos, int espacios, int geometrias) {
        Log.d(TAG, "════════════════════════════════════════════");
        Log.d(TAG, "✅ INSERCIÓN COMPLETADA");
        Log.d(TAG, "════════════════════════════════════════════");
        Log.d(TAG, "  Lugares insertados: " + lugares);
        Log.d(TAG, "  Pisos insertados: " + pisos);
        Log.d(TAG, "  Espacios insertados: " + espacios);
        Log.d(TAG, "  Geometrías insertadas: " + geometrias);
        Log.d(TAG, "════════════════════════════════════════════");

        String mensaje = String.format(
                "✅ Datos cargados:\n" +
                        "  📍 Lugares: %d\n" +
                        "  🏢 Pisos: %d\n" +
                        "  🚪 Espacios: %d\n" +
                        "  📐 Geometrías: %d",
                lugares, pisos, espacios, geometrias
        );

        mostrarToast(mensaje);
    }

    /**
     * Mostrar toast en el contexto
     */
    private void mostrarToast(String mensaje) {
        if (context != null) {
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.post(() -> {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
            });
        }
    }
}