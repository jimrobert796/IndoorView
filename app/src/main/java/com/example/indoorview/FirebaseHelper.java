package com.example.indoorview;
import com.example.indoorview.models.Eventos;
import com.example.indoorview.models.Usuarios;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class FirebaseHelper {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CloudinaryHelper cloudinary = new CloudinaryHelper();



    // En FirebaseHelper.java - Nuevo callback para listas
    public interface FirebaseListCallback {
        void onSuccess(List<DocumentSnapshot> documentos);
        void onError(String error);
    }

    public interface FirebaseCallback {
        void onSuccess(String mensaje);
        void onError(String error);
    }

    // ════════════════════════════════════════════════════════════════
    // ✅ GUARDAR LUGAR EN FIRESTORE (ACTUALIZADO CON CALLBACK)
    // ════════════════════════════════════════════════════════════════
    // ✅ NUEVO (recibe callback)
    public void guardarLugarEnFirestore(
            String nombre,
            String descripcion,
            String urlImagenes,
            String color,
            String geojson,
            String lugarId,
            int estado,
            FirebaseCallback callback) {  // ✅ AGREGAR ESTE PARÁMETRO

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> lugar = new HashMap<>();
        lugar.put("nombre", nombre);
        lugar.put("descripcion", descripcion);
        lugar.put("url_imagenes", urlImagenes);  // Corregir typo
        lugar.put("color", color);
        lugar.put("geojson", geojson);
        lugar.put("estado", estado);
        lugar.put("fecha_creacion", FieldValue.serverTimestamp());

        Log.d("FIREBASE_LUGAR", "════════════════════════════════════════════");
        Log.d("FIREBASE_LUGAR", "🔥 Guardando lugar en Firestore");
        Log.d("FIREBASE_LUGAR", "  Nombre: " + nombre);
        Log.d("FIREBASE_LUGAR", "  ID: " + lugarId);
        Log.d("FIREBASE_LUGAR", "════════════════════════════════════════════");

        db.collection("lugares").document(lugarId).set(lugar)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_LUGAR", "✅ ÉXITO: Lugar guardado en Firestore");
                    Log.d("FIREBASE_LUGAR", "  ID: " + lugarId);
                    Log.d("FIREBASE_LUGAR", "  Llamando callback.onSuccess()");

                    // ✅ AQUÍ LLAMA AL CALLBACK
                    if (callback != null) {
                        callback.onSuccess("Lugar guardado en Firebase: " + nombre);
                    } else {
                        Log.e("FIREBASE_LUGAR", "❌ callback es NULL!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_LUGAR", "❌ ERROR: No se pudo guardar el lugar");
                    Log.e("FIREBASE_LUGAR", "  Mensaje: " + e.getMessage());
                    Log.e("FIREBASE_LUGAR", "  Llamando callback.onError()");

                    // ✅ AQUÍ LLAMA AL CALLBACK DE ERROR
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    } else {
                        Log.e("FIREBASE_LUGAR", "❌ callback es NULL!");
                    }
                });
    }




    // Crear piso en firebase o firestore
    public void crearPisoEnFirestore(String lugarId, int numero, String nombrePiso, int activo, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ Usar el NOMBRE sanitizado como ID, no el número
        String pisoId = nombrePiso;  // "primer_piso", "segundo_piso"

        Map<String, Object> piso = new HashMap<>();
        piso.put("numero", numero);
        piso.put("nombre", nombrePiso);
        piso.put("activo", activo);
        piso.put("fecha_creacion", FieldValue.serverTimestamp());

        Log.d("FIREBASE_PISO", "════════════════════════════════════════════");
        Log.d("FIREBASE_PISO", "🔥 Guardando piso en Firestore");
        Log.d("FIREBASE_PISO", "  Lugar ID: " + lugarId);
        Log.d("FIREBASE_PISO", "  Piso ID: " + pisoId);     // ← Ahora será "primer_piso"
        Log.d("FIREBASE_PISO", "  Nombre: " + nombrePiso);   // ← "Primer Piso"
        Log.d("FIREBASE_PISO", "  Número: " + numero);
        Log.d("FIREBASE_PISO", "════════════════════════════════════════════");

        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .document(pisoId)  // ← Usa el nombre sanitizado
                .set(piso)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_PISO", "✅ ÉXITO: Piso guardado en Firestore");
                    Log.d("FIREBASE_PISO", "  Ruta: " + lugarId + "/pisos/" + pisoId);
                    if (callback != null) {
                        callback.onSuccess("Piso creado: " + nombrePiso);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_PISO", "❌ ERROR: No se pudo guardar el piso");
                    Log.e("FIREBASE_PISO", "  Mensaje: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Crear espacio en firestore si es posible
    public void crearEspacioEnFirestore(String lugarId,
                                        String pisoId,
                                        String espacioId,
                                        String nombre,
                                        String descripcion,
                                        String url_imagenes,
                                        int estado,
                                        FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> espacio = new HashMap<>();
        espacio.put("nombre", nombre);
        espacio.put("descripcion", descripcion);
        espacio.put("url_imagenes", url_imagenes);
        espacio.put("estado", estado);
        espacio.put("fecha_creacion", FieldValue.serverTimestamp());

        Log.d("FIREBASE_ESPACIO", "════════════════════════════════════════════");
        Log.d("FIREBASE_ESPACIO", "🔥 Guardando espacio en Firestore");
        Log.d("FIREBASE_ESPACIO", "  Ruta: " + lugarId + "/pisos/" + pisoId + "/espacios/" + espacioId);
        Log.d("FIREBASE_ESPACIO", "  Nombre: " + nombre);
        Log.d("FIREBASE_ESPACIO", "  Descripción: " + descripcion);
        Log.d("FIREBASE_ESPACIO", "  URLs imágenes: " + url_imagenes);
        Log.d("FIREBASE_ESPACIO", "════════════════════════════════════════════");

        // Guardar como subcolección dentro del piso
        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .document(pisoId)
                .collection("espacios")
                .document(espacioId)
                .set(espacio)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_ESPACIO", "✅ ÉXITO: Espacio guardado en Firestore");
                    Log.d("FIREBASE_ESPACIO", "  Ruta: " + lugarId + "/pisos/" + pisoId + "/espacios/" + espacioId);

                    if (callback != null) {
                        callback.onSuccess("Espacio creado: " + nombre);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ESPACIO", "❌ ERROR: No se pudo guardar el espacio");
                    Log.e("FIREBASE_ESPACIO", "  Mensaje: " + e.getMessage());

                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Guardar la geometeria
    /**
     * Guardar geometría de un espacio en Firestore
     * @param lugarId ID del lugar (ej: "edificio_ingenieria")
     * @param pisoId ID del piso (ej: "piso_1")
     * @param espacioId ID del espacio (ej: "aula_101")
     * @param vertices GeoJSON de los vértices del polígono
     * @param color Color hexadecimal del espacio
     * @param callback Callback para notificar resultado
     */
    public void guardarGeometriaEspacio(String lugarId,
                                        String pisoId,
                                        String espacioId,
                                        String vertices,
                                        String color,
                                        FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> geometria = new HashMap<>();
        geometria.put("tipo", "Polygon"); // Tipo poligono por defecto
        geometria.put("vertices", vertices);
        geometria.put("color", color);
        geometria.put("fecha_creacion", FieldValue.serverTimestamp());

        Log.d("FIREBASE_GEOMETRIA", "════════════════════════════════════════════");
        Log.d("FIREBASE_GEOMETRIA", "🔥 Guardando geometría en Firestore");
        Log.d("FIREBASE_GEOMETRIA", "  Ruta: " + lugarId + "/pisos/" + pisoId + "/espacios/" + espacioId + "/geometria/vertices");
        Log.d("FIREBASE_GEOMETRIA", "  Espacio ID: " + espacioId);
        Log.d("FIREBASE_GEOMETRIA", "  Color: " + color);
        Log.d("FIREBASE_GEOMETRIA", "  Vertices: " + (vertices != null ? vertices.substring(0, Math.min(100, vertices.length())) + "..." : "null"));
        Log.d("FIREBASE_GEOMETRIA", "════════════════════════════════════════════");

        // Guardar geometría como subcolección del espacio
        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .document(pisoId)
                .collection("espacios")
                .document(espacioId)
                .collection("geometria")
                .document("vertices")
                .set(geometria)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_GEOMETRIA", "✅ ÉXITO: Geometría guardada");
                    Log.d("FIREBASE_GEOMETRIA", "  Ruta: " + lugarId + "/pisos/" + pisoId + "/espacios/" + espacioId);

                    if (callback != null) {
                        callback.onSuccess("Geometría guardada para: " + espacioId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_GEOMETRIA", "❌ ERROR: No se pudo guardar la geometría");
                    Log.e("FIREBASE_GEOMETRIA", "  Mensaje: " + e.getMessage());

                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }



    // Esto nos ayudara ya que los ids no son numeros sino que son como tal TEXTO PLANO
    public void actualizarLugarPorNombre(String nombreActual,  // Nombre actual del lugar
                                         String nuevoNombre,
                                         String nuevaDescripcion,
                                         String nuevasUrlsImagenes,
                                         String nuevoColor,
                                         FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // PRIMERO: Buscar el documento por su campo "nombre"
        db.collection("lugares")
                .whereEqualTo("nombre", nombreActual)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("FIREBASE_UPDATE", "❌ No se encontró el lugar: " + nombreActual);
                        if (callback != null) {
                            callback.onError("No se encontró el lugar: " + nombreActual);
                        }
                        return;
                    }

                    // Obtener el ID real del documento
                    String lugarId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    Log.d("FIREBASE_UPDATE", "📌 Lugar encontrado - ID: " + lugarId);

                    // SEGUNDO: Actualizar el documento encontrado
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("nombre", nuevoNombre);
                    updates.put("descripcion", nuevaDescripcion);

                    if (nuevasUrlsImagenes != null && !nuevasUrlsImagenes.isEmpty()) {
                        // Verificar si las URLs son de Cloudinary (ya están bien)
                        boolean sonCloudinary = !nuevasUrlsImagenes.contains("storage");

                        if (sonCloudinary) {
                            updates.put("url_imagenes", nuevasUrlsImagenes);
                            Log.d("FIREBASE_UPDATE", "  🖼️ Actualizando URLs de Cloudinary");
                        } else {
                            Log.d("FIREBASE_UPDATE", "  ⚠️ URLs locales detectadas, NO se actualizan en Firebase");
                        }
                    }

                    updates.put("color", nuevoColor);
                    updates.put("fecha_actualizacion", FieldValue.serverTimestamp());

                    db.collection("lugares").document(lugarId).update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FIREBASE_UPDATE", "✅ Lugar actualizado en Firebase");
                                Log.d("FIREBASE_UPDATE", "  De: " + nombreActual + " → A: " + nuevoNombre);
                                if (callback != null) {
                                    callback.onSuccess("Lugar actualizado: " + nuevoNombre);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FIREBASE_UPDATE", "❌ Error al actualizar: " + e.getMessage());
                                if (callback != null) {
                                    callback.onError(e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_UPDATE", "❌ Error al buscar: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Error al buscar lugar: " + e.getMessage());
                    }
                });
    }

    // EN FirebaseHelper.java - BÚSQUEDA MANUAL (NO necesita índice) es tardao pero es lo mejor que se podria de hacer
    // o eso es lo que yo supongo
    public void actualizarEspacioPorNombre(String nombreActual,
                                           String nuevoNombre,
                                           String nuevaDescripcion,
                                           String nuevasUrlsImagenes,
                                           FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Obtener TODOS los lugares
        db.collection("lugares").get().addOnSuccessListener(lugaresSnapshot -> {
            for (DocumentSnapshot lugar : lugaresSnapshot.getDocuments()) {
                String lugarId = lugar.getId();

                // 2. Por cada lugar, obtener TODOS sus pisos
                db.collection("lugares").document(lugarId).collection("pisos").get()
                        .addOnSuccessListener(pisosSnapshot -> {
                            for (DocumentSnapshot piso : pisosSnapshot.getDocuments()) {
                                String pisoId = piso.getId();

                                // 3. Buscar el espacio por su NOMBRE en este piso específico
                                db.collection("lugares")
                                        .document(lugarId)
                                        .collection("pisos")
                                        .document(pisoId)
                                        .collection("espacios")
                                        .whereEqualTo("nombre", nombreActual)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(espacios -> {
                                            if (!espacios.isEmpty()) {
                                                // ¡Espacio encontrado!
                                                DocumentSnapshot espacioDoc = espacios.getDocuments().get(0);
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("nombre", nuevoNombre);
                                                updates.put("descripcion", nuevaDescripcion);
                                                if (nuevasUrlsImagenes != null && !nuevasUrlsImagenes.isEmpty()) {
                                                    // Verificar si las URLs son de Cloudinary (ya están bien)
                                                    boolean sonCloudinary = !nuevasUrlsImagenes.contains("storage");

                                                    if (sonCloudinary) {
                                                        updates.put("url_imagenes", nuevasUrlsImagenes);
                                                        Log.d("FIREBASE_UPDATE", "  🖼️ Actualizando URLs de Cloudinary");
                                                    } else {
                                                        Log.d("FIREBASE_UPDATE", "  ⚠️ URLs locales detectadas, NO se actualizan en Firebase");
                                                    }
                                                }
                                                updates.put("fecha_actualizacion", FieldValue.serverTimestamp());

                                                espacioDoc.getReference().update(updates)
                                                        .addOnSuccessListener(aVoid -> {
                                                            if (callback != null)
                                                                callback.onSuccess("Espacio actualizado: " + nuevoNombre);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            if (callback != null) callback.onError(e.getMessage());
                                                        });
                                                return; // Salir del bucle
                                            }
                                        });
                            }
                        });
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Error obteniendo lugares: " + e.getMessage());
        });
    }

    /**
     * Actualiza SOLO el color de la geometría buscando el espacio por su nombre
     * @param nombreEspacio Nombre actual del espacio
     * @param nuevoColor Nuevo color en formato hexadecimal (ej: "#2196F3")
     * @param callback Callback para resultado
     */
    public void actualizarColorGeometriaPorNombre(String nombreEspacio,
                                                  String nuevoColor,
                                                  FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Buscar manualmente sin usar collectionGroup
        db.collection("lugares").get().addOnSuccessListener(lugaresSnapshot -> {

            final boolean[] encontrado = {false};
            final int[] totalPisos = {0};
            final int[] pisosProcesados = {0};

            for (DocumentSnapshot lugar : lugaresSnapshot.getDocuments()) {
                String lugarId = lugar.getId();

                // Obtener todos los pisos de este lugar
                db.collection("lugares").document(lugarId).collection("pisos").get()
                        .addOnSuccessListener(pisosSnapshot -> {
                            totalPisos[0] += pisosSnapshot.size();

                            for (DocumentSnapshot piso : pisosSnapshot.getDocuments()) {
                                String pisoId = piso.getId();

                                // Buscar el espacio por nombre en este piso
                                db.collection("lugares")
                                        .document(lugarId)
                                        .collection("pisos")
                                        .document(pisoId)
                                        .collection("espacios")
                                        .whereEqualTo("nombre", nombreEspacio)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(espacios -> {
                                            pisosProcesados[0]++;

                                            if (!espacios.isEmpty() && !encontrado[0]) {
                                                encontrado[0] = true;

                                                DocumentSnapshot espacioDoc = espacios.getDocuments().get(0);
                                                String espacioId = espacioDoc.getId();

                                                Log.d("FIREBASE_GEOMETRIA", "📌 Espacio encontrado:");
                                                Log.d("FIREBASE_GEOMETRIA", "  Lugar: " + lugarId);
                                                Log.d("FIREBASE_GEOMETRIA", "  Piso: " + pisoId);
                                                Log.d("FIREBASE_GEOMETRIA", "  Espacio: " + espacioId);

                                                // Actualizar el color
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("color", nuevoColor);
                                                updates.put("fecha_actualizacion", FieldValue.serverTimestamp());

                                                db.collection("lugares")
                                                        .document(lugarId)
                                                        .collection("pisos")
                                                        .document(pisoId)
                                                        .collection("espacios")
                                                        .document(espacioId)
                                                        .collection("geometria")
                                                        .document("vertices")
                                                        .update(updates)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Log.d("FIREBASE_GEOMETRIA", "✅ Color actualizado a: " + nuevoColor);
                                                            if (callback != null) {
                                                                callback.onSuccess("Color actualizado para: " + nombreEspacio);
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            if (callback != null) callback.onError(e.getMessage());
                                                        });
                                            }

                                            // Si ya procesamos todos los pisos y no se encontró
                                            if (pisosProcesados[0] == totalPisos[0] && !encontrado[0]) {
                                                Log.e("FIREBASE_GEOMETRIA", "❌ No se encontró el espacio: " + nombreEspacio);
                                                if (callback != null) {
                                                    callback.onError("No se encontró el espacio: " + nombreEspacio);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            pisosProcesados[0]++;
                                            if (pisosProcesados[0] == totalPisos[0] && !encontrado[0]) {
                                                if (callback != null) callback.onError(e.getMessage());
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (!encontrado[0] && callback != null) {
                                callback.onError("Error obteniendo pisos: " + e.getMessage());
                            }
                        });
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Error obteniendo lugares: " + e.getMessage());
        });
    }


    // ELIMINAR

    public void softDeleteLugar(String lugarId, int nuevoEstado, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", nuevoEstado);
        updates.put("fecha_actualizacion", FieldValue.serverTimestamp());

        Log.d("FIREBASE_ESTADO", "Soft dellete estado del lugar: " + lugarId);
        Log.d("FIREBASE_ESTADO", "  Nuevo estado: " + (nuevoEstado == 1 ? "ACTIVO" : "INACTIVO"));

        db.collection("lugares").document(lugarId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_ESTADO", "✅ Estado actualizado: " + lugarId);
                    if (callback != null) {
                        callback.onSuccess("Lugar " + (nuevoEstado == 1 ? "activado" : "desactivado"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ESTADO", "❌ Error actualizando estado: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void softDeleteEspacioPorNombre(String nombreEspacio,
                                           int nuevoEstado,
                                           FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Obtener todos los lugares
        db.collection("lugares").get().addOnSuccessListener(lugaresSnapshot -> {

            final boolean[] encontrado = {false};

            // Recorrer cada lugar
            for (DocumentSnapshot lugar : lugaresSnapshot.getDocuments()) {
                if (encontrado[0]) break;

                String lugarId = lugar.getId();

                // Obtener pisos de este lugar
                db.collection("lugares").document(lugarId).collection("pisos").get()
                        .addOnSuccessListener(pisosSnapshot -> {

                            // Recorrer cada piso
                            for (DocumentSnapshot piso : pisosSnapshot.getDocuments()) {
                                if (encontrado[0]) break;

                                String pisoId = piso.getId();

                                // Buscar el espacio por nombre
                                db.collection("lugares")
                                        .document(lugarId)
                                        .collection("pisos")
                                        .document(pisoId)
                                        .collection("espacios")
                                        .whereEqualTo("nombre", nombreEspacio)
                                        .get()  // ← Sin limit, pero solo tomamos el primero
                                        .addOnSuccessListener(espacios -> {

                                            if (!espacios.isEmpty() && !encontrado[0]) {
                                                encontrado[0] = true;

                                                DocumentSnapshot espacioDoc = espacios.getDocuments().get(0);

                                                Log.d("FIREBASE_ESTADO", "📌 Espacio encontrado:");
                                                Log.d("FIREBASE_ESTADO", "  Lugar: " + lugarId);
                                                Log.d("FIREBASE_ESTADO", "  Piso: " + pisoId);
                                                Log.d("FIREBASE_ESTADO", "  Espacio: " + espacioDoc.getId());

                                                // Actualizar estado
                                                espacioDoc.getReference().update(
                                                        "estado", nuevoEstado,
                                                        "fecha_actualizacion", FieldValue.serverTimestamp()
                                                ).addOnSuccessListener(aVoid -> {
                                                    Log.d("FIREBASE_ESTADO", "✅ Estado actualizado: " + nombreEspacio);
                                                    if (callback != null) {
                                                        callback.onSuccess("Espacio " + (nuevoEstado == 1 ? "activado" : "desactivado"));
                                                    }
                                                }).addOnFailureListener(e -> {
                                                    if (callback != null) callback.onError(e.getMessage());
                                                });
                                            }
                                        }).addOnFailureListener(e -> {
                                            if (!encontrado[0] && callback != null) {
                                                callback.onError(e.getMessage());
                                            }
                                        });
                            }
                        }).addOnFailureListener(e -> {
                            if (!encontrado[0] && callback != null) {
                                callback.onError("Error obteniendo pisos: " + e.getMessage());
                            }
                        });
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Error obteniendo lugares: " + e.getMessage());
        });
    }

    // Obtencion de datos en Firebase

    // En FirebaseHelper.java

    // Método que devuelve lista de lugares
    public void obtenerTodosLugares(FirebaseListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ Filtrar solo lugares con estado = 1 (activos)
        db.collection("lugares")
                .whereEqualTo("estado", 1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> lugares = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        lugares.add(doc);
                    }
                    Log.d("FIREBASE", "✅ " + lugares.size() + " lugares activos obtenidos");
                    callback.onSuccess(lugares);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "❌ Error obteniendo lugares: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // Método que devuelve lista de pisos
    public void obtenerPisosDeLugar(String lugarId, FirebaseListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .get()
                .addOnSuccessListener(query -> {
                    List<DocumentSnapshot> pisos = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        pisos.add(doc);
                    }
                    callback.onSuccess(pisos);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Método que devuelve lista de espacios
    public void obtenerEspaciosDePiso(String lugarId, String pisoId, FirebaseListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .document(pisoId)
                .collection("espacios")
                .whereEqualTo("estado", 1)  // ✅ Solo espacios activos
                .get()
                .addOnSuccessListener(query -> {
                    List<DocumentSnapshot> espacios = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        espacios.add(doc);
                    }
                    Log.d("FIREBASE", "✅ " + espacios.size() + " espacios activos obtenidos");
                    callback.onSuccess(espacios);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "❌ Error obteniendo espacios: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Obtener geometrías de un espacio específico
     */
    public void obtenerGeometriasDeEspacio(String lugarId,
                                           String pisoId,
                                           String espacioId,
                                           FirebaseListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .document(pisoId)
                .collection("espacios")
                .document(espacioId)
                .collection("geometria")
                .get()
                .addOnSuccessListener(query -> {
                    List<DocumentSnapshot> geometrias = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        geometrias.add(doc);
                    }
                    callback.onSuccess(geometrias);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }



    private void cargarLugarCompleto(String lugarId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Obtener datos del lugar
        db.collection("lugares").document(lugarId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nombre = document.getString("nombre");
                        Log.d("FIREBASE", "📦 Lugar: " + nombre);

                        // 2. Cargar pisos del lugar
                        db.collection("lugares")
                                .document(lugarId)
                                .collection("pisos")
                                .get()
                                .addOnSuccessListener(pisos -> {
                                    for (QueryDocumentSnapshot pisoDoc : pisos) {
                                        String pisoId = pisoDoc.getId();
                                        String pisoNombre = pisoDoc.getString("nombre");
                                        Log.d("FIREBASE", "  📌 Piso: " + pisoNombre);

                                        // 3. Cargar espacios del piso
                                        pisoDoc.getReference()
                                                .collection("espacios")
                                                .get()
                                                .addOnSuccessListener(espacios -> {
                                                    for (QueryDocumentSnapshot espacioDoc : espacios) {
                                                        String espacioNombre = espacioDoc.getString("nombre");
                                                        Log.d("FIREBASE", "    📍 Espacio: " + espacioNombre);

                                                        // 4. Cargar geometría del espacio
                                                        espacioDoc.getReference()
                                                                .collection("geometria")
                                                                .document("vertices")
                                                                .get()
                                                                .addOnSuccessListener(geoDoc -> {
                                                                    String vertices = geoDoc.getString("vertices");
                                                                    Log.d("FIREBASE", "      📐 Vertices: " + vertices);
                                                                });
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private void modificarEspacio(String lugarId, String pisoId, String espacioId,
                                  String nuevoNombre, String nuevaDescripcion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ Actualización directa usando los IDs legibles
        db.collection("lugares")
                .document(lugarId)
                .collection("pisos")
                .document(pisoId)
                .collection("espacios")
                .document(espacioId)
                .update("nombre", nuevoNombre, "descripcion", nuevaDescripcion)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE", "✅ Espacio actualizado: " + espacioId);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // GUARDAR PISO
    // ════════════════════════════════════════════════════════════════
    public void guardarPiso(
            long idPiso,
            long idLugar,
            int numero,
            String nombre,
            FirebaseCallback callback) {

        Map<String, Object> pisoData = new HashMap<>();
        pisoData.put("id_piso", idPiso);
        pisoData.put("id_lugar", idLugar);
        pisoData.put("numero", numero);
        pisoData.put("nombre", nombre);
        pisoData.put("activo", 1);
        pisoData.put("fecha_creacion", System.currentTimeMillis());

        db.collection("pisos").document(String.valueOf(idPiso)).set(pisoData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_PISO", "✓ Piso guardado: " + nombre);
                    callback.onSuccess("Piso guardado en Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_PISO", "✗ Error: " + e.getMessage());
                    callback.onError("Error al guardar piso: " + e.getMessage());
                });
    }

    // ════════════════════════════════════════════════════════════════
    // GUARDAR ESPACIO CON IMAGEN
    // ════════════════════════════════════════════════════════════════
    public void guardarEspacioConImagen(
            java.io.File archivoImagen,
            long idEspacio,
            long idLugar,
            long idPiso,
            String nombre,
            String descripcion,
            FirebaseCallback callback) {

        if (archivoImagen == null) {
            guardarEspacioEnFirebase(idEspacio, idLugar, idPiso, nombre, descripcion, "", callback);
            return;
        }

        cloudinary.subirImagen(archivoImagen, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onResult(boolean success, String url, String publicId) {
                if (success) {
                    guardarEspacioEnFirebase(idEspacio, idLugar, idPiso, nombre, descripcion, url, callback);
                } else {
                    callback.onError("Error al subir imagen a Cloudinary");
                }
            }
        });
    }

    // Guardar espacio en Firebase (colección: espacios)
    private void guardarEspacioEnFirebase(
            long idEspacio,
            long idLugar,
            long idPiso,
            String nombre,
            String descripcion,
            String urlImagen,
            FirebaseCallback callback) {

        Map<String, Object> espacioData = new HashMap<>();
        espacioData.put("id_espacio", idEspacio);
        espacioData.put("id_lugar", idLugar);
        espacioData.put("id_piso", idPiso);
        espacioData.put("nombre", nombre);
        espacioData.put("descripcion", descripcion);
        espacioData.put("url_imagenes", urlImagen);
        espacioData.put("estado", 1);
        espacioData.put("fecha_creacion", System.currentTimeMillis());

        db.collection("espacios").document(String.valueOf(idEspacio)).set(espacioData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_ESPACIO", "✓ Espacio guardado: " + nombre);
                    callback.onSuccess("Espacio guardado en Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ESPACIO", "✗ Error: " + e.getMessage());
                    callback.onError("Error al guardar espacio: " + e.getMessage());
                });
    }

    // ════════════════════════════════════════════════════════════════
    // GUARDAR GEOMETRÍA
    // ════════════════════════════════════════════════════════════════
    public void guardarGeometria(
            long idGeometria,
            long idEspacio,
            long idLugar,
            long idPiso,
            String tipo,
            String vertices,
            String color,
            FirebaseCallback callback) {

        Map<String, Object> geometriaData = new HashMap<>();
        geometriaData.put("id_geometria", idGeometria);
        geometriaData.put("id_espacio", idEspacio);
        geometriaData.put("id_lugar", idLugar);
        geometriaData.put("id_piso", idPiso);
        geometriaData.put("tipo", tipo);
        geometriaData.put("vertices", vertices);
        geometriaData.put("color", color);
        geometriaData.put("fecha_creacion", System.currentTimeMillis());

        db.collection("geometria").document(String.valueOf(idGeometria)).set(geometriaData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_GEOMETRIA", "✓ Geometría guardada: " + tipo);
                    callback.onSuccess("Geometría guardada en Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_GEOMETRIA", "✗ Error: " + e.getMessage());
                    callback.onError("Error al guardar geometría: " + e.getMessage());
                });
    }

    // ════════════════════════════════════════════════════════════════
    // OBTENER LUGARES
    // ════════════════════════════════════════════════════════════════
    public void obtenerLugares(OnLugaresObtendidosListener listener) {
        db.collection("lugares")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_GET", "Error: " + error.getMessage());
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> lugares = new java.util.ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            lugares.add(data);
                        }
                    }
                    listener.onSuccess(lugares);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // OBTENER PISOS POR LUGAR
    // ════════════════════════════════════════════════════════════════
    public void obtenerPisosPorLugar(long idLugar, OnPisosObtendidosListener listener) {
        db.collection("pisos")
                .whereEqualTo("id_lugar", idLugar)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_GET", "Error: " + error.getMessage());
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> pisos = new java.util.ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            pisos.add(data);
                        }
                    }
                    listener.onSuccess(pisos);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // OBTENER ESPACIOS POR LUGAR
    // ════════════════════════════════════════════════════════════════
    public void obtenerEspaciosPorLugar(long idLugar, OnEspaciosObtendidosListener listener) {
        db.collection("espacios")
                .whereEqualTo("id_lugar", idLugar)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_GET", "Error: " + error.getMessage());
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> espacios = new java.util.ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            espacios.add(data);
                        }
                    }
                    listener.onSuccess(espacios);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // OBTENER ESPACIOS POR PISO
    // ════════════════════════════════════════════════════════════════
    public void obtenerEspaciosPorPiso(long idPiso, OnEspaciosObtendidosListener listener) {
        db.collection("espacios")
                .whereEqualTo("id_piso", idPiso)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_GET", "Error: " + error.getMessage());
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> espacios = new java.util.ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            espacios.add(data);
                        }
                    }
                    listener.onSuccess(espacios);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // OBTENER GEOMETRÍA POR ESPACIO
    // ════════════════════════════════════════════════════════════════
    public void obtenerGeometriaPorEspacio(long idEspacio, OnGeometriaObtendidaListener listener) {
        db.collection("geometria")
                .whereEqualTo("id_espacio", idEspacio)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_GET", "Error: " + error.getMessage());
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> geometrias = new java.util.ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            geometrias.add(data);
                        }
                    }
                    listener.onSuccess(geometrias);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // OBTENER GEOMETRÍA POR LUGAR
    // ════════════════════════════════════════════════════════════════
    public void obtenerGeometriaPorLugar(long idLugar, OnGeometriaObtendidaListener listener) {
        db.collection("geometria")
                .whereEqualTo("id_lugar", idLugar)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_GET", "Error: " + error.getMessage());
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> geometrias = new java.util.ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            geometrias.add(data);
                        }
                    }
                    listener.onSuccess(geometrias);
                });
    }

    // ════════════════════════════════════════════════════════════════
    // ACTUALIZAR LUGAR
    // ════════════════════════════════════════════════════════════════
    public void actualizarLugar(
            long idLugar,
            java.io.File archivoImagen,
            String nombre,
            String descripcion,
            String color,
            FirebaseCallback callback) {

        if (archivoImagen == null) {
            actualizarLugarEnFirebase(idLugar, nombre, descripcion, "", color, callback);
            return;
        }

        cloudinary.subirImagen(archivoImagen, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onResult(boolean success, String url, String publicId) {
                if (success) {
                    actualizarLugarEnFirebase(idLugar, nombre, descripcion, url, color, callback);
                } else {
                    callback.onError("Error al subir imagen");
                }
            }
        });
    }

    private void actualizarLugarEnFirebase(
            long idLugar,
            String nombre,
            String descripcion,
            String urlImagen,
            String color,
            FirebaseCallback callback) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("descripcion", descripcion);
        updates.put("color", color);
        updates.put("fecha_actualizacion", System.currentTimeMillis());

        if (!urlImagen.isEmpty()) {
            updates.put("url_imagenes", urlImagen);
        }

        db.collection("lugares").document(String.valueOf(idLugar)).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_LUGAR", "✓ Lugar actualizado");
                    callback.onSuccess("Lugar actualizado en Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_LUGAR", "✗ Error: " + e.getMessage());
                    callback.onError("Error al actualizar: " + e.getMessage());
                });
    }

    // ════════════════════════════════════════════════════════════════
    // ACTUALIZAR ESPACIO
    // ════════════════════════════════════════════════════════════════
    public void actualizarEspacio(
            long idEspacio,
            java.io.File archivoImagen,
            String nombre,
            String descripcion,
            FirebaseCallback callback) {

        if (archivoImagen == null) {
            actualizarEspacioEnFirebase(idEspacio, nombre, descripcion, "", callback);
            return;
        }

        cloudinary.subirImagen(archivoImagen, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onResult(boolean success, String url, String publicId) {
                if (success) {
                    actualizarEspacioEnFirebase(idEspacio, nombre, descripcion, url, callback);
                } else {
                    callback.onError("Error al subir imagen");
                }
            }
        });
    }

    private void actualizarEspacioEnFirebase(
            long idEspacio,
            String nombre,
            String descripcion,
            String urlImagen,
            FirebaseCallback callback) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("descripcion", descripcion);
        updates.put("fecha_actualizacion", System.currentTimeMillis());

        if (!urlImagen.isEmpty()) {
            updates.put("url_imagenes", urlImagen);
        }

        db.collection("espacios").document(String.valueOf(idEspacio)).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_ESPACIO", "✓ Espacio actualizado");
                    callback.onSuccess("Espacio actualizado en Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ESPACIO", "✗ Error: " + e.getMessage());
                    callback.onError("Error al actualizar: " + e.getMessage());
                });
    }

    // ════════════════════════════════════════════════════════════════
    // ELIMINAR LUGAR
    // ════════════════════════════════════════════════════════════════
    public void eliminarLugar(long idLugar, FirebaseCallback callback) {
        db.collection("lugares").document(String.valueOf(idLugar)).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_LUGAR", "✓ Lugar eliminado");
                    callback.onSuccess("Lugar eliminado de Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_LUGAR", "✗ Error: " + e.getMessage());
                    callback.onError("Error al eliminar: " + e.getMessage());
                });
    }

    // ════════════════════════════════════════════════════════════════
    // ELIMINAR ESPACIO
    // ════════════════════════════════════════════════════════════════
    public void eliminarEspacio(long idEspacio, FirebaseCallback callback) {
        db.collection("espacios").document(String.valueOf(idEspacio)).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_ESPACIO", "✓ Espacio eliminado");
                    callback.onSuccess("Espacio eliminado de Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ESPACIO", "✗ Error: " + e.getMessage());
                    callback.onError("Error al eliminar: " + e.getMessage());
                });
    }


    /**
     * Guardar un evento en Firestore
     */
    // Dejar que Firebase genere el ID automáticamente
    public void guardarEventoEnFirestore(Eventos evento, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Convertir fecha de dd/MM/yyyy a yyyy-MM-dd para Firebase
        String fechaInicioISO = convertirFechaISO(evento.getFecha_inicio());
        String fechaFinISO = convertirFechaISO(evento.getFecha_fin());

        Map<String, Object> eventoMap = new HashMap<>();
        eventoMap.put("nombre", evento.getNombre());
        eventoMap.put("descripcion", evento.getDescripcion());
        eventoMap.put("longitud", evento.getLongitud());
        eventoMap.put("latitud", evento.getLatitud());
        eventoMap.put("fecha_inicio", fechaInicioISO);  // Guardar en ISO
        eventoMap.put("fecha_inicio_original", evento.getFecha_inicio()); // Guardar original para mostrar
        eventoMap.put("hora_inicio", evento.getHora_inicio());
        eventoMap.put("fecha_fin", fechaFinISO);
        eventoMap.put("fecha_fin_original", evento.getFecha_fin());
        eventoMap.put("hora_fin", evento.getHora_fin());
        eventoMap.put("estado", evento.getEstado());
        eventoMap.put("fecha_creacion", FieldValue.serverTimestamp());

        String id_firebase = Utilidades.generarIdEvento(evento.getNombre());

        db.collection("eventos").document(id_firebase).set(eventoMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_EVENTO", "✅ Evento guardado con ID: " + id_firebase);
                    if (callback != null) {
                        callback.onSuccess("Evento guardado con ID: " + id_firebase);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    // Método auxiliar para convertir fecha
    private String convertirFechaISO(String fechaDMY) {
        try {
            SimpleDateFormat sdfDMY = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdfDMY.parse(fechaDMY);
            return sdfISO.format(date);
        } catch (ParseException e) {
            return fechaDMY;
        }
    }

    /**
     * Modificar un evento existente en Firestore buscándolo por nombre
     * @param nombreActual Nombre actual del evento (para buscarlo)
     * @param evento Evento con los datos actualizados
     * @param callback Callback para resultado
     */
    public void modificarEventoPorNombre(String nombreActual, Eventos evento, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        // Convertir fecha de dd/MM/yyyy a yyyy-MM-dd para Firebase
        String fechaInicioISO = convertirFechaISO(evento.getFecha_inicio());
        String fechaFinISO = convertirFechaISO(evento.getFecha_fin());


        // PRIMERO: Buscar el evento por su nombre
        db.collection("eventos")
                .whereEqualTo("nombre", nombreActual)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("FIREBASE_EVENTO", "❌ No se encontró el evento: " + nombreActual);
                        if (callback != null) {
                            callback.onError("No se encontró el evento: " + nombreActual);
                        }
                        return;
                    }

                    // Obtener el ID real del documento
                    String idFirebase = queryDocumentSnapshots.getDocuments().get(0).getId();
                    Log.d("FIREBASE_EVENTO", "📌 Evento encontrado - ID: " + idFirebase);

                    // SEGUNDO: Actualizar el documento
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("nombre", evento.getNombre());
                    updates.put("descripcion", evento.getDescripcion());
                    updates.put("longitud", evento.getLongitud());
                    updates.put("latitud", evento.getLatitud());
                    updates.put("fecha_inicio", fechaInicioISO);  // Guardar en ISO
                    updates.put("fecha_inicio_original", evento.getFecha_inicio()); // Guardar original para mostrar
                    updates.put("hora_inicio", evento.getHora_inicio());
                    updates.put("fecha_fin", fechaFinISO);
                    updates.put("fecha_fin_original", evento.getFecha_fin());
                    updates.put("hora_fin", evento.getHora_fin());
                    updates.put("estado", evento.getEstado());
                    updates.put("fecha_modificacion", FieldValue.serverTimestamp());

                    db.collection("eventos").document(idFirebase).update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FIREBASE_EVENTO", "✅ Evento modificado: " + evento.getNombre());
                                if (callback != null) {
                                    callback.onSuccess("Evento modificado: " + evento.getNombre());
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FIREBASE_EVENTO", "❌ Error: " + e.getMessage());
                                if (callback != null) callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_EVENTO", "❌ Error al buscar: " + e.getMessage());
                    if (callback != null) callback.onError("Error al buscar evento: " + e.getMessage());
                });
    }

    /**
     * Soft delete de evento buscándolo por nombre
     * @param nombreEvento Nombre del evento a eliminar
     * @param callback Callback para resultado
     */
    /**
     * Eliminar permanentemente un evento de Firestore (borrado físico)
     * @param nombreEvento Nombre del evento a eliminar
     * @param callback Callback para resultado
     */
    public void eliminarEventoPermanentePorNombre(String nombreEvento, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Buscar el evento por su nombre
        db.collection("eventos")
                .whereEqualTo("nombre", nombreEvento)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("FIREBASE_EVENTO", "❌ No se encontró el evento: " + nombreEvento);
                        if (callback != null) {
                            callback.onError("No se encontró el evento: " + nombreEvento);
                        }
                        return;
                    }

                    // Obtener el ID del documento
                    String idFirebase = queryDocumentSnapshots.getDocuments().get(0).getId();
                    Log.d("FIREBASE_EVENTO", "📌 Evento encontrado - ID: " + idFirebase);
                    Log.d("FIREBASE_EVENTO", "  Nombre: " + nombreEvento);

                    // ELIMINAR PERMANENTEMENTE
                    db.collection("eventos").document(idFirebase).delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FIREBASE_EVENTO", "✅ Evento ELIMINADO PERMANENTEMENTE: " + nombreEvento);
                                if (callback != null) {
                                    callback.onSuccess("Evento eliminado permanentemente: " + nombreEvento);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FIREBASE_EVENTO", "❌ Error eliminando: " + e.getMessage());
                                if (callback != null) callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_EVENTO", "❌ Error al buscar: " + e.getMessage());
                    if (callback != null) callback.onError("Error al buscar evento: " + e.getMessage());
                });
    }


    // LO ARREGLAREMOS MAÑANA MISMO POR QUE NECESITAMOS SINCRONIZAR CORRECTAMENTE POR FECHA

    /**
     * Obtener eventos desde Firestore con fecha de inicio >= fecha actual
     * @param callback Callback con la lista de eventos
     */
    public void obtenerEventosFuturos(FirebaseListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String fechaActualISO = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // PASO 1: Filtrar solo por estado (usa índice existente)
        db.collection("eventos")
                .whereEqualTo("estado", 1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> eventosActivos = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String fechaFin = doc.getString("fecha_fin");
                        String fechaInicio = doc.getString("fecha_inicio");

                        // ✅ Condición CORREGIDA: evento activo si fecha_fin >= fecha actual
                        // No importa si empezó antes, mientras no haya terminado
                        if (fechaFin != null && fechaFin.compareTo(fechaActualISO) >= 0) {
                            eventosActivos.add(doc);
                            Log.d("FIREBASE_EVENTO", "  📌 Activo: " + doc.getString("nombre") +
                                    " (fecha_fin: " + fechaFin + ")");
                        } else if (fechaFin == null) {
                            // Si no tiene fecha_fin, usar fecha_inicio como fallback
                            if (fechaInicio != null && fechaInicio.compareTo(fechaActualISO) >= 0) {
                                eventosActivos.add(doc);
                            }
                        }
                    }

                    // Ordenar por fecha_inicio (próximos primero)
                    eventosActivos.sort((a, b) -> {
                        String fechaA = a.getString("fecha_inicio");
                        String fechaB = b.getString("fecha_inicio");
                        if (fechaA == null) return 1;
                        if (fechaB == null) return -1;
                        return fechaA.compareTo(fechaB);
                    });

                    Log.d("FIREBASE_EVENTO", "════════════════════════════════════════════");
                    Log.d("FIREBASE_EVENTO", "📅 Fecha actual: " + fechaActualISO);
                    Log.d("FIREBASE_EVENTO", "✅ " + eventosActivos.size() + " eventos activos");
                    Log.d("FIREBASE_EVENTO", "════════════════════════════════════════════");

                    callback.onSuccess(eventosActivos);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_EVENTO", "❌ Error: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Eliminar eventos que ya terminaron (fecha_fin < fecha actual)
     * @param callback Callback con el resultado
     */
    public void eliminarEventosPasados(FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fecha actual en ISO
        String fechaActualISO = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Log.d("FIREBASE_EVENTO", "════════════════════════════════════════════");
        Log.d("FIREBASE_EVENTO", "🗑️ Buscando eventos pasados para eliminar");
        Log.d("FIREBASE_EVENTO", "📅 Fecha actual: " + fechaActualISO);
        Log.d("FIREBASE_EVENTO", "════════════════════════════════════════════");

        // Buscar eventos con fecha_fin < fecha actual
        db.collection("eventos")
                .whereLessThan("fecha_fin", fechaActualISO)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> eventosPasados = queryDocumentSnapshots.getDocuments();

                    if (eventosPasados.isEmpty()) {
                        Log.d("FIREBASE_EVENTO", "✅ No hay eventos pasados para eliminar");
                        if (callback != null) {
                            callback.onSuccess("No hay eventos pasados");
                        }
                        return;
                    }

                    Log.d("FIREBASE_EVENTO", "📦 " + eventosPasados.size() + " eventos pasados encontrados");

                    // Eliminar cada evento
                    int[] eliminados = {0};
                    int total = eventosPasados.size();

                    for (DocumentSnapshot evento : eventosPasados) {
                        String idFirebase = evento.getId();
                        String nombre = evento.getString("nombre");
                        String fechaFin = evento.getString("fecha_fin_display");
                        if (fechaFin == null) {
                            fechaFin = evento.getString("fecha_fin");
                        }

                        Log.d("FIREBASE_EVENTO", "  🗑️ Eliminando: " + nombre + " (finalizó: " + fechaFin + ")");

                        db.collection("eventos").document(idFirebase).delete()
                                .addOnSuccessListener(aVoid -> {
                                    eliminados[0]++;
                                    Log.d("FIREBASE_EVENTO", "    ✅ Eliminado: " + nombre);

                                    if (eliminados[0] == total && callback != null) {
                                        Log.d("FIREBASE_EVENTO", "════════════════════════════════════════════");
                                        Log.d("FIREBASE_EVENTO", "✅ Eliminados " + eliminados[0] + " eventos pasados");
                                        Log.d("FIREBASE_EVENTO", "════════════════════════════════════════════");
                                        callback.onSuccess("Eliminados " + eliminados[0] + " eventos pasados");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    eliminados[0]++;
                                    Log.e("FIREBASE_EVENTO", "    ❌ Error eliminando: " + nombre + " - " + e.getMessage());

                                    if (eliminados[0] == total && callback != null) {
                                        callback.onSuccess("Eliminados " + eliminados[0] + " eventos (con errores)");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_EVENTO", "❌ Error buscando eventos pasados: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Guardar usuario en Firestore usando carnet como ID
     * @param usuario Usuario a guardar
     * @param callback Callback para resultado
     */
    public void guardarUsuarioEnFirestore(Usuarios usuario, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("id_tipo", usuario.getId_tipo());
        usuarioMap.put("nombres", usuario.getNombres());
        usuarioMap.put("apellidos", usuario.getApellidos());
        usuarioMap.put("correo", usuario.getCorreo());
        usuarioMap.put("carnet", usuario.getCarnet());
        usuarioMap.put("contraseña", usuario.getContraseña());
        usuarioMap.put("estado", usuario.getEstado());
        usuarioMap.put("fecha_creacion", FieldValue.serverTimestamp());

        // ✅ Usar carnet como ID (único e inmutable)
        String idFirebase = usuario.getCarnet();

        Log.d("FIREBASE_USUARIO", "════════════════════════════════════════════");
        Log.d("FIREBASE_USUARIO", "📝 Guardando usuario en Firestore");
        Log.d("FIREBASE_USUARIO", "  Carnet: " + idFirebase);
        Log.d("FIREBASE_USUARIO", "  Nombre: " + usuario.getNombres());
        Log.d("FIREBASE_USUARIO", "  Tipo: " + usuario.getId_tipo());
        Log.d("FIREBASE_USUARIO", "════════════════════════════════════════════");

        db.collection("usuarios").document(idFirebase).set(usuarioMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_USUARIO", "✅ Usuario guardado: " + usuario.getCarnet());
                    if (callback != null) {
                        callback.onSuccess("Usuario guardado: " + usuario.getNombres());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_USUARIO", "❌ Error: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void actualizarUsuarioPorCarnet(Usuarios usuario, FirebaseCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("id_tipo", usuario.getId_tipo());
        usuarioMap.put("nombres", usuario.getNombres());
        usuarioMap.put("apellidos", usuario.getApellidos());
        usuarioMap.put("correo", usuario.getCorreo());
        usuarioMap.put("contraseña", usuario.getContraseña());
        usuarioMap.put("estado", usuario.getEstado());

        // El carnet NO se cambia porque es el ID
        String carnet = usuario.getCarnet();

        Log.d("FIREBASE_USUARIO", "════════════════════════════════════════════");
        Log.d("FIREBASE_USUARIO", "✏️ Actualizando usuario");
        Log.d("FIREBASE_USUARIO", "  Carnet: " + carnet);
        Log.d("FIREBASE_USUARIO", "════════════════════════════════════════════");

        db.collection("usuarios")
                .document(carnet)
                .update(usuarioMap)
                .addOnSuccessListener(unused -> {

                    Log.d("FIREBASE_USUARIO", "✅ Usuario actualizado");

                    if (callback != null) {
                        callback.onSuccess("Usuario actualizado");
                    }
                })
                .addOnFailureListener(e -> {

                    Log.e("FIREBASE_USUARIO", "❌ Error: " + e.getMessage());

                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }


    /**
     * Soft delete de usuario (cambiar estado a 0)
     * @param carnet Carnet del usuario
     * @param callback Callback para resultado
     */
    public void softDeleteUsuarioPorCarnet(String carnet, FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Validar carnet
        if (carnet == null || carnet.isEmpty()) {
            Log.e("FIREBASE_USUARIO", "❌ Carnet no puede ser nulo");
            if (callback != null) {
                callback.onError("Carnet inválido");
            }
            return;
        }

        // Crear mapa con los campos a actualizar
        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", 0);
        updates.put("fecha_eliminacion", FieldValue.serverTimestamp());

        Log.d("FIREBASE_USUARIO", "════════════════════════════════════════════");
        Log.d("FIREBASE_USUARIO", "🗑️ SOFT DELETE - Eliminando lógicamente");
        Log.d("FIREBASE_USUARIO", "  Carnet: " + carnet);
        Log.d("FIREBASE_USUARIO", "  Estado actual: 1 → 0");
        Log.d("FIREBASE_USUARIO", "════════════════════════════════════════════");

        // Actualizar estado del documento
        db.collection("usuarios").document(carnet).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_USUARIO", "✅ Usuario eliminado lógicamente");
                    Log.d("FIREBASE_USUARIO", "  Carnet: " + carnet);
                    if (callback != null) {
                        callback.onSuccess("Usuario eliminado correctamente");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_USUARIO", "❌ Error eliminando usuario");
                    Log.e("FIREBASE_USUARIO", "  Carnet: " + carnet);
                    Log.e("FIREBASE_USUARIO", "  Error: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Error al eliminar usuario: " + e.getMessage());
                    }
                });
    }

    public void obtenerUsuariosActualizados(
            Timestamp ultimaFecha,
            FirebaseCallback callback
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("usuarios")
                .whereGreaterThan("fecha_actualizacion", ultimaFecha)
                .orderBy("fecha_actualizacion", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Usuarios> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {

                        Usuarios usuario = doc.toObject(Usuarios.class);

                        if (usuario != null) {

                            usuario.setCarnet(doc.getId());

                            // guardar timestamp si quieres
                            Timestamp ts = doc.getTimestamp("fecha_actualizacion");

                            lista.add(usuario);

                            Log.d("SYNC_FIREBASE",
                                    "⬇ Usuario actualizado: "
                                            + usuario.getNombres());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SYNC_FIREBASE", "❌ " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Obtener TODOS los usuarios de Firebase de una sola vez
     */

    public void obtenerYGuardarTodosLosUsuarios(Context context) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Database dbLocal = new Database(context);

        Log.d("SYNC_USUARIOS", "════════════════════════════════════════════");
        Log.d("SYNC_USUARIOS", "🚀 INICIANDO SINCRONIZACIÓN DE USUARIOS");
        Log.d("SYNC_USUARIOS", "════════════════════════════════════════════");

        long inicioTiempo = System.currentTimeMillis();

        db.collection("usuarios")
                .get()

                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int contadorGuardados = 0;
                    int contadorActualizados = 0;
                    int contadorErrores = 0;

                    int total = queryDocumentSnapshots.size();

                    Log.d("SYNC_USUARIOS", "📥 Usuarios encontrados en Firebase: " + total);

                    if (total == 0) {

                        Log.w("SYNC_USUARIOS", "⚠️ No hay usuarios para sincronizar");
                        return;
                    }

                    Log.d("SYNC_USUARIOS", "════════════════════════════════════════════");

                    int posicion = 1;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {

                        try {

                            Log.d("SYNC_USUARIOS", "");
                            Log.d("SYNC_USUARIOS", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                            Log.d("SYNC_USUARIOS", "👤 PROCESANDO USUARIO " + posicion + "/" + total);

                            // ==================== OBTENER DATOS ====================

                            String carnet = doc.getId();

                            String nombres = doc.getString("nombres");
                            String apellidos = doc.getString("apellidos");
                            String contra = doc.getString("contraseña");

                            // ⚠️ OJO: en tu guardar usas "correo"
                            String correo = doc.getString("correo");

                            Long idTipoLong = doc.getLong("id_tipo");
                            Long estadoLong = doc.getLong("estado");

                            int idTipo = (idTipoLong != null)
                                    ? idTipoLong.intValue()
                                    : 1;

                            int estado = (estadoLong != null)
                                    ? estadoLong.intValue()
                                    : 1;

                            Log.d("SYNC_USUARIOS", "🆔 Carnet: " + carnet);
                            Log.d("SYNC_USUARIOS", "📛 Nombre: " + nombres);
                            Log.d("SYNC_USUARIOS", "📛 Apellido: " + apellidos);
                            Log.d("SYNC_USUARIOS", "📧 Correo: " + correo);
                            Log.d("SYNC_USUARIOS", "🔐 Contraseña: " + contra);
                            Log.d("SYNC_USUARIOS", "👨‍💼 Tipo: " + idTipo);
                            Log.d("SYNC_USUARIOS", "📌 Estado: " + estado);

                            // ==================== VALIDACIONES ====================

                            if (carnet == null || carnet.isEmpty()) {

                                Log.e("SYNC_USUARIOS", "❌ ERROR: carnet vacío");
                                contadorErrores++;
                                continue;
                            }

                            if (nombres == null) nombres = "";
                            if (apellidos == null) apellidos = "";
                            if (correo == null) correo = "";
                            if (contra == null) contra = "";

                            // ==================== CREAR OBJETO ====================

                            Usuarios usuario = new Usuarios(
                                    0,
                                    idTipo,
                                    nombres,
                                    apellidos,
                                    correo,
                                    carnet,
                                    contra,
                                    estado
                            );

                            // ==================== VERIFICAR EXISTENCIA ====================


                            boolean guardado =
                                    dbLocal.insertarOActualizarPorCarnet(usuario);

                            if (guardado) {

                            } else {

                                contadorErrores++;

                                Log.e("SYNC_USUARIOS",
                                        "❌ ERROR GUARDANDO EN SQLITE");
                            }

                        } catch (Exception e) {

                            contadorErrores++;

                            Log.e("SYNC_USUARIOS",
                                    "❌ EXCEPCIÓN PROCESANDO USUARIO");

                            Log.e("SYNC_USUARIOS",
                                    "Mensaje: " + e.getMessage());

                            e.printStackTrace();
                        }

                        posicion++;
                    }

                    // ==================== RESUMEN FINAL ====================

                    long finTiempo = System.currentTimeMillis();
                    long duracion = finTiempo - inicioTiempo;

                    Log.d("SYNC_USUARIOS", "");
                    Log.d("SYNC_USUARIOS", "════════════════════════════════════════════");
                    Log.d("SYNC_USUARIOS", "🏁 SINCRONIZACIÓN FINALIZADA");
                    Log.d("SYNC_USUARIOS", "════════════════════════════════════════════");

                    Log.d("SYNC_USUARIOS", "📥 Total Firebase: " + total);
                    Log.d("SYNC_USUARIOS", "➕ Insertados: " + contadorGuardados);
                    Log.d("SYNC_USUARIOS", "🔄 Actualizados: " + contadorActualizados);
                    Log.d("SYNC_USUARIOS", "❌ Errores: " + contadorErrores);

                    Log.d("SYNC_USUARIOS", "⏱ Tiempo total: " + duracion + " ms");

                    Log.d("SYNC_USUARIOS", "════════════════════════════════════════════");
                })

                .addOnFailureListener(e -> {

                    Log.e("SYNC_USUARIOS", "════════════════════════════════════════════");
                    Log.e("SYNC_USUARIOS", "❌ ERROR OBTENIENDO USUARIOS");
                    Log.e("SYNC_USUARIOS", "════════════════════════════════════════════");

                    Log.e("SYNC_USUARIOS", "📛 Mensaje: " + e.getMessage());

                    e.printStackTrace();
                });
    }


    public interface FirebaseBusquedaCallback {
        void onComplete(int resultados, int guardados);
        void onError(String error);
    }

    public void buscarYGuardarUsuarios(
            String textoBusqueda,
            int tipoFiltro,
            Context context,
            FirebaseBusquedaCallback callback
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Database dbLocal = new Database(context);

        Log.d("FIREBASE", "════════════════════════════════════");
        Log.d("FIREBASE", "🔍 BUSCANDO USUARIOS");
        Log.d("FIREBASE", "📌 Texto búsqueda: " + textoBusqueda);
        Log.d("FIREBASE", "📌 Tipo filtro: " + tipoFiltro);

        Query query = db.collection("usuarios");

        // Filtro por tipo
        if (tipoFiltro != -1) {
            query = query.whereEqualTo("id_tipo", tipoFiltro);
        }

        // ✅ Limitar resultados
        query = query.limit(20);

        query.get()

                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Usuarios> resultados = new ArrayList<>();

                    int guardados = 0;
                    int total = queryDocumentSnapshots.size();

                    Log.d("FIREBASE", "📥 Documentos obtenidos: " + total);

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {

                        try {

                            String carnet = doc.getId();

                            String nombres = doc.getString("nombres");
                            String apellidos = doc.getString("apellidos");
                            String contra = doc.getString("contraseña");
                            String correo = doc.getString("correo");

                            Long idTipoLong = doc.getLong("id_tipo");
                            Long estadoLong = doc.getLong("estado");

                            int idTipo = (idTipoLong != null)
                                    ? idTipoLong.intValue()
                                    : 1;

                            int estado = (estadoLong != null)
                                    ? estadoLong.intValue()
                                    : 1;

                            Usuarios usuario = new Usuarios(
                                    0,
                                    idTipo,
                                    nombres,
                                    apellidos,
                                    correo,
                                    carnet,
                                    contra,
                                    estado
                            );

                            // ==================== FILTRO ====================

                            boolean coincide = false;

                            if (textoBusqueda != null && !textoBusqueda.isEmpty()) {

                                String busqueda = textoBusqueda.toLowerCase();

                                if (
                                        carnet.toLowerCase().contains(busqueda)
                                                || nombres.toLowerCase().contains(busqueda)
                                ) {

                                    coincide = true;
                                }

                            } else {

                                coincide = true;
                            }

                            // ==================== GUARDAR ====================

                            if (coincide) {

                                resultados.add(usuario);

                                Log.d("FIREBASE",
                                        "✅ Usuario encontrado: "
                                                + carnet
                                                + " - "
                                                + nombres);

                                if (dbLocal.insertarOActualizarPorCarnet(usuario)) {

                                    guardados++;

                                    Log.d("FIREBASE",
                                            "💾 Guardado localmente: "
                                                    + carnet);

                                } else {

                                    Log.w("FIREBASE",
                                            "⚠️ No se guardó: "
                                                    + carnet);
                                }
                            }

                        } catch (Exception e) {

                            Log.e("FIREBASE",
                                    "❌ Error procesando documento: "
                                            + e.getMessage());
                        }
                    }

                    Log.d("FIREBASE", "════════════════════════════════════");
                    Log.d("FIREBASE", "✅ BÚSQUEDA FINALIZADA");
                    Log.d("FIREBASE", "📥 Total documentos: " + total);
                    Log.d("FIREBASE", "🔍 Resultados: " + resultados.size());
                    Log.d("FIREBASE", "💾 Guardados: " + guardados);
                    Log.d("FIREBASE", "════════════════════════════════════");

                    dbLocal.close();

                    // ✅ CALLBACK
                    if (callback != null) {
                        callback.onComplete(resultados.size(), guardados);
                    }
                })

                .addOnFailureListener(e -> {

                    Log.e("FIREBASE",
                            "❌ Error en búsqueda: "
                                    + e.getMessage());

                    dbLocal.close();

                    // ✅ CALLBACK ERROR
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }


    public void buscarUsuarioPorCarnet(
            String carnet,
            FirebaseUsuarioCallback callback
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FIREBASE_BUSCAR",
                "═══════════════════════════════");

        Log.d("FIREBASE_BUSCAR",
                "🔍 Buscando usuario: " + carnet);

        db.collection("usuarios")
                .document(carnet)
                .get()

                .addOnSuccessListener(documentSnapshot -> {

                    // ==================== EXISTE ====================

                    if (documentSnapshot.exists()) {

                        try {

                            // ==================== DATOS ====================

                            String nombres =
                                    documentSnapshot.getString("nombres");

                            String apellidos =
                                    documentSnapshot.getString("apellidos");

                            String correo =
                                    documentSnapshot.getString("correo");

                            String contra =
                                    documentSnapshot.getString("contraseña");

                            Long idTipoLong =
                                    documentSnapshot.getLong("id_tipo");

                            Long estadoLong =
                                    documentSnapshot.getLong("estado");

                            int idTipo = (idTipoLong != null)
                                    ? idTipoLong.intValue()
                                    : 1;

                            int estado = (estadoLong != null)
                                    ? estadoLong.intValue()
                                    : 0;

                            // ==================== VALIDAR ESTADO ====================

                            if (estado != 1) {

                                Log.w("FIREBASE_BUSCAR",
                                        "⚠️ Usuario desactivado");

                                callback.onNotFound();
                                return;
                            }

                            // ==================== CREAR OBJETO ====================

                            Usuarios usuario = new Usuarios(
                                    0,
                                    idTipo,
                                    nombres,
                                    apellidos,
                                    correo,
                                    carnet,
                                    contra,
                                    estado
                            );

                            Log.d("FIREBASE_BUSCAR",
                                    "✅ Usuario encontrado");

                            Log.d("FIREBASE_BUSCAR",
                                    "👤 Nombre: " + nombres);

                            Log.d("FIREBASE_BUSCAR",
                                    "🆔 Carnet: " + carnet);

                            Log.d("FIREBASE_BUSCAR",
                                    "📧 Correo: " + correo);

                            Log.d("FIREBASE_BUSCAR",
                                    "👥 Tipo: " + idTipo);

                            Log.d("FIREBASE_BUSCAR",
                                    "📌 Estado: " + estado);

                            Log.d("FIREBASE_BUSCAR",
                                    "═══════════════════════════════");

                            callback.onSuccess(usuario);

                        } catch (Exception e) {

                            Log.e("FIREBASE_BUSCAR",
                                    "❌ Error procesando usuario");

                            Log.e("FIREBASE_BUSCAR",
                                    "❌ " + e.getMessage());

                            callback.onError(e.getMessage());
                        }

                    } else {

                        Log.w("FIREBASE_BUSCAR",
                                "⚠️ Usuario no existe");

                        callback.onNotFound();
                    }
                })

                .addOnFailureListener(e -> {

                    Log.e("FIREBASE_BUSCAR",
                            "❌ Error Firebase");

                    Log.e("FIREBASE_BUSCAR",
                            "❌ " + e.getMessage());

                    callback.onError(e.getMessage());
                });
    }

    // Para el olvido de constraseña
    public void buscarUsuarioPorCorreo(
            String correo,
            FirebaseUsuarioCallback callback
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FIREBASE_BUSCAR",
                "═══════════════════════════════");

        Log.d("FIREBASE_BUSCAR",
                "🔍 Buscando usuario por correo: " + correo);

        db.collection("usuarios")
                .whereEqualTo("correo", correo)
                .limit(1)
                .get()

                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // ==================== EXISTE ====================

                    if (!queryDocumentSnapshots.isEmpty()) {

                        try {

                            DocumentSnapshot documentSnapshot =
                                    queryDocumentSnapshots.getDocuments().get(0);

                            // ==================== DATOS ====================

                            String nombres =
                                    documentSnapshot.getString("nombres");

                            String apellidos =
                                    documentSnapshot.getString("apellidos");

                            String carnet =
                                    documentSnapshot.getString("carnet");

                            String contra =
                                    documentSnapshot.getString("contraseña");

                            Long idTipoLong =
                                    documentSnapshot.getLong("id_tipo");

                            Long estadoLong =
                                    documentSnapshot.getLong("estado");

                            int idTipo = (idTipoLong != null)
                                    ? idTipoLong.intValue()
                                    : 1;

                            int estado = (estadoLong != null)
                                    ? estadoLong.intValue()
                                    : 0;

                            // ==================== VALIDAR ESTADO ====================

                            if (estado != 1) {

                                Log.w("FIREBASE_BUSCAR",
                                        "⚠️ Usuario desactivado");

                                callback.onNotFound();
                                return;
                            }

                            // ==================== CREAR OBJETO ====================

                            Usuarios usuario = new Usuarios(
                                    0,
                                    idTipo,
                                    nombres,
                                    apellidos,
                                    correo,
                                    carnet,
                                    contra,
                                    estado
                            );

                            Log.d("FIREBASE_BUSCAR",
                                    "✅ Usuario encontrado por correo");

                            Log.d("FIREBASE_BUSCAR",
                                    "👤 Nombre: " + nombres);

                            Log.d("FIREBASE_BUSCAR",
                                    "🆔 Carnet: " + carnet);

                            Log.d("FIREBASE_BUSCAR",
                                    "📧 Correo: " + correo);

                            Log.d("FIREBASE_BUSCAR",
                                    "👥 Tipo: " + idTipo);

                            Log.d("FIREBASE_BUSCAR",
                                    "📌 Estado: " + estado);

                            Log.d("FIREBASE_BUSCAR",
                                    "═══════════════════════════════");

                            callback.onSuccess(usuario);

                        } catch (Exception e) {

                            Log.e("FIREBASE_BUSCAR",
                                    "❌ Error procesando usuario");

                            Log.e("FIREBASE_BUSCAR",
                                    "❌ " + e.getMessage());

                            callback.onError(e.getMessage());
                        }

                    } else {

                        Log.w("FIREBASE_BUSCAR",
                                "⚠️ No existe usuario con correo: " + correo);

                        callback.onNotFound();
                    }
                })

                .addOnFailureListener(e -> {

                    Log.e("FIREBASE_BUSCAR",
                            "❌ Error Firebase");

                    Log.e("FIREBASE_BUSCAR",
                            "❌ " + e.getMessage());

                    callback.onError(e.getMessage());
                });
    }

    public interface FirebaseUsuarioCallback {

        void onSuccess(Usuarios usuario);

        void onNotFound();

        void onError(String error);
    }



    // ════════════════════════════════════════════════════════════════
    // LISTENERS
    // ════════════════════════════════════════════════════════════════
    public interface OnLugaresObtendidosListener {
        void onSuccess(List<Map<String, Object>> lugares);
        void onError(String error);
    }

    public interface OnPisosObtendidosListener {
        void onSuccess(List<Map<String, Object>> pisos);
        void onError(String error);
    }

    public interface OnEspaciosObtendidosListener {
        void onSuccess(List<Map<String, Object>> espacios);
        void onError(String error);
    }

    public interface OnGeometriaObtendidaListener {
        void onSuccess(List<Map<String, Object>> geometrias);
        void onError(String error);
    }
}