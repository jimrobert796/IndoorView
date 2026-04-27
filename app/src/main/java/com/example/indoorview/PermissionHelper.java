package com.example.indoorview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Clase para manejar permisos de CÁMARA y ALMACENAMIENTO
 * Compatible con Android 6.0+ (API 23+)
 */
public class PermissionHelper {

    // Códigos de solicitud únicos
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 101;
    private static final int PERMISSION_REQUEST_CODE_STORAGE = 102;
    private static final int PERMISSION_REQUEST_CODE_ALL = 103;

    // ════════════════════════════════════════════════════════════════
    // VERIFICAR PERMISOS
    // ════════════════════════════════════════════════════════════════

    /**
     * Verificar si tiene permiso de CÁMARA
     */
    public static boolean tieneCamara(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Verificar si tiene permiso de ALMACENAMIENTO (lectura)
     */
    public static boolean tieneAlmacenamientoLectura(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: READ_MEDIA_IMAGES
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 6-12: READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Verificar si tiene permiso de ALMACENAMIENTO (escritura)
     */
    public static boolean tieneAlmacenamientoEscritura(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: No necesita WRITE_EXTERNAL_STORAGE si usa app-specific storage
            // Pero si quieres acceso general, aquí está
            return true; // O verificar si es scoped storage
        } else {
            // Android 6-9: WRITE_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Verificar si tiene AMBOS permisos (cámara y almacenamiento)
     */
    public static boolean tienePermisosCombinados(Context context) {
        return tieneCamara(context) && tieneAlmacenamientoLectura(context);
    }

    // ════════════════════════════════════════════════════════════════
    // SOLICITAR PERMISOS (Fragment)
    // ════════════════════════════════════════════════════════════════

    /**
     * Solicitar permiso de CÁMARA desde un Fragment
     */
    public static void solicitarCameraFragment(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!tieneCamara(fragment.requireContext())) {
                ActivityCompat.requestPermissions(
                        fragment.requireActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CODE_CAMERA
                );
            } else {
                Toast.makeText(fragment.requireContext(),
                        "✓ Permiso de cámara ya concedido",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Solicitar permiso de ALMACENAMIENTO desde un Fragment
     */
    public static void solicitarAlmacenamientoFragment(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!tieneAlmacenamientoLectura(fragment.requireContext())) {
                String[] permisos;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permisos = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
                } else {
                    permisos = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                }

                ActivityCompat.requestPermissions(
                        fragment.requireActivity(),
                        permisos,
                        PERMISSION_REQUEST_CODE_STORAGE
                );
            } else {
                Toast.makeText(fragment.requireContext(),
                        "✓ Permiso de almacenamiento ya concedido",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Solicitar AMBOS permisos (cámara + almacenamiento) desde un Fragment
     */
    public static void solicitarTodosFragment(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permisos;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                permisos = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                };
            } else {
                // Android 6-12
                permisos = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
            }

            ActivityCompat.requestPermissions(
                    fragment.requireActivity(),
                    permisos,
                    PERMISSION_REQUEST_CODE_ALL
            );
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SOLICITAR PERMISOS (Activity)
    // ════════════════════════════════════════════════════════════════

    /**
     * Solicitar permiso de CÁMARA desde una Activity
     */
    public static void solicitarCameraActivity(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!tieneCamara(activity)) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CODE_CAMERA
                );
            } else {
                Toast.makeText(activity,
                        "✓ Permiso de cámara ya concedido",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Solicitar permiso de ALMACENAMIENTO desde una Activity
     */
    public static void solicitarAlmacenamientoActivity(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!tieneAlmacenamientoLectura(activity)) {
                String[] permisos;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permisos = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
                } else {
                    permisos = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                }

                ActivityCompat.requestPermissions(
                        activity,
                        permisos,
                        PERMISSION_REQUEST_CODE_STORAGE
                );
            } else {
                Toast.makeText(activity,
                        "✓ Permiso de almacenamiento ya concedido",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Solicitar AMBOS permisos desde una Activity
     */
    public static void solicitarTodosActivity(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permisos;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permisos = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                };
            } else {
                permisos = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
            }

            ActivityCompat.requestPermissions(
                    activity,
                    permisos,
                    PERMISSION_REQUEST_CODE_ALL
            );
        }
    }

    // ════════════════════════════════════════════════════════════════
    // VERIFICAR RESULTADOS DE PERMISOS
    // ════════════════════════════════════════════════════════════════

    /**
     * Procesar resultados de solicitud de permisos
     * Llamar esta desde onRequestPermissionsResult() de Activity/Fragment
     */
    public static void procesarResultadoPermisos(
            int requestCode,
            String[] permissions,
            int[] grantResults,
            OnPermissionCallback callback
    ) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) {
                        callback.onPermissionGranted("CAMERA");
                    }
                } else {
                    if (callback != null) {
                        callback.onPermissionDenied("CAMERA");
                    }
                }
                break;

            case PERMISSION_REQUEST_CODE_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) {
                        callback.onPermissionGranted("STORAGE");
                    }
                } else {
                    if (callback != null) {
                        callback.onPermissionDenied("STORAGE");
                    }
                }
                break;

            case PERMISSION_REQUEST_CODE_ALL:
                boolean cameraConcedido = grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean storageConcedido = grantResults.length > 1
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (cameraConcedido && storageConcedido) {
                    if (callback != null) {
                        callback.onPermissionGranted("ALL");
                    }
                } else if (cameraConcedido) {
                    if (callback != null) {
                        callback.onPermissionPartial("CAMERA_GRANTED");
                    }
                } else if (storageConcedido) {
                    if (callback != null) {
                        callback.onPermissionPartial("STORAGE_GRANTED");
                    }
                } else {
                    if (callback != null) {
                        callback.onPermissionDenied("ALL");
                    }
                }
                break;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // INTERFAZ DE CALLBACK
    // ════════════════════════════════════════════════════════════════

    /**
     * Interface para manejar respuestas de permisos
     */
    public interface OnPermissionCallback {
        /**
         * Permiso concedido
         * @param tipo "CAMERA", "STORAGE" o "ALL"
         */
        void onPermissionGranted(String tipo);

        /**
         * Permiso denegado
         * @param tipo "CAMERA", "STORAGE" o "ALL"
         */
        void onPermissionDenied(String tipo);

        /**
         * Algunos permisos concedidos, otros denegados
         * @param tipo "CAMERA_GRANTED" o "STORAGE_GRANTED"
         */
        void onPermissionPartial(String tipo);
    }

    // ════════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ════════════════════════════════════════════════════════════════

    /**
     * Mostrar diálogo informativo sobre por qué se necesitan los permisos
     */
    public static void mostrarExplicacionPermisos(Context context) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Permisos necesarios")
                .setMessage("Esta aplicación necesita:\n\n" +
                        "CÁMARA - Para tomar fotos de lugares y espacios\n\n" +
                        "ALMACENAMIENTO - Para acceder a la galería e imágenes\n\n" +
                        "Estos permisos son esenciales para el funcionamiento de la app.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    /**
     * Obtener el nombre legible del permiso
     */
    public static String obtenerNombrePermiso(String permiso) {
        switch (permiso) {
            case Manifest.permission.CAMERA:
                return "Cámara";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Acceso a almacenamiento";
            case Manifest.permission.READ_MEDIA_IMAGES:
                return "Acceso a imágenes";
            default:
                return "Permiso desconocido";
        }
    }

    /**
     * Verificar si el usuario puede solicitar permiso de nuevo
     * (devuelve false si el usuario marcó "No volver a preguntar")
     */
    public static boolean puedeVolverASolicitarPermiso(Activity activity, String permiso) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activity.shouldShowRequestPermissionRationale(permiso);
        }
        return false;
    }

    /**
     * Obtener lista de permisos faltantes
     */
    public static String[] obtenerPermisosFaltantes(Context context) {
        if (!tieneCamara(context) && !tieneAlmacenamientoLectura(context)) {
            // Ambos faltantes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                };
            } else {
                return new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
            }
        } else if (!tieneCamara(context)) {
            // Solo falta cámara
            return new String[]{Manifest.permission.CAMERA};
        } else if (!tieneAlmacenamientoLectura(context)) {
            // Solo falta almacenamiento
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            } else {
                return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            }
        }
        return new String[]{};
    }
}