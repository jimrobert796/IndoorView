package com.example.indoorview;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SyncManager {

    private static final String TAG = "SYNC_MANAGER";

    private final Context context;
    private final Database db;
    private final FirebaseHelper firebaseHelper;
    private final ImageStorageManager imageStorageManager;

    private SyncListener listener;

    public interface SyncListener {
        void onProgress(String message, int progress, int total);
        void onComplete(String message);
        void onError(String error);
    }

    public SyncManager(Context context, Database db, FirebaseHelper firebaseHelper) {
        this.context = context;
        this.db = db;
        this.firebaseHelper = firebaseHelper;
        this.imageStorageManager = new ImageStorageManager(context);
    }

    public void setSyncListener(SyncListener listener) {
        this.listener = listener;
    }

    // Metodo que inica toda la sincronizacion de mapa al iniciar
    public void syncAllMap() {
        Log.d(TAG, "🔄 INICIANDO SINCRONIZACIÓN COMPLETA");
        notifyProgress("Sincronizando lugares...", 0, 4);
        sincronizarLugares();
    }


    public void syncAllMapWithClean() {
        Log.d(TAG, "🧹 LIMPIANDO BD LOCAL ANTES DE SINCRONIZAR");
        db.limpiarTablasMapa();
        syncAllMap();
    }



    private void sincronizarLugares() {
        firebaseHelper.obtenerTodosLugares(new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> lugares) {
                Log.d(TAG, "✅ " + lugares.size() + " lugares obtenidos de Firebase");
                if (lugares.isEmpty()) {
                    notifyError("No hay lugares para sincronizar");
                    return;
                }
                procesarLugaresEnCadena(lugares, 0);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error sincronizando lugares: " + error);
                notifyError(error);
            }
        });
    }

    private void procesarLugaresEnCadena(List<DocumentSnapshot> lugares, int indice) {
        if (indice >= lugares.size()) {
            Log.d(TAG, "✅ Todos los lugares procesados");
            notifyComplete("Sincronización completada");
            return;
        }

        DocumentSnapshot lugar = lugares.get(indice);
        guardarLugarLocalYContinuar(lugar, lugares, indice);
    }



    private void guardarLugarLocalYContinuar(DocumentSnapshot lugar,
                                             List<DocumentSnapshot> lugares,
                                             int indice) {
        try {
            String lugarFirebaseId = lugar.getId();
            String nombre = lugar.getString("nombre");
            String descripcion = lugar.getString("descripcion");
            String urlImagenesFirebase = lugar.getString("url_imagenes");
            String color = lugar.getString("color");
            String geojson = lugar.getString("geojson");

            Log.d(TAG, "════════════════════════════════════════════");
            Log.d(TAG, "📍 INSERTANDO LUGAR EN BD LOCAL");
            Log.d(TAG, "════════════════════════════════════════════");
            Log.d(TAG, "  DATOS A GUARDAR:");
            Log.d(TAG, "    • nombre: " + nombre);
            Log.d(TAG, "    • descripcion: " + descripcion);
            Log.d(TAG, "    • color: " + color);
            Log.d(TAG, "    • geojson: " + (geojson != null && geojson.length() > 50 ? geojson.substring(0, 50) + "..." : geojson));
            Log.d(TAG, "    • url_imagenes (Firebase): " + urlImagenesFirebase);
            Log.d(TAG, "    • estado: 1");


            procesarMultiplesImagenes(urlImagenesFirebase, new ImageProcessCallback() {
                @Override
                public void onComplete(String urlImagenesLocal) {

                    Log.d(TAG, "IMAGENES LOCALES DE LUGAR: " + urlImagenesLocal);
                    long idLugarLocal = db.insertLugar(nombre, descripcion, urlImagenesLocal, geojson, color);

                    if (idLugarLocal == -1) {
                        Log.e(TAG, "❌ Error al guardar lugar: " + nombre);
                        procesarLugaresEnCadena(lugares, indice + 1);
                        return;
                    }

                    Log.d(TAG, "════════════════════════════════════════════");
                    Log.d(TAG, "✅ LUGAR GUARDADO EN BD LOCAL");
                    Log.d(TAG, "  • ID Local (generado): " + idLugarLocal);
                    Log.d(TAG, "  • Firebase ID: " + lugarFirebaseId);
                    Log.d(TAG, "════════════════════════════════════════════");

                    String nombrePrimerPiso = "Primera Planta";
                    long idPrimerPiso = db.insertPiso((int)idLugarLocal, 1, nombrePrimerPiso);

                    if (idPrimerPiso == -1) {
                        Log.e(TAG, "❌ Error al crear primer piso");
                        procesarLugaresEnCadena(lugares, indice + 1);
                        return;
                    }

                    Log.d(TAG, "════════════════════════════════════════════");
                    Log.d(TAG, "📦 INSERTANDO PRIMER PISO EN BD LOCAL");
                    Log.d(TAG, "════════════════════════════════════════════");
                    Log.d(TAG, "  DATOS A GUARDAR:");
                    Log.d(TAG, "    • id_lugar (FK): " + idLugarLocal);
                    Log.d(TAG, "    • numero: 1");
                    Log.d(TAG, "    • nombre: " + nombrePrimerPiso);
                    Log.d(TAG, "    • activo: 1");
                    Log.d(TAG, "════════════════════════════════════════════");
                    Log.d(TAG, "✅ PISO GUARDADO EN BD LOCAL");
                    Log.d(TAG, "  • ID Local (generado): " + idPrimerPiso);
                    Log.d(TAG, "  • ID Lugar (FK): " + idLugarLocal);
                    Log.d(TAG, "════════════════════════════════════════════");

                    sincronizarEspaciosDelLugarYContinuar(lugarFirebaseId, (int)idLugarLocal, (int)idPrimerPiso, lugares, indice);

                }
            });



        } catch (Exception e) {
            Log.e(TAG, "Error guardando lugar: " + e.getMessage());
            e.printStackTrace();
            procesarLugaresEnCadena(lugares, indice + 1);
        }
    }

    private void sincronizarEspaciosDelLugarYContinuar(String lugarFirebaseId,
                                                       int idLugarLocal,
                                                       int idPrimerPisoLocal,
                                                       List<DocumentSnapshot> lugares,
                                                       int indiceLugar) {
        firebaseHelper.obtenerPisosDeLugar(lugarFirebaseId, new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> pisos) {
                Log.d(TAG, "  📦 " + pisos.size() + " pisos encontrados en Firebase");
                if (pisos.isEmpty()) {
                    Log.d(TAG, "  ⚠️ Sin pisos, continuando");
                    procesarLugaresEnCadena(lugares, indiceLugar + 1);
                    return;
                }
                procesarPisosYEspacios(pisos, 0, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error obteniendo pisos: " + error);
                procesarLugaresEnCadena(lugares, indiceLugar + 1);
            }
        });
    }

    /**
     * ✅ CORREGIDO: NO convertir pisoFirebaseId a número
     */
    private void procesarPisosYEspacios(List<DocumentSnapshot> pisos,
                                        int indicePiso,
                                        int idLugarLocal,
                                        String lugarFirebaseId,
                                        List<DocumentSnapshot> lugares,
                                        int indiceLugar) {
        if (indicePiso >= pisos.size()) {
            Log.d(TAG, "✅ Todos los pisos procesados para lugar");
            procesarLugaresEnCadena(lugares, indiceLugar + 1);
            return;
        }

        DocumentSnapshot piso = pisos.get(indicePiso);
        String pisoFirebaseId = piso.getId();
        String nombrePiso = piso.getString("nombre");

        Log.d(TAG, "  📦 Procesando piso de Firebase: " + nombrePiso + " (ID Firebase: " + pisoFirebaseId + ")");

        firebaseHelper.obtenerEspaciosDePiso(lugarFirebaseId, pisoFirebaseId, new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> espacios) {
                Log.d(TAG, "    🏢 " + espacios.size() + " espacios encontrados en piso: " + nombrePiso);
                if (espacios.isEmpty()) {
                    Log.d(TAG, "    ⚠️ Sin espacios en este piso");
                    procesarPisosYEspacios(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
                    return;
                }

                // ✅ NO CONVERTIR - pisoFirebaseId es STRING como "Primera Planta"
                // Usar 1 como número de piso fijo
                long idPisoLocal = db.insertPiso(idLugarLocal, 1, nombrePiso);

                Log.d(TAG, "    📦 Piso creado/obtenido - ID LOCAL: " + idPisoLocal + " (FK Lugar: " + idLugarLocal + ")");

                procesarEspaciosEnCadena(espacios, 0, idLugarLocal, (int)idPisoLocal, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error obteniendo espacios: " + error);
                procesarPisosYEspacios(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
            }
        });
    }

    private void procesarEspaciosEnCadena(List<DocumentSnapshot> espacios,
                                          int indiceEspacio,
                                          int idLugarLocal,
                                          int idPisoLocal,
                                          List<DocumentSnapshot> pisos,
                                          int indicePiso,
                                          String lugarFirebaseId,
                                          List<DocumentSnapshot> lugares,
                                          int indiceLugar) {
        if (indiceEspacio >= espacios.size()) {
            procesarPisosYEspacios(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
            return;
        }

        DocumentSnapshot espacio = espacios.get(indiceEspacio);
        guardarEspacioYGeometriaLocalYContinuar(
                espacio, idLugarLocal, idPisoLocal, espacios, indiceEspacio, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar
        );
    }

    private void guardarEspacioYGeometriaLocalYContinuar(DocumentSnapshot espacio,
                                                         int idLugarLocal,
                                                         int idPisoLocal,
                                                         List<DocumentSnapshot> espacios,
                                                         int indiceEspacio,
                                                         List<DocumentSnapshot> pisos,
                                                         int indicePiso,
                                                         String lugarFirebaseId,
                                                         List<DocumentSnapshot> lugares,
                                                         int indiceLugar) {
        try {
            String espacioFirebaseId = espacio.getId();
            String nombre = espacio.getString("nombre");
            String descripcion = espacio.getString("descripcion");
            String urlImagenesFirebase = espacio.getString("url_imagenes");
            String color = espacio.getString("color");
            int estado = espacio.getLong("estado") != null ?
                    espacio.getLong("estado").intValue() : 1;

            Log.d(TAG, "════════════════════════════════════════════");
            Log.d(TAG, "🏢 INSERTANDO ESPACIO EN BD LOCAL");
            Log.d(TAG, "════════════════════════════════════════════");
            Log.d(TAG, "  DATOS A GUARDAR:");
            Log.d(TAG, "    • id_lugar (FK): " + idLugarLocal);
            Log.d(TAG, "    • id_piso (FK): " + idPisoLocal + " ✅");
            Log.d(TAG, "    • nombre: " + nombre);
            Log.d(TAG, "    • descripcion: " + descripcion);
            Log.d(TAG, "    • color: " + color);
            Log.d(TAG, "    • url_imagenes (Firebase): " + urlImagenesFirebase);
            Log.d(TAG, "    • estado: " + estado);

            procesarMultiplesImagenes(urlImagenesFirebase, new ImageProcessCallback() {
                        @Override
                        public void onComplete(String urlImagenesLocal) {

                            long idEspacioLocal = db.insertEspacio(idLugarLocal, idPisoLocal, nombre, descripcion, urlImagenesLocal);
                            Log.d(TAG, "  • url_imagenes: " + urlImagenesLocal );

                            if (idEspacioLocal == -1) {
                                Log.e(TAG, "❌ Error al guardar espacio: " + nombre);
                                procesarEspaciosEnCadena(espacios, indiceEspacio + 1, idLugarLocal, idPisoLocal, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar);
                                return;
                            }

                            Log.d(TAG, "════════════════════════════════════════════");
                            Log.d(TAG, "✅ ESPACIO GUARDADO EN BD LOCAL");
                            Log.d(TAG, "  • ID Local (generado): " + idEspacioLocal);
                            Log.d(TAG, "  • Firebase ID: " + espacioFirebaseId);
                            Log.d(TAG, "  • ID Lugar (FK): " + idLugarLocal);
                            Log.d(TAG, "  • ID Piso (FK): " + idPisoLocal + " ✅");
                            Log.d(TAG, "════════════════════════════════════════════");

                            DocumentSnapshot espacioDoc = espacio;
                            espacioDoc.getReference()
                                    .collection("geometria")
                                    .document("vertices")
                                    .get()
                                    .addOnSuccessListener(geoDoc -> {
                                        if (geoDoc.exists()) {
                                            String vertices = geoDoc.getString("vertices");
                                            String colorGeo = geoDoc.getString("color");

                                            Log.d(TAG, "════════════════════════════════════════════");
                                            Log.d(TAG, "📐 INSERTANDO GEOMETRÍA EN BD LOCAL");
                                            Log.d(TAG, "════════════════════════════════════════════");
                                            Log.d(TAG, "  DATOS A GUARDAR:");
                                            Log.d(TAG, "    • id_espacio (FK): " + idEspacioLocal);
                                            Log.d(TAG, "    • id_lugar (FK): " + idLugarLocal);
                                            Log.d(TAG, "    • id_piso (FK): " + idPisoLocal + " ✅");
                                            Log.d(TAG, "    • tipo: polygon");
                                            Log.d(TAG, "    • vertices: " + (vertices != null && vertices.length() > 50 ? vertices.substring(0, 50) + "..." : vertices));
                                            Log.d(TAG, "    • color: " + colorGeo);

                                            long idGeometria = db.insertGeometria(
                                                    (int)idEspacioLocal,
                                                    idLugarLocal,
                                                    idPisoLocal,
                                                    vertices,
                                                    colorGeo
                                            );

                                            if (idGeometria != -1) {
                                                Log.d(TAG, "════════════════════════════════════════════");
                                                Log.d(TAG, "✅ GEOMETRÍA GUARDADA EN BD LOCAL");
                                                Log.d(TAG, "  • ID Local (generado): " + idGeometria);
                                                Log.d(TAG, "  • ID Espacio (FK): " + idEspacioLocal);
                                                Log.d(TAG, "  • ID Lugar (FK): " + idLugarLocal);
                                                Log.d(TAG, "  • ID Piso (FK): " + idPisoLocal + " ✅");
                                                Log.d(TAG, "════════════════════════════════════════════");
                                            } else {
                                                Log.e(TAG, "❌ Error al guardar geometría");
                                            }
                                        }

                                        procesarEspaciosEnCadena(espacios, indiceEspacio + 1, idLugarLocal, idPisoLocal, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Error obteniendo geometría: " + e.getMessage());
                                        procesarEspaciosEnCadena(espacios, indiceEspacio + 1, idLugarLocal, idPisoLocal, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar);
                                    });

                        }
            });




        } catch (Exception e) {
            Log.e(TAG, "Error guardando espacio: " + e.getMessage());
            e.printStackTrace();
            procesarEspaciosEnCadena(espacios, indiceEspacio + 1, idLugarLocal, idPisoLocal, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar);
        }
    }

    /**
     * Procesa múltiples URLs de imágenes (separadas por coma)
     * Descarga cada una, guarda localmente y retorna las rutas locales
     */
    private void procesarMultiplesImagenes(String urlsCloudinary,
                                           ImageProcessCallback callback) {
        if (urlsCloudinary == null || urlsCloudinary.isEmpty()) {
            if (callback != null) callback.onComplete("");
            return;
        }

        String[] urls = urlsCloudinary.split(",");
        List<String> rutasLocales = new ArrayList<>();
        int[] contador = {0};
        int total = urls.length;

        for (int i = 0; i < total; i++) {
            final String urlActual = urls[i].trim(); // ✅ final desde el inicio

            if (urlActual.isEmpty()) {
                contador[0]++;
                if (contador[0] == total && callback != null) {
                    callback.onComplete(String.join(",", rutasLocales));
                }
                continue;
            }

            Log.d(TAG, "📥 Descargando: " + urlActual);

            imageStorageManager.descargarYGuardarImagen(urlActual,
                    new ImageStorageManager.ImageDownloadCallback() {
                        @Override
                        public void onSuccess(String rutaLocal) {
                            rutasLocales.add(rutaLocal);
                            contador[0]++;
                            verificarCompletado();
                        }

                        @Override
                        public void onError(String error) {
                            rutasLocales.add(urlActual);
                            contador[0]++;
                            verificarCompletado();
                        }

                        private void verificarCompletado() {
                            if (contador[0] == total && callback != null) {
                                callback.onComplete(String.join(",", rutasLocales));
                            }
                        }
                    });
        }
    }

    // Interfaz de callback
    public interface ImageProcessCallback {
        void onComplete(String urlsProcesadas);
    }

    private void notifyProgress(String message, int progress, int total) {
        Log.d(TAG, message + " (" + progress + "/" + total + ")");
        if (listener != null) {
            listener.onProgress(message, progress, total);
        }
    }

    private void notifyComplete(String message) {
        Log.d(TAG, "✅ " + message);
        if (listener != null) {
            listener.onComplete(message);
        }
    }

    private void notifyError(String error) {
        Log.e(TAG, "❌ " + error);
        if (listener != null) {
            listener.onError(error);
        }
    }
}