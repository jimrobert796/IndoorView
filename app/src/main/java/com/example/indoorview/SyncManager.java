package com.example.indoorview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.indoorview.models.Eventos;
import com.example.indoorview.models.Usuarios;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Para manejar su propio thread
    public interface SyncCallback {
        void onSyncComplete();
        void onSyncError(String error);
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
        Log.d(TAG, "LIMPIANDO BD LOCAL ANTES DE SINCRONIZAR");
        db.limpiarTablasMapa();
        syncAllMap();
    }

    public void syncAllEventosWithClean(SyncCallback callback) {

        limpiarEventosPasados();// Automaticamente

        // PASO 1: Sincronizar eventos NUEVOS (estado = 3)
        syncEventosPendientesAgregar(new SyncCallback() {
            @Override
            public void onSyncComplete() {
                Log.d(TAG, "✅ Eventos nuevos sincronizados");

                // PASO 2: Sincronizar eventos MODIFICADOS (estado = 2)
                syncEventosPendientesModificar(new SyncCallback() {
                    @Override
                    public void onSyncComplete() {
                        Log.d(TAG, "✅ Eventos modificados sincronizados");

                        // PASO 3: Sincronizar eventos ELIMINADOS (estado = 4)
                        syncEventosPendientesEliminar(new SyncCallback() {
                            @Override
                            public void onSyncComplete() {
                                Log.d(TAG, "✅ Eventos eliminados sincronizados");

                                // PASO 4: LIMPIAR BD LOCAL
                                db.limpiarTablaEventos();
                                Log.d(TAG, "✅ BD limpiada");

                                // PASO 5: SINCRONIZAR DESDE FIREBASE
                                sincronizarEventos(callback);
                            }

                            @Override
                            public void onSyncError(String error) {
                                Log.e(TAG, "❌ Error eliminando: " + error);
                                // Continuar igual
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }
                        });
                    }

                    @Override
                    public void onSyncError(String error) {
                        Log.e(TAG, "❌ Error modificando: " + error);
                        // Continuar igual
                        syncEventosPendientesEliminar(new SyncCallback() {
                            @Override
                            public void onSyncComplete() {
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }

                            @Override
                            public void onSyncError(String error) {
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }
                        });
                    }
                });
            }

            @Override
            public void onSyncError(String error) {
                Log.e(TAG, "❌ Error agregando: " + error);
                // Continuar igual
                syncEventosPendientesModificar(new SyncCallback() {
                    @Override
                    public void onSyncComplete() {
                        syncEventosPendientesEliminar(new SyncCallback() {
                            @Override
                            public void onSyncComplete() {
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }

                            @Override
                            public void onSyncError(String error) {
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }
                        });
                    }

                    @Override
                    public void onSyncError(String error) {
                        syncEventosPendientesEliminar(new SyncCallback() {
                            @Override
                            public void onSyncComplete() {
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }

                            @Override
                            public void onSyncError(String error) {
                                db.limpiarTablaEventos();
                                sincronizarEventos(callback);
                            }
                        });
                    }
                });
            }
        });
    }


    /**
     * Sincronizar SOLO eventos pendientes a modificar (estado = 2) a Firebase
     */
    public void syncEventosPendientesModificar(SyncCallback callback) {

        SharedPreferences prefs = context.getSharedPreferences("eventos_pendientes_modificar", Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        List<Integer> idsPendientes = new ArrayList<>();
        for (String key : all.keySet()) {
            if (key.startsWith("pendiente_") && (boolean) all.get(key)) {
                String idStr = key.replace("pendiente_", "");
                idsPendientes.add(Integer.parseInt(idStr));
            }
        }

        // ✅ Si no hay pendientes, notificar igual
        if (idsPendientes.isEmpty()) {
            Log.d(TAG, "✅ No hay eventos pendientes");
            if (callback != null) {
                callback.onSyncComplete();  // ← IMPORTANTE
            }
            return;
        }

        Log.d(TAG, "📦 " + idsPendientes.size() + " eventos pendientes encontrados");

        final int[] contador = {0};
        final int total = idsPendientes.size();
        final boolean[] hayError = {false};

        for (int idLocal : idsPendientes) {
            String nombreOriginal = prefs.getString("nombre_original_" + idLocal, "");
            String nombreActual = prefs.getString("nombre_actual_" + idLocal, "");

            Eventos evento = db.getEventoById(idLocal);
            if (evento == null) {
                Log.e(TAG, "❌ Evento no encontrado en BD: " + idLocal);
                contador[0]++;

                // ✅ Verificar si es el último
                if (contador[0] == total && callback != null) {
                    callback.onSyncComplete();
                }
                continue;
            }

            evento.setEstado(1);
            final String nombreEvento = evento.getNombre();
            final int finalIdLocal = idLocal;

            Log.d(TAG, "📝 Sincronizando: " + nombreEvento);

            firebaseHelper.modificarEventoPorNombre(nombreOriginal, evento,
                    new FirebaseHelper.FirebaseCallback() {
                        @Override
                        public void onSuccess(String mensaje) {
                            Log.d(TAG, "✅ Evento sincronizado: " + nombreEvento);

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove("pendiente_" + finalIdLocal);
                            editor.remove("nombre_original_" + finalIdLocal);
                            editor.remove("nombre_actual_" + finalIdLocal);
                            editor.apply();

                            contador[0]++;

                            // ✅ Cuando se completan todos
                            if (contador[0] == total && callback != null) {
                                callback.onSyncComplete();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error sincronizando: " + nombreEvento + " - " + error);
                            hayError[0] = true;
                            contador[0]++;

                            if (contador[0] == total && callback != null) {
                                callback.onSyncComplete();  // ← Continuar igual
                            }
                        }
                    });
        }
    }

    /**
     * Sincronizar eventos NUEVOS (estado = 3) a Firebase
     * @param callback Callback para resultado
     */
    public void syncEventosPendientesAgregar(SyncCallback callback) {
        List<Eventos> eventosNuevos = db.getEventosPendientesAgregar();

        if (eventosNuevos.isEmpty()) {
            Log.d(TAG, "✅ No hay eventos nuevos pendientes");
            if (callback != null) callback.onSyncComplete();
            return;
        }

        Log.d(TAG, "📦 " + eventosNuevos.size() + " eventos nuevos encontrados");

        final int[] contador = {0};
        final int total = eventosNuevos.size();
        final boolean[] hayError = {false};

        for (Eventos evento : eventosNuevos) {
            final String nombreEvento = evento.getNombre();
            evento.setEstado(1);

            Log.d(TAG, "📝 Guardando evento nuevo: " + nombreEvento);

            firebaseHelper.guardarEventoEnFirestore(evento, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {
                    Log.d(TAG, "✅ Evento nuevo guardado en Firebase: " + nombreEvento);
                    contador[0]++;

                    // ✅ Verificar si es el último
                    if (contador[0] == total && callback != null) {
                        callback.onSyncComplete();
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error guardando evento nuevo: " + nombreEvento + " - " + error);
                    hayError[0] = true;
                    contador[0]++;

                    // ✅ Continuar igual aunque falle
                    if (contador[0] == total && callback != null) {
                        callback.onSyncComplete();
                    }
                }
            });
        }
    }

    public void syncEventosPendientesEliminar(SyncCallback callback) {
        List<Eventos> eventosEliminar = db.getEventosPendientesEliminar();

        if (eventosEliminar.isEmpty()) {
            Log.d(TAG, "✅ No hay eventos para eliminar");
            if (callback != null) callback.onSyncComplete();
            return;
        }

        Log.d(TAG, "📦 " + eventosEliminar.size() + " eventos para eliminar encontrados");

        final int[] contador = {0};
        final int total = eventosEliminar.size();
        final boolean[] hayError = {false};

        for (Eventos evento : eventosEliminar) {
            final String nombreEvento = evento.getNombre();
            evento.setEstado(0);

            Log.d(TAG, "📝 Eliminando evento: " + nombreEvento);

            firebaseHelper.eliminarEventoPermanentePorNombre(nombreEvento, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {
                    Log.d(TAG, "✅ Evento eliminado en Firebase: " + nombreEvento);
                    contador[0]++;

                    // ✅ Verificar si es el último
                    if (contador[0] == total && callback != null) {
                        callback.onSyncComplete();
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error eliminando evento: " + nombreEvento + " - " + error);
                    hayError[0] = true;
                    contador[0]++;

                    // ✅ Continuar igual aunque falle
                    if (contador[0] == total && callback != null) {
                        callback.onSyncComplete();
                    }
                }
            });
        }
    }


        private void limpiarEventosPasados() {
        firebaseHelper.eliminarEventosPasados(new FirebaseHelper.FirebaseCallback() {
            @Override
            public void onSuccess(String mensaje) {
                Log.d("LIMPIAR", mensaje);
            }

            @Override
            public void onError(String error) {
                Log.e("LIMPIAR", "Error: " + error);
            }
        });
    }


    /**
     * Sincronizar eventos desde Firebase a BD local
     */
    private void sincronizarEventos(SyncCallback callback) {
        firebaseHelper.obtenerEventosFuturos(new FirebaseHelper.FirebaseListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documentos) {
                if (documentos.isEmpty()) {
                    Log.d("SYNC_EVENTOS", "⚠️ No hay eventos para sincronizar");
                    // ✅ Notificar incluso si no hay eventos
                    if (callback != null) callback.onSyncComplete();
                    return;
                }
                Log.d("SYNC_EVENTOS", "📦 " + documentos.size() + " eventos obtenidos");
                procesarEventos(documentos);

                // ✅ Callback después de procesar
                if (callback != null) callback.onSyncComplete();
            }

            @Override
            public void onError(String error) {
                Log.e("SYNC_EVENTOS", "❌ Error: " + error);
                // ✅ Notificar el error
                if (callback != null) callback.onSyncError(error);
            }
        });
    }
    /**
     * Procesar eventos y guardar en BD local
     */
    private void procesarEventos(List<DocumentSnapshot> eventos) {
        if (eventos == null || eventos.isEmpty()) {
            return;
        }

        int guardados = 0;
        int actualizados = 0;

        for (DocumentSnapshot doc : eventos) {
            try {
                // Obtener datos del documento
                String nombre = doc.getString("nombre");
                String descripcion = doc.getString("descripcion");
                String longitud = doc.getString("longitud");
                String latitud = doc.getString("latitud");

                // Obtener fechas (priorizar display, si no usar la ISO)
                String fechaInicio = doc.getString("fecha_inicio_original");

                String fechaFin = doc.getString("fecha_fin_original");

                String horaInicio = doc.getString("hora_inicio");
                String horaFin = doc.getString("hora_fin");
                int estado = doc.getLong("estado") != null ? doc.getLong("estado").intValue() : 1;

                Log.d("SYNC_EVENTOS", "📌 Procesando: " + nombre);
                Log.d("SYNC_EVENTOS", "   Fecha: " + fechaInicio + " " + horaInicio);


                    // Insertar nuevo evento
                    Eventos evento = new Eventos(
                            -1,  // ID local (autoincremental)
                            nombre,
                            descripcion,
                            longitud,
                            latitud,
                            fechaInicio,
                            horaInicio,
                            fechaFin,
                            horaFin,
                            estado
                    );

                    long id = db.insertarEvento(evento);
                    if (id > 0) {
                        guardados++;
                        Log.d("SYNC_EVENTOS", "   Evento guardado: " + nombre);
                    }


            } catch (Exception e) {
                Log.e("SYNC_EVENTOS", "❌ Error procesando evento: " + e.getMessage());
            }
        }

        Log.d("SYNC_EVENTOS", "═══════════════════════════════════════");
        Log.d("SYNC_EVENTOS", "📊 RESUMEN SINCRONIZACIÓN EVENTOS");
        Log.d("SYNC_EVENTOS", "   Nuevos: " + guardados);
        Log.d("SYNC_EVENTOS", "   Actualizados: " + actualizados);
        Log.d("SYNC_EVENTOS", "   Total: " + (guardados + actualizados));
        Log.d("SYNC_EVENTOS", "═══════════════════════════════════════");
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


                    sincronizarEspaciosDelLugarYContinuar(lugarFirebaseId, (int)idLugarLocal, 0, lugares, indice);

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

        // ✅ Procesar secuencialmente con índice
        procesarImagenSecuencial(urls, 0, rutasLocales, callback);
    }

    private void procesarImagenSecuencial(String[] urls,
                                          int index,
                                          List<String> rutasLocales,
                                          ImageProcessCallback callback) {
        if (index >= urls.length) {
            // Todas las imágenes procesadas
            String resultado = String.join(",", rutasLocales);
            callback.onComplete(resultado.isEmpty() ? "" : resultado);
            return;
        }

        String urlActual = urls[index].trim();

        if (urlActual.isEmpty()) {
            rutasLocales.add("");
            procesarImagenSecuencial(urls, index + 1, rutasLocales, callback);
            return;
        }

        Log.d(TAG, "📥 Descargando [" + index + "/" + urls.length + "]: " + urlActual);

        imageStorageManager.descargarYGuardarImagen(urlActual,
                new ImageStorageManager.ImageDownloadCallback() {
                    @Override
                    public void onSuccess(String rutaLocal) {
                        rutasLocales.add(rutaLocal);
                        // ✅ Siguiente imagen (secuencial)
                        procesarImagenSecuencial(urls, index + 1, rutasLocales, callback);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Error en " + urlActual + ", usando URL original");
                        rutasLocales.add(urlActual);
                        // ✅ Continuar con siguiente aunque falle
                        procesarImagenSecuencial(urls, index + 1, rutasLocales, callback);
                    }
                });
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

    // ==================== FUNCIONES PARA MANEJAR PENDIENTES ====================

    /**
     * 1️⃣ Enviar USUARIOS PENDIENTES DE AGREGAR (estado = 3)
     * @param context Contexto de la aplicación
     * @param callback Callback con resultado
     */
    public void sincronizarPendientesAgregar(Context context, SyncManagerCallback callback) {
        Database dbLocal = new Database(context);
        List<Usuarios> pendientesAgregar = dbLocal.getUsuariosPendientesAgregar();

        if (pendientesAgregar.isEmpty()) {
            Log.d("SYNC_PENDIENTES", "📭 No hay usuarios pendientes de agregar");
            if (callback != null) callback.onAgregarCompletado(0);
            return;
        }

        int total = pendientesAgregar.size();
        int exitosos = 0;
        int fallidos = 0;

        Log.d("SYNC_PENDIENTES", "════════════════════════════════════════════");
        Log.d("SYNC_PENDIENTES", "➕ PROCESANDO PENDIENTES DE AGREGAR: " + total);

        for (Usuarios usuario : pendientesAgregar) {
            firebaseHelper.guardarUsuarioEnFirestore(usuario, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {

                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }

    /**
     * 2️⃣ Enviar USUARIOS PENDIENTES DE MODIFICAR (estado = 2)
     * @param context Contexto de la aplicación
     * @param callback Callback con resultado
     */
    public void sincronizarPendientesModificar(Context context, SyncManagerCallback callback) {
        Database dbLocal = new Database(context);
        List<Usuarios> pendientesModificar = dbLocal.getUsuariosPendientesModificar();

        if (pendientesModificar.isEmpty()) {
            Log.d("SYNC_PENDIENTES", "📭 No hay usuarios pendientes de modificar");
            if (callback != null) callback.onModificarCompletado(0);
            return;
        }

        int total = pendientesModificar.size();
        int exitosos = 0;
        int fallidos = 0;

        Log.d("SYNC_PENDIENTES", "════════════════════════════════════════════");
        Log.d("SYNC_PENDIENTES", "✏️ PROCESANDO PENDIENTES DE MODIFICAR: " + total);

        for (Usuarios usuario : pendientesModificar) {
            firebaseHelper.actualizarUsuarioPorCarnet(usuario, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {

                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }

    /**
     * 3️⃣ Enviar USUARIOS PENDIENTES DE ELIMINAR (estado = 4)
     * @param context Contexto de la aplicación
     * @param callback Callback con resultado
     */
    public void sincronizarPendientesEliminar(Context context, SyncManagerCallback callback) {
        Database dbLocal = new Database(context);
        List<Usuarios> pendientesEliminar = dbLocal.getUsuariosPendientesEliminar();

        if (pendientesEliminar.isEmpty()) {
            Log.d("SYNC_PENDIENTES", "📭 No hay usuarios pendientes de eliminar");
            if (callback != null) callback.onEliminarCompletado(0);
            return;
        }

        int total = pendientesEliminar.size();
        int exitosos = 0;
        int fallidos = 0;

        Log.d("SYNC_PENDIENTES", "════════════════════════════════════════════");
        Log.d("SYNC_PENDIENTES", "🗑️ PROCESANDO PENDIENTES DE ELIMINAR: " + total);

        for (Usuarios usuario : pendientesEliminar) {
            firebaseHelper.softDeleteUsuarioPorCarnet(usuario.getCarnet(), new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {

                }

                @Override
                public void onError(String error) {

                }
            });
        }
    }

// ==================== INTERFACES ====================

    public interface SyncManagerCallback {

        void onAgregarCompletado(int cantidad);

        void onModificarCompletado(int cantidad);

        void onEliminarCompletado(int cantidad);
    }

    public interface SyncManagerFinalCallback {

        void onCompletado();
    }


    public void procesarTodosLosPendientes(
            Context context,
            SyncManagerFinalCallback callback
    ) {

        // ==================== AGREGAR ====================

        sincronizarPendientesAgregar(context, new SyncManagerCallback() {

            @Override
            public void onAgregarCompletado(int cantidad) {

                Log.d("SYNC_PENDIENTES",
                        "✅ AGREGAR completado: " + cantidad);

                // ==================== MODIFICAR ====================

                sincronizarPendientesModificar(context, new SyncManagerCallback() {

                    @Override
                    public void onModificarCompletado(int cantidad) {

                        Log.d("SYNC_PENDIENTES",
                                "✅ MODIFICAR completado: " + cantidad);

                        // ==================== ELIMINAR ====================

                        sincronizarPendientesEliminar(context, new SyncManagerCallback() {

                            @Override
                            public void onEliminarCompletado(int cantidad) {

                                Log.d("SYNC_PENDIENTES",
                                        "✅ ELIMINAR completado: " + cantidad);

                                Log.d("SYNC_PENDIENTES",
                                        "🎉 TODOS LOS PENDIENTES SINCRONIZADOS");

                                if (callback != null) {
                                    callback.onCompletado();
                                }
                            }

                            @Override
                            public void onAgregarCompletado(int cantidad) {}

                            @Override
                            public void onModificarCompletado(int cantidad) {}
                        });
                    }

                    @Override
                    public void onAgregarCompletado(int cantidad) {}

                    @Override
                    public void onEliminarCompletado(int cantidad) {}
                });
            }

            @Override
            public void onModificarCompletado(int cantidad) {}

            @Override
            public void onEliminarCompletado(int cantidad) {}
        });
    }


}