package com.example.indoorview;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// En tu DatabaseHelper o clase aparte
public class ImageStorageManager {

    private static final String TAG = "ImageStorageManager";
    private Context context;
    private File imageDirectory;

    public ImageStorageManager(Context context) {
        this.context = context;
        this.imageDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Crear directorio si no existe
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs();
            Log.d(TAG, "Directorio creado: " + imageDirectory.getAbsolutePath());
        }
    }

    /**
     * Extrae el nombre limpio de la imagen desde la URL de Cloudinary
     * Ejemplo: https://res.cloudinary.com/drnx0udik/image/upload/v1778867054/galeria_20260515_114411_img1_nqfsfg.jpg
     * Retorna: galeria_20260515_114411_img1
     */
    private String extraerNombreImagen(String cloudinaryUrl) {
        try {
            // Obtener solo el nombre del archivo de la URL
            String nombreCompleto = cloudinaryUrl.substring(cloudinaryUrl.lastIndexOf('/') + 1);

            // Separar nombre y extensión
            String nombre = nombreCompleto.substring(0, nombreCompleto.lastIndexOf('.'));
            String extension = nombreCompleto.substring(nombreCompleto.lastIndexOf('.'));

            // Remover el hash único de Cloudinary (últimas 6-8 caracteres después del último _)
            // Patrón: galeria_20260515_114411_img1_nqfsfg -> galeria_20260515_114411_img1
            Pattern pattern = Pattern.compile("^(.+?)_[a-z0-9]{6,}$");
            Matcher matcher = pattern.matcher(nombre);

            if (matcher.matches()) {
                nombre = matcher.group(1);
                Log.d(TAG, "Nombre limpio extraído: " + nombre + extension);
                return nombre + extension;
            } else {
                // Si no tiene hash, devolver tal cual
                Log.d(TAG, "Sin hash detectado, devolviendo: " + nombreCompleto);
                return nombreCompleto;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extrayendo nombre: " + e.getMessage());
            return null;
        }
    }

    /**
     * Valida que el nombre sea de galeria o camara
     */
    private boolean esNombreValido(String nombreArchivo) {
        return nombreArchivo.startsWith("galeria_") || nombreArchivo.startsWith("camara_");
    }

    /**
     * Descargar imagen desde URL y guardar localmente
     */
    public String descargarYGuardarImagen(String cloudinaryUrl) {
        try {
            String nombreArchivo = extraerNombreImagen(cloudinaryUrl);

            if (nombreArchivo == null || !esNombreValido(nombreArchivo)) {
                Log.e(TAG, "Nombre inválido: " + nombreArchivo);
                return null;
            }

            File imagenLocal = new File(imageDirectory, nombreArchivo);

            // Si ya existe, no descargar de nuevo
            if (imagenLocal.exists()) {
                Log.d(TAG, "Imagen ya existe localmente: " + imagenLocal.getAbsolutePath());
                return obtenerRutaBD(imagenLocal);
            }

            // Descargar desde Cloudinary
            InputStream inputStream = new URL(cloudinaryUrl).openStream();
            FileOutputStream outputStream = new FileOutputStream(imagenLocal);

            byte[] buffer = new byte[1024];
            int bytesLeidos;
            while ((bytesLeidos = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesLeidos);
            }

            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Imagen guardada: " + imagenLocal.getAbsolutePath());

            return obtenerRutaBD(imagenLocal);

        } catch (IOException e) {
            Log.e(TAG, "Error descargando imagen: " + e.getMessage());
            return null;
        }
    }

    /**
     * Guardar Bitmap directamente (para cámara)
     */
    public String guardarBitmapCamara(Bitmap bitmap, String nombrePersonalizado) {
        try {
            String nombreArchivo;

            if (nombrePersonalizado != null && !nombrePersonalizado.isEmpty()) {
                nombreArchivo = nombrePersonalizado;
            } else {
                // Generar nombre con timestamp: camara_20260515_114411_img1
                String timestamp = obtenerTimestamp();
                nombreArchivo = "camara_" + timestamp + ".jpg";
            }

            if (!esNombreValido(nombreArchivo)) {
                Log.e(TAG, "Nombre inválido: " + nombreArchivo);
                return null;
            }

            File imagenLocal = new File(imageDirectory, nombreArchivo);
            FileOutputStream outputStream = new FileOutputStream(imagenLocal);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();

            Log.d(TAG, "Bitmap guardado: " + imagenLocal.getAbsolutePath());

            return obtenerRutaBD(imagenLocal);

        } catch (IOException e) {
            Log.e(TAG, "Error guardando bitmap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la ruta relativa para guardar en BD
     * Ejemplo: /galeria_20260515_114411_img1.jpg
     */
    private String obtenerRutaBD(File archivo) {
        String rutaRelativa =   File.separator + archivo.getName();
        Log.d(TAG, "Ruta para BD: " + rutaRelativa);
        return rutaRelativa;
    }

    /**
     * Obtener ruta absoluta de una imagen guardada
     */
    public File obtenerArchivo(String rutaBD) {
        // Si rutaBD es "/galeria_20260515_114411_img1.jpg"
        // Extrae solo el nombre: "galeria_20260515_114411_img1.jpg"
        String nombreArchivo = new File(rutaBD).getName();
        return new File(imageDirectory, nombreArchivo);
    }

    /**
     * Generar timestamp en formato: 20260515_114411
     */
    private String obtenerTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new java.util.Date());
    }

    /**
     * Obtener todas las imágenes guardadas
     */
    public File[] obtenerTodasLasImagenes() {
        if (imageDirectory.exists()) {
            return imageDirectory.listFiles();
        }
        return new File[0];
    }

    /**
     * Eliminar una imagen
     */
    public boolean eliminarImagen(String rutaBD) {
        try {
            File archivo = obtenerArchivo(rutaBD);
            if (archivo.exists()) {
                boolean eliminada = archivo.delete();
                Log.d(TAG, "Imagen eliminada: " + eliminada);
                return eliminada;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error eliminando imagen: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtener el tamaño total de las imágenes guardadas
     */
    public long obtenerTamanoTotal() {
        long total = 0;
        File[] archivos = obtenerTodasLasImagenes();
        for (File archivo : archivos) {
            total += archivo.length();
        }
        return total;
    }
}