package com.example.indoorview;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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