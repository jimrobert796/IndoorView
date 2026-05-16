package com.example.indoorview;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SyncManager {

    // AUN NO LO HE PROBADO PERO SE INTENTARA DESPUES

    private static final String TAG = "SYNC_MANAGER";

    private final Context context;
    private final Database db;
    private final FirebaseHelper firebaseHelper;
    private final ImageStorageManager imageStorageManager;  // ← AGREGAR ESTO

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
        this.imageStorageManager = new ImageStorageManager(context);  // ← INICIALIZAR
    }

    public void setSyncListener(SyncListener listener) {
        this.listener = listener;
    }

    /**
     * SINCRONIZACIÓN COMPLETA EN CADENA
     * Orden: Lugares → Pisos → Espacios → Geometrías
     */
    public void syncAll() {
        Log.d(TAG, "🔄 INICIANDO SINCRONIZACIÓN COMPLETA");
        notifyProgress("Sincronizando lugares...", 0, 4);
        sincronizarLugares();
    }

    // ==================== 1. SINCRONIZAR LUGARES ====================
    private void sincronizarLugares() {
        firebaseHelper.obtenerTodosLugares(new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> lugares) {
                Log.d(TAG, "✅ " + lugares.size() + " lugares obtenidos");

                if (lugares.isEmpty()) {
                    notifyError("No hay lugares para sincronizar");
                    return;
                }

                // Procesar lugares en cadena
                procesarLugaresEnCadena(lugares, 0);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error sincronizando lugares: " + error);
                notifyError(error);
            }
        });
    }

    /**
     * Procesa lugares recursivamente en cadena
     */
    private void procesarLugaresEnCadena(List<DocumentSnapshot> lugares, int indice) {
        if (indice >= lugares.size()) {
            // Todos los lugares procesados
            notifyProgress("Sincronizando pisos...", 1, 4);
            return;
        }

        DocumentSnapshot lugar = lugares.get(indice);
        guardarLugarLocalYContinuar(lugar, lugares, indice);
    }

    /**
     * Guarda un lugar y luego procesa el siguiente
     */
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
            int estado = lugar.getLong("estado") != null ?
                    lugar.getLong("estado").intValue() : 1;

            // ✅ PROCESAR IMÁGENES (descargar y guardar localmente)
            String urlImagenesLocal = procesarMultiplesImagenes(urlImagenesFirebase);

            Log.d(TAG, "  🖼️ Imágenes del lugar: " + nombre);
            Log.d(TAG, "     Firebase: " + urlImagenesFirebase);
            Log.d(TAG, "     Local: " + urlImagenesLocal);

            // ✅ GUARDAR LUGAR con URLs locales
            long idLugarLocal = db.insertOrUpdateLugarSync(
                    Integer.parseInt(lugarFirebaseId),
                    nombre,
                    descripcion,
                    urlImagenesLocal,  // ← Usar URLs locales
                    geojson,
                    color,
                    estado
            );

            Log.d(TAG, "  ✓ Lugar guardado: " + nombre + " (ID Local: " + idLugarLocal + ")");

            // 🔄 AHORA procesar los pisos de este lugar
            sincronizarPisosDeLugarYContinuar(lugarFirebaseId, (int)idLugarLocal, lugares, indice);

        } catch (Exception e) {
            Log.e(TAG, "Error guardando lugar: " + e.getMessage());
            // Continuar con el siguiente lugar
            procesarLugaresEnCadena(lugares, indice + 1);
        }
    }

    // ==================== 2. SINCRONIZAR PISOS ====================
    private void sincronizarPisosDeLugarYContinuar(String lugarFirebaseId,
                                                   int idLugarLocal,
                                                   List<DocumentSnapshot> lugares,
                                                   int indiceLugar) {
        firebaseHelper.obtenerPisosDeLugar(lugarFirebaseId, new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> pisos) {
                Log.d(TAG, "  📦 " + pisos.size() + " pisos para lugar: " + lugarFirebaseId);

                if (pisos.isEmpty()) {
                    Log.d(TAG, "  ⚠️ No hay pisos, continuando con siguiente lugar");
                    procesarLugaresEnCadena(lugares, indiceLugar + 1);
                    return;
                }

                // Procesar pisos en cadena
                procesarPisosEnCadena(pisos, 0, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error sincronizando pisos: " + error);
                // Continuar con siguiente lugar
                procesarLugaresEnCadena(lugares, indiceLugar + 1);
            }
        });
    }

    /**
     * Procesa pisos recursivamente
     */
    private void procesarPisosEnCadena(List<DocumentSnapshot> pisos,
                                       int indicePiso,
                                       int idLugarLocal,
                                       String lugarFirebaseId,
                                       List<DocumentSnapshot> lugares,
                                       int indiceLugar) {
        if (indicePiso >= pisos.size()) {
            // Todos los pisos procesados, continuar con siguiente lugar
            procesarLugaresEnCadena(lugares, indiceLugar + 1);
            return;
        }

        DocumentSnapshot piso = pisos.get(indicePiso);
        guardarPisoLocalYContinuar(piso, idLugarLocal, pisos, indicePiso, lugarFirebaseId, lugares, indiceLugar);
    }

    /**
     * Guarda un piso y luego procesa el siguiente
     */
    private void guardarPisoLocalYContinuar(DocumentSnapshot piso,
                                            int idLugarLocal,
                                            List<DocumentSnapshot> pisos,
                                            int indicePiso,
                                            String lugarFirebaseId,
                                            List<DocumentSnapshot> lugares,
                                            int indiceLugar) {
        try {
            String pisoId = piso.getId();
            String nombrePiso = piso.getString("nombre");
            int numeroPiso = Integer.parseInt(pisoId);
            int estado = piso.getLong("estado") != null ?
                    piso.getLong("estado").intValue() : 1;

            // ✅ GUARDAR PISO y obtener su ID local
            long idPisoLocal = db.insertOrUpdatePisoSync(
                    Integer.parseInt(pisoId),
                    idLugarLocal,
                    numeroPiso,
                    nombrePiso
            );

            Log.d(TAG, "    ✓ Piso guardado: " + nombrePiso + " (ID Local: " + idPisoLocal + ")");

            // 🔄 AHORA procesar espacios de este piso
            sincronizarEspaciosDePisoYContinuar(
                    lugarFirebaseId,
                    pisoId,
                    idLugarLocal,
                    (int)idPisoLocal,
                    pisos,
                    indicePiso,
                    lugares,
                    indiceLugar
            );

        } catch (Exception e) {
            Log.e(TAG, "Error guardando piso: " + e.getMessage());
            // Continuar con siguiente piso
            procesarPisosEnCadena(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
        }
    }

    // ==================== 3. SINCRONIZAR ESPACIOS ====================
    private void sincronizarEspaciosDePisoYContinuar(String lugarFirebaseId,
                                                     String pisoId,
                                                     int idLugarLocal,
                                                     int idPisoLocal,
                                                     List<DocumentSnapshot> pisos,
                                                     int indicePiso,
                                                     List<DocumentSnapshot> lugares,
                                                     int indiceLugar) {
        firebaseHelper.obtenerEspaciosDePiso(lugarFirebaseId, pisoId, new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> espacios) {
                Log.d(TAG, "    🏢 " + espacios.size() + " espacios para piso: " + pisoId);

                if (espacios.isEmpty()) {
                    Log.d(TAG, "    ⚠️ No hay espacios, continuando con siguiente piso");
                    procesarPisosEnCadena(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
                    return;
                }

                // Procesar espacios en cadena
                procesarEspaciosEnCadena(
                        espacios, 0, idPisoLocal, idLugarLocal,
                        lugarFirebaseId, pisoId, pisos, indicePiso, lugares, indiceLugar
                );
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error sincronizando espacios: " + error);
                procesarPisosEnCadena(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
            }
        });
    }

    /**
     * Procesa espacios recursivamente
     */
    private void procesarEspaciosEnCadena(List<DocumentSnapshot> espacios,
                                          int indiceEspacio,
                                          int idPisoLocal,
                                          int idLugarLocal,
                                          String lugarFirebaseId,
                                          String pisoId,
                                          List<DocumentSnapshot> pisos,
                                          int indicePiso,
                                          List<DocumentSnapshot> lugares,
                                          int indiceLugar) {
        if (indiceEspacio >= espacios.size()) {
            // Todos los espacios procesados, continuar con siguiente piso
            procesarPisosEnCadena(pisos, indicePiso + 1, idLugarLocal, lugarFirebaseId, lugares, indiceLugar);
            return;
        }

        DocumentSnapshot espacio = espacios.get(indiceEspacio);
        guardarEspacioLocalYContinuar(
                espacio, idPisoLocal, idLugarLocal, espacios, indiceEspacio,
                lugarFirebaseId, pisoId, pisos, indicePiso, lugares, indiceLugar
        );
    }

    /**
     * Guarda un espacio y luego procesa el siguiente
     */
    private void guardarEspacioLocalYContinuar(DocumentSnapshot espacio,
                                               int idPisoLocal,
                                               int idLugarLocal,
                                               List<DocumentSnapshot> espacios,
                                               int indiceEspacio,
                                               String lugarFirebaseId,
                                               String pisoId,
                                               List<DocumentSnapshot> pisos,
                                               int indicePiso,
                                               List<DocumentSnapshot> lugares,
                                               int indiceLugar) {
        try {
            String espacioFirebaseId = espacio.getId();
            String nombre = espacio.getString("nombre");
            String tipo = espacio.getString("tipo");
            String color = espacio.getString("color");
            String descripcion = espacio.getString("descripcion");
            String urlImagenes = espacio.getString("url_imagenes");
            int estado = espacio.getLong("estado") != null ?
                    espacio.getLong("estado").intValue() : 1;

            // ✅ GUARDAR ESPACIO y obtener su ID local
            long idEspacioLocal = db.insertOrUpdateEspacio(
                    Integer.parseInt(espacioFirebaseId),
                    idLugarLocal,
                    idPisoLocal,
                    nombre,
                    descripcion,
                    urlImagenes,
                    estado
            );

            Log.d(TAG, "      ✓ Espacio guardado: " + nombre + " (ID Local: " + idEspacioLocal + ")");

            // 🔄 AHORA procesar geometrías de este espacio
            sincronizarGeometriasDeEspacioYContinuar(
                    lugarFirebaseId,
                    pisoId,
                    espacioFirebaseId,
                    (int)idEspacioLocal,
                    idPisoLocal,
                    idLugarLocal,
                    espacios,
                    indiceEspacio,
                    pisos,
                    indicePiso,
                    lugares,
                    indiceLugar
            );

        } catch (Exception e) {
            Log.e(TAG, "Error guardando espacio: " + e.getMessage());
            procesarEspaciosEnCadena(
                    espacios, indiceEspacio + 1, idPisoLocal, idLugarLocal,
                    lugarFirebaseId, pisoId, pisos, indicePiso, lugares, indiceLugar
            );
        }
    }

    // ==================== 4. SINCRONIZAR GEOMETRÍAS ====================
    private void sincronizarGeometriasDeEspacioYContinuar(String lugarFirebaseId,
                                                          String pisoId,
                                                          String espacioFirebaseId,
                                                          int idEspacioLocal,
                                                          int idPisoLocal,
                                                          int idLugarLocal,
                                                          List<DocumentSnapshot> espacios,
                                                          int indiceEspacio,
                                                          List<DocumentSnapshot> pisos,
                                                          int indicePiso,
                                                          List<DocumentSnapshot> lugares,
                                                          int indiceLugar) {
        firebaseHelper.obtenerGeometriasDeEspacio(lugarFirebaseId, pisoId, espacioFirebaseId,
                new FirebaseHelper.FirebaseListCallback() {
                    @Override
                    public void onSuccess(List<DocumentSnapshot> geometrias) {
                        Log.d(TAG, "        🔷 " + geometrias.size() + " geometrías para espacio: " + espacioFirebaseId);

                        if (geometrias.isEmpty()) {
                            Log.d(TAG, "        ⚠️ No hay geometrías, continuando con siguiente espacio");
                            procesarEspaciosEnCadena(
                                    espacios, indiceEspacio + 1, idPisoLocal, idLugarLocal,
                                    lugarFirebaseId, pisoId, pisos, indicePiso, lugares, indiceLugar
                            );
                            return;
                        }

                        // Procesar geometrías en cadena
                        procesarGeometriasEnCadena(
                                geometrias, 0, idEspacioLocal, idPisoLocal, idLugarLocal,
                                espacios, indiceEspacio, pisos, indicePiso, lugares, indiceLugar
                        );
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error sincronizando geometrías: " + error);
                        procesarEspaciosEnCadena(
                                espacios, indiceEspacio + 1, idPisoLocal, idLugarLocal,
                                lugarFirebaseId, pisoId, pisos, indicePiso, lugares, indiceLugar
                        );
                    }
                });
    }

    /**
     * Procesa geometrías recursivamente
     */
    private void procesarGeometriasEnCadena(List<DocumentSnapshot> geometrias,
                                            int indiceGeometria,
                                            int idEspacioLocal,
                                            int idPisoLocal,
                                            int idLugarLocal,
                                            List<DocumentSnapshot> espacios,
                                            int indiceEspacio,
                                            List<DocumentSnapshot> pisos,
                                            int indicePiso,
                                            List<DocumentSnapshot> lugares,
                                            int indiceLugar) {
        if (indiceGeometria >= geometrias.size()) {
            // Todas las geometrías procesadas, continuar con siguiente espacio
            String lugarFirebaseId = lugares.get(indiceLugar).getId();
            String pisoId = pisos.get(indicePiso).getId();
            procesarEspaciosEnCadena(
                    espacios, indiceEspacio + 1, idPisoLocal, idLugarLocal,
                    lugarFirebaseId, pisoId, pisos, indicePiso, lugares, indiceLugar
            );
            return;
        }

        DocumentSnapshot geometria = geometrias.get(indiceGeometria);
        guardarGeometriaLocalYContinuar(
                geometria, idEspacioLocal, idPisoLocal, idLugarLocal, geometrias, indiceGeometria,
                espacios, indiceEspacio, pisos, indicePiso, lugares, indiceLugar
        );
    }

    /**
     * Guarda una geometría y luego procesa la siguiente
     */
    private void guardarGeometriaLocalYContinuar(DocumentSnapshot geometria,
                                                 int idEspacioLocal,
                                                 int idPisoLocal,
                                                 int idLugarLocal,
                                                 List<DocumentSnapshot> geometrias,
                                                 int indiceGeometria,
                                                 List<DocumentSnapshot> espacios,
                                                 int indiceEspacio,
                                                 List<DocumentSnapshot> pisos,
                                                 int indicePiso,
                                                 List<DocumentSnapshot> lugares,
                                                 int indiceLugar) {
        try {
            String geometriaId = geometria.getId();
            String geojson = geometria.getString("geojson");
            String nombre = geometria.getString("nombre");
            String color = geometria.getString("color");

            // ✅ GUARDAR GEOMETRÍA con todos los IDs locales
            long idGeometriaLocal = db.insertOrUpdateGeometria(
                    Integer.parseInt(geometriaId),
                    idEspacioLocal,
                    idLugarLocal,
                    idPisoLocal,
                    geojson,
                    color
            );

            Log.d(TAG, "        ✓ Geometría guardada: " + nombre + " (ID Local: " + idGeometriaLocal + ")");

            // Continuar con siguiente geometría
            procesarGeometriasEnCadena(
                    geometrias, indiceGeometria + 1, idEspacioLocal, idPisoLocal, idLugarLocal,
                    espacios, indiceEspacio, pisos, indicePiso, lugares, indiceLugar
            );

        } catch (Exception e) {
            Log.e(TAG, "Error guardando geometría: " + e.getMessage());
            procesarGeometriasEnCadena(
                    geometrias, indiceGeometria + 1, idEspacioLocal, idPisoLocal, idLugarLocal,
                    espacios, indiceEspacio, pisos, indicePiso, lugares, indiceLugar
            );
        }
    }

    // ==================== PROCESAMIENTO DE IMÁGENES ====================
    /**
     * Procesa múltiples URLs de imágenes (separadas por coma)
     * Descarga cada una, guarda localmente y retorna las rutas locales
     */
    private String procesarMultiplesImagenes(String urlsCloudinary) {
        if (urlsCloudinary == null || urlsCloudinary.isEmpty()) {
            return "";
        }

        // Separar URLs por coma
        String[] urls = urlsCloudinary.split(",");
        List<String> rutasLocales = new ArrayList<>();

        Log.d(TAG, "📸 Procesando " + urls.length + " imágenes");

        for (String url : urls) {
            url = url.trim(); // Remover espacios

            if (url.isEmpty()) continue;

            // Descargar y guardar cada imagen
            String rutaLocal = imageStorageManager.descargarYGuardarImagen(url);

            if (rutaLocal != null) {
                rutasLocales.add(rutaLocal);
                Log.d(TAG, "  ✓ Imagen guardada: " + rutaLocal);
            } else {
                Log.w(TAG, "  ⚠️ Error descargando: " + url);
            }
        }

        // Retornar rutas locales separadas por coma
        return String.join(",", rutasLocales);
    }

    // ==================== NOTIFICACIONES ====================
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