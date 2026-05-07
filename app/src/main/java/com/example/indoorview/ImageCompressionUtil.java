package com.example.indoorview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


/**
 * Utilidad para comprimir imágenes sin perder mucha calidad
 * Reduce peso de 8MB a ~1-2MB
 *
 * USO:
 * ImageCompressionUtil.comprimirArchivoImagen(ruta, ruta, 70);
 */
public class ImageCompressionUtil {

    /**
     * ✅ Obtener ángulo de rotación desde EXIF
     * @param imagePath Ruta de la imagen
     * @return Ángulo de rotación (0, 90, 180, 270)
     */
    public static int obtenerRotacionExif(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    Log.d("EXIF", "Orientación: 90°");
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    Log.d("EXIF", "Orientación: 180°");
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d("EXIF", "Orientación: 270°");
                    return 270;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    Log.d("EXIF", "Orientación: Normal");
                    return 0;
            }
        } catch (Exception e) {
            Log.e("EXIF", "Error leyendo EXIF: " + e.getMessage());
            return 0;
        }
    }

    /**
     * ✅ Rotar bitmap según ángulo
     * @param bitmap Imagen original
     * @param degrees Ángulos a rotar (90, 180, 270)
     * @return Bitmap rotado
     */
    public static Bitmap rotarBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) return bitmap;

        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0, 0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

            // No reciclar si es el mismo bitmap
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }

            Log.d("ROTATION", "✓ Bitmap rotado: " + degrees + "°");
            return rotatedBitmap;
        } catch (Exception e) {
            Log.e("ROTATION", "Error rotando: " + e.getMessage());
            return bitmap;
        }
    }

    /**
     * ✅ Comprimir bitmap y guardar como archivo
     * @param bitmap Imagen a comprimir
     * @param outputFile Archivo destino
     * @param quality Calidad 0-100 (70-80 es ideal)
     * @return true si se comprimió exitosamente
     */
    public static boolean comprimirBitmap(Bitmap bitmap, File outputFile, int quality) {
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            fos.close();

            long sizeKB = outputFile.length() / 1024;
            Log.d("COMPRESS", "✓ Imagen comprimida: " + sizeKB + " KB");

            return true;
        } catch (Exception e) {
            Log.e("COMPRESS", "✗ Error comprimiendo: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Cargar imagen de archivo y comprimir
     * @param filePath Ruta del archivo
     * @return Bitmap comprimido
     */
    public static Bitmap cargarYComprimirDesdeArchivo(String filePath) {
        try {
            // Opciones para decodificar (reducir tamaño)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Reducir tamaño a la mitad

            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap == null) {
                Log.e("COMPRESS", "No se pudo decodificar imagen");
                return null;
            }

            // Escalar a resolución máxima de 1920x1080
            bitmap = escalarBitmap(bitmap, 1920, 1080);

            return bitmap;
        } catch (Exception e) {
            Log.e("COMPRESS", "✗ Error cargando imagen: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Cargar desde URI y comprimir
     * @param context Contexto de la app
     * @param uri URI de la imagen
     * @return Bitmap comprimido
     */
    public static Bitmap cargarYComprimirDesdeUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e("COMPRESS", "InputStream es null");
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                Log.e("COMPRESS", "No se pudo decodificar desde URI");
                return null;
            }

            // Escalar
            bitmap = escalarBitmap(bitmap, 1920, 1080);

            return bitmap;
        } catch (Exception e) {
            Log.e("COMPRESS", "✗ Error cargando desde URI: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Escalar bitmap a máximo de dimensiones
     * @param bitmap Imagen original
     * @param maxWidth Ancho máximo
     * @param maxHeight Alto máximo
     * @return Bitmap escalado
     */
    public static Bitmap escalarBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Si ya es menor, no escalar
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float scaleWidth = (float) maxWidth / width;
        float scaleHeight = (float) maxHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        Log.d("COMPRESS", "Escala: " + width + "x" + height +
                " → " + newWidth + "x" + newHeight);

        return scaledBitmap;
    }

    /**
     * ✅ PRINCIPAL: Comprimir + Rotar archivo de imagen completo
     * Carga, rota según EXIF, escala, comprime y guarda
     *
     * @param inputPath Ruta del archivo original
     * @param outputPath Ruta del archivo comprimido (puede ser igual para sobrescribir)
     * @param quality Calidad 0-100 (70 es ideal)
     * @return true si fue exitoso
     */
    public static boolean comprimirYRotarArchivoImagen(String inputPath, String outputPath, int quality) {
        try {
            Log.d("COMPRESS_ROTATE", "========== PROCESANDO IMAGEN ==========");
            Log.d("COMPRESS_ROTATE", "Input: " + inputPath);
            Log.d("COMPRESS_ROTATE", "Quality: " + quality + "%");

            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                Log.e("COMPRESS_ROTATE", "Archivo no existe: " + inputPath);
                return false;
            }

            long tamanioOriginal = inputFile.length() / 1024; // KB
            Log.d("COMPRESS_ROTATE", "Tamaño original: " + tamanioOriginal + " KB");

            // 1. OBTENER ROTACIÓN EXIF
            int rotacionExif = obtenerRotacionExif(inputPath);
            Log.d("COMPRESS_ROTATE", "Rotación EXIF detectada: " + rotacionExif + "°");

            // 2. CARGAR IMAGEN
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeFile(inputPath, options);

            if (bitmap == null) {
                Log.e("COMPRESS_ROTATE", "No se pudo decodificar imagen");
                return false;
            }

            // 3. ROTAR SI ES NECESARIO
            if (rotacionExif != 0) {
                bitmap = rotarBitmap(bitmap, rotacionExif);
                Log.d("COMPRESS_ROTATE", "✓ Imagen rotada a posición correcta");
            } else {
                Log.d("COMPRESS_ROTATE", "✓ Imagen ya está en orientación correcta");
            }

            // 4. ESCALAR
            bitmap = escalarBitmap(bitmap, 1920, 1080);

            // 5. COMPRIMIR
            File outputFile = new File(outputPath);
            boolean resultado = comprimirBitmap(bitmap, outputFile, quality);

            if (resultado) {
                long tamanioComprimido = outputFile.length() / 1024; // KB
                long ahorro = tamanioOriginal - tamanioComprimido;
                double porcentajeAhorro = (double) ahorro / tamanioOriginal * 100;

                Log.d("COMPRESS_ROTATE", "Tamaño comprimido: " + tamanioComprimido + " KB");
                Log.d("COMPRESS_ROTATE", "Ahorro: " + ahorro + " KB (" +
                        String.format("%.1f", porcentajeAhorro) + "%)");
            }

            bitmap.recycle();

            Log.d("COMPRESS_ROTATE", "========== ✓ LISTO ==========");
            return resultado;

        } catch (Exception e) {
            Log.e("COMPRESS_ROTATE", "✗ Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ⚠️ ANTIGUO: Comprimir sin rotación (mantener para compatibilidad)
     * @deprecated Usar comprimirYRotarArchivoImagen() en su lugar
     */
    @Deprecated
    public static boolean comprimirArchivoImagen(String inputPath, String outputPath, int quality) {
        Log.w("COMPRESS", "comprimirArchivoImagen() es DEPRECATED. Usar comprimirYRotarArchivoImagen()");
        return comprimirYRotarArchivoImagen(inputPath, outputPath, quality);
    }

    /**
     * ✅ Obtener tamaño del archivo en MB
     * @param file Archivo
     * @return Tamaño en MB
     */
    public static double obtenerTamanoMB(File file) {
        if (file == null || !file.exists()) return 0;
        return (double) file.length() / (1024 * 1024);
    }

    /**
     * ✅ Obtener tamaño del archivo en KB
     * @param file Archivo
     * @return Tamaño en KB
     */
    public static long obtenerTamanoKB(File file) {
        if (file == null || !file.exists()) return 0;
        return file.length() / 1024;
    }

    /**
     * ✅ Obtener tamaño formateado (ej: "1.2 MB" o "850 KB")
     * @param file Archivo
     * @return String con tamaño formateado
     */
    public static String obtenerTamanoFormato(File file) {
        if (file == null || !file.exists()) return "0 KB";

        double sizeKB = (double) file.length() / 1024;

        if (sizeKB > 1024) {
            double sizeMB = sizeKB / 1024;
            return String.format("%.2f MB", sizeMB);
        } else {
            return String.format("%.0f KB", sizeKB);
        }
    }
}
