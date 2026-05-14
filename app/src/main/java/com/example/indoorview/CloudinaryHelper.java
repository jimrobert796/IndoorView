package com.example.indoorview;

import android.util.Log;
import okhttp3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

public class CloudinaryHelper {





    private OkHttpClient client = new OkHttpClient();

    public interface UploadCallback {
        void onResult(boolean success, String url, String publicId);
    }

    public interface DeleteCallback {
        void onResult(boolean success);
    }

    // ════════════════════════════════════════════════════════════════
    // SUBIR IMAGEN CON LOGS DETALLADOS
    // ════════════════════════════════════════════════════════════════
    public void subirImagen(File archivo, UploadCallback callback) {
        Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
        Log.d("CLOUDINARY_UPLOAD", "🚀 INICIANDO UPLOAD DE IMAGEN");
        Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");

        // Verificar que el archivo existe
        if (archivo == null) {
            Log.e("CLOUDINARY_UPLOAD", "❌ ERROR: Archivo es NULL");
            callback.onResult(false, "", "");
            return;
        }

        if (!archivo.exists()) {
            Log.e("CLOUDINARY_UPLOAD", "❌ ERROR: Archivo no existe en: " + archivo.getAbsolutePath());
            callback.onResult(false, "", "");
            return;
        }

        Log.d("CLOUDINARY_UPLOAD", "✓ Archivo encontrado");
        Log.d("CLOUDINARY_UPLOAD", "  Nombre: " + archivo.getName());
        Log.d("CLOUDINARY_UPLOAD", "  Tamaño: " + archivo.length() + " bytes");
        Log.d("CLOUDINARY_UPLOAD", "  Ruta: " + archivo.getAbsolutePath());

        // Configuración
        Log.d("CLOUDINARY_UPLOAD", "✓ Configuración Cloudinary:");
        Log.d("CLOUDINARY_UPLOAD", "  CLOUD_NAME: " + CLOUD_NAME);
        Log.d("CLOUDINARY_UPLOAD", "  UPLOAD_PRESET: " + UPLOAD_PRESET);
        Log.d("CLOUDINARY_UPLOAD", "  API_KEY: " + API_KEY);

        RequestBody fileBody = RequestBody.create(
                MediaType.parse("image/*"),
                archivo
        );

        Log.d("CLOUDINARY_UPLOAD", "✓ RequestBody creado");

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", archivo.getName(), fileBody)
                .addFormDataPart("upload_preset", UPLOAD_PRESET);

        RequestBody requestBody = builder.build();
        Log.d("CLOUDINARY_UPLOAD", "✓ MultipartBody construido");

        String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
        Log.d("CLOUDINARY_UPLOAD", "✓ URL de upload: " + uploadUrl);

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build();

        Log.d("CLOUDINARY_UPLOAD", "✓ Request construido, enviando...");
        Log.d("CLOUDINARY_UPLOAD", "  Esperando respuesta del servidor...");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
                Log.e("CLOUDINARY_UPLOAD", "❌ FALLO EN LA PETICIÓN");
                Log.e("CLOUDINARY_UPLOAD", "Error: " + e.getMessage());
                Log.e("CLOUDINARY_UPLOAD", "Causa: " + e.getCause());
                Log.e("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
                e.printStackTrace();
                callback.onResult(false, "", "");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
                Log.d("CLOUDINARY_UPLOAD", "✓ RESPUESTA RECIBIDA DEL SERVIDOR");
                Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
                Log.d("CLOUDINARY_UPLOAD", "  Código HTTP: " + response.code());
                Log.d("CLOUDINARY_UPLOAD", "  Mensaje: " + response.message());

                try {
                    String jsonResponse = response.body().string();
                    Log.d("CLOUDINARY_UPLOAD", "✓ JSON recibido:");
                    Log.d("CLOUDINARY_UPLOAD", jsonResponse);

                    JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

                    if (response.code() == 200) {
                        Log.d("CLOUDINARY_UPLOAD", "✓ Código 200 OK");

                        if (json.has("secure_url")) {
                            String url = json.get("secure_url").getAsString();
                            String publicId = json.get("public_id").getAsString();

                            Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
                            Log.d("CLOUDINARY_UPLOAD", "✅ ✅ ✅ UPLOAD EXITOSO ✅ ✅ ✅");
                            Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");
                            Log.d("CLOUDINARY_UPLOAD", "  URL: " + url);
                            Log.d("CLOUDINARY_UPLOAD", "  Public ID: " + publicId);
                            Log.d("CLOUDINARY_UPLOAD", "════════════════════════════════════════════");

                            callback.onResult(true, url, publicId);
                        } else {
                            Log.e("CLOUDINARY_UPLOAD", "❌ ERROR: No hay 'secure_url' en respuesta");
                            Log.e("CLOUDINARY_UPLOAD", "Campos disponibles:");
                            for (String key : json.keySet()) {
                                Log.e("CLOUDINARY_UPLOAD", "  - " + key + ": " + json.get(key));
                            }
                            callback.onResult(false, "", "");
                        }
                    } else {
                        Log.e("CLOUDINARY_UPLOAD", "❌ ERROR HTTP: " + response.code());
                        Log.e("CLOUDINARY_UPLOAD", "Respuesta: " + jsonResponse);

                        if (json.has("error")) {
                            Log.e("CLOUDINARY_UPLOAD", "Error Cloudinary: " + json.get("error").getAsJsonObject());
                        }

                        callback.onResult(false, "", "");
                    }
                } catch (Exception e) {
                    Log.e("CLOUDINARY_UPLOAD", "❌ ERROR al parsear JSON");
                    Log.e("CLOUDINARY_UPLOAD", "Exception: " + e.getMessage());
                    e.printStackTrace();
                    callback.onResult(false, "", "");
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    // ELIMINAR IMAGEN CON FIRMA
    // ════════════════════════════════════════════════════════════════
    public void eliminarImagen(String publicId, DeleteCallback callback) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;

            // Generar firma
            String toSign = "public_id=" + publicId + "&timestamp=" + timestamp + API_SECRET;
            String signature = generateSignature(toSign);

            Log.d("CLOUDINARY_DELETE", "🗑️ Eliminando imagen: " + publicId);
            Log.d("CLOUDINARY_DELETE", "  Timestamp: " + timestamp);
            Log.d("CLOUDINARY_DELETE", "  Signature: " + signature);

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("public_id", publicId)
                    .addFormDataPart("api_key", API_KEY)
                    .addFormDataPart("timestamp", String.valueOf(timestamp))
                    .addFormDataPart("signature", signature);

            RequestBody requestBody = builder.build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/destroy")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("CLOUDINARY_DELETE", "❌ Error: " + e.getMessage());
                    callback.onResult(false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonResponse = response.body().string();
                    Log.d("CLOUDINARY_DELETE", "✓ Respuesta: " + jsonResponse);

                    if (response.isSuccessful()) {
                        Log.d("CLOUDINARY_DELETE", "✅ Imagen eliminada");
                    } else {
                        Log.e("CLOUDINARY_DELETE", "❌ Error en eliminación");
                    }

                    callback.onResult(response.isSuccessful());
                }
            });

        } catch (Exception e) {
            Log.e("CLOUDINARY_DELETE", "❌ Error: " + e.getMessage());
            callback.onResult(false);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // GENERAR FIRMA SHA-1
    // ════════════════════════════════════════════════════════════════
    private String generateSignature(String toSign) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(toSign.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}