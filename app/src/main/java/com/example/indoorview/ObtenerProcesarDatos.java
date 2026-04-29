package com.example.indoorview;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.indoorview.models.CouchDBResponse;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * OBTENER Y PROCESAR DATOS DE COUCHDB
 * Versión mejorada con procesamiento automático
 * ═══════════════════════════════════════════════════════════════════════
 */
public class ObtenerProcesarDatos extends AsyncTask<Void, String, Boolean> {

    private static final String TAG = "OBTENER_PROCESAR";
    private Context context;
    private Database db;
    private ProcesadorDatosCouchDB procesador;
    private OnDatosCargatosListener listener;
    private String urlConsulta;

    // Para la conexión HTTP
    private java.net.HttpURLConnection httpURLConnection;

    /**
     * Interfaz para notificar cuando los datos se cargaron
     */
    public interface OnDatosCargatosListener {
        void onDatosEnCarga();
        void onDatosCargados(boolean exitoso, String mensaje);
    }

    /**
     * Constructor
     */
    public ObtenerProcesarDatos(Context context, Database db, String urlConsulta) {
        this.context = context;
        this.db = db;
        this.urlConsulta = urlConsulta;
        this.procesador = new ProcesadorDatosCouchDB(db, context);
    }

    /**
     * Establecer listener para notificaciones
     */
    public void setOnDatosCargatosListener(OnDatosCargatosListener listener) {
        this.listener = listener;
    }

    /**
     * Se ejecuta antes de comenzar en background
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "════════════════════════════════════════════");
        Log.d(TAG, "INICIANDO CARGA DE DATOS");
        Log.d(TAG, "════════════════════════════════════════════");
        Log.d(TAG, "URL: " + urlConsulta);

        if (listener != null) {
            listener.onDatosEnCarga();
        }
    }

    /**
     * PASO 1: Obtener datos del servidor
     * PASO 2: Parsear JSON
     * PASO 3: Insertar en BD local
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            // ═══════════════════════════════════════════════════════════════
            // PASO 1: OBTENER JSON DEL SERVIDOR
            // ═══════════════════════════════════════════════════════════════
            Log.d(TAG, "\n🌐 PASO 1: Obteniendo datos del servidor...");
            String jsonResponse = obtenerJsonDelServidor();

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                Log.e(TAG, "✗ No se obtuvo respuesta del servidor");
                return false;
            }

            Log.d(TAG, "✓ JSON obtenido (" + jsonResponse.length() + " caracteres)");
            Log.d(TAG, "Primeros 200 caracteres:\n" + jsonResponse.substring(0, Math.min(200, jsonResponse.length())));

            publishProgress("Datos obtenidos del servidor");

            // ═══════════════════════════════════════════════════════════════
            // PASO 2: PARSEAR JSON
            // ═══════════════════════════════════════════════════════════════
            Log.d(TAG, "\n📋 PASO 2: Parseando JSON...");
            CouchDBResponse response = procesador.parsearRespuesta(jsonResponse);

            if (response == null) {
                Log.e(TAG, "✗ Error parseando JSON");
                return false;
            }

            publishProgress("JSON parseado correctamente");

            // ═══════════════════════════════════════════════════════════════
            // PASO 3: INSERTAR EN BASE DE DATOS LOCAL
            // ═══════════════════════════════════════════════════════════════
            Log.d(TAG, "\n💾 PASO 3: Insertando en BD local...");
            boolean insercionExitosa = procesador.procesarRespuestaCompleta(response);

            if (!insercionExitosa) {
                Log.e(TAG, "✗ Error en la inserción");
                return false;
            }

            publishProgress("Datos insertados en BD local");

            Log.d(TAG, "\n✅ ¡PROCESO COMPLETADO EXITOSAMENTE!");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "✗ Error en doInBackground: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Se ejecuta en el thread principal mientras avanza
     */
    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        if (values.length > 0) {
            Log.d(TAG, "📊 Progreso: " + values[0]);
        }
    }

    /**
     * Se ejecuta en el thread principal cuando termina
     */
    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (result) {
            Log.d(TAG, "\n✅ RESULTADO FINAL: ÉXITO");
            if (listener != null) {
                listener.onDatosCargados(true, "Datos cargados correctamente");
            }
        } else {
            Log.d(TAG, "\n❌ RESULTADO FINAL: ERROR");
            if (listener != null) {
                listener.onDatosCargados(false, "Error al cargar los datos");
            }
        }
    }

    /**
     * Obtener JSON del servidor via HTTP GET
     * @return JSON crudo del servidor o null
     */
    private String obtenerJsonDelServidor() {
        StringBuilder respuesta = new StringBuilder();
        try {
            java.net.URL url = new java.net.URL(urlConsulta);
            httpURLConnection = (java.net.HttpURLConnection) url.openConnection();

            // Configurar conexión
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Authorization",
                    "Basic " + Utilidades.credencialesCodificadas);
            httpURLConnection.setRequestProperty("Accept", "application/json");

            Log.d(TAG, "Conectando a servidor...");
            int codigoRespuesta = httpURLConnection.getResponseCode();
            Log.d(TAG, "Código de respuesta: " + codigoRespuesta);

            if (codigoRespuesta == java.net.HttpURLConnection.HTTP_OK) {
                // Leer respuesta
                java.io.InputStream inputStream = httpURLConnection.getInputStream();
                java.io.BufferedReader bufferedReader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(inputStream)
                        );

                String linea;
                while ((linea = bufferedReader.readLine()) != null) {
                    respuesta.append(linea);
                }

                bufferedReader.close();
                inputStream.close();

                Log.d(TAG, "✓ Respuesta obtenida: " + respuesta.length() + " caracteres");
                return respuesta.toString();

            } else {
                Log.e(TAG, "✗ Error HTTP: " + codigoRespuesta);

                // Intentar leer mensaje de error
                java.io.InputStream errorStream = httpURLConnection.getErrorStream();
                if (errorStream != null) {
                    java.io.BufferedReader errorReader =
                            new java.io.BufferedReader(
                                    new java.io.InputStreamReader(errorStream)
                            );
                    String lineaError;
                    while ((lineaError = errorReader.readLine()) != null) {
                        Log.e(TAG, "Error: " + lineaError);
                    }
                    errorReader.close();
                }
                return null;
            }

        } catch (java.net.MalformedURLException e) {
            Log.e(TAG, "✗ URL inválida: " + e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            Log.e(TAG, "✗ Error de I/O: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "✗ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }
}