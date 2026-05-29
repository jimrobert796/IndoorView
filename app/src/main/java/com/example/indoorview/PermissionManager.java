package com.example.indoorview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private static PermissionManager instance;
    private PermissionCallback callback;
    private Activity currentActivity;

    // Códigos de solicitud
    public static final int RC_NOTIFICATIONS = 100;
    public static final int RC_CAMERA = 101;
    public static final int RC_GALLERY = 102;
    public static final int RC_LOCATION = 103;
    public static final int RC_ALL = 200;

    // Singleton
    private PermissionManager() {}

    public static synchronized PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    // Registrar el Activity actual (llamar en onCreate/resume de cada Activity)
    public void setCurrentActivity(Activity activity) {
        this.currentActivity = activity;
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    /**
     * Verificar si tiene permiso de NOTIFICACIONES
     */
    public boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Verificar si tiene permiso de CÁMARA
     */
    public boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Verificar si tiene permiso de GALERÍA
     */
    public boolean hasGalleryPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Verificar si tiene permiso de UBICACIÓN
     */
    public boolean hasLocationPermission(Context context) {

        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isGpsEnabled(Context context) {

        LocationManager locationManager =
                (LocationManager)
                        context.getSystemService(Context.LOCATION_SERVICE);

        return locationManager != null &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // ==================== PEDIR PERMISOS (diálogo nativo) ====================

    /**
     * Pedir permiso de NOTIFICACIONES (diálogo nativo del sistema)
     */
    public void requestNotificationPermission(Activity activity, PermissionCallback callback) {
        if (hasNotificationPermission(activity)) {
            if (callback != null) callback.onPermissionGranted(RC_NOTIFICATIONS);
            return;
        }

        this.callback = callback;
        this.currentActivity = activity;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                RC_NOTIFICATIONS);
    }

    /**
     * Pedir permiso de NOTIFICACIONES y UBICACIÓN juntos
     */
    /**
     * Pedir permiso de NOTIFICACIONES y UBICACIÓN juntos
     */
    public void requestNotificationAndLocationPermissions(
            Activity activity,
            PermissionCallback callback
    ) {

        boolean tieneNotificacion =
                hasNotificationPermission(activity);

        boolean tieneUbicacion =
                hasLocationPermission(activity);

        // ===============================
        // SI YA TIENE TODOS LOS PERMISOS
        // ===============================

        if (tieneNotificacion && tieneUbicacion) {

            // Verificar GPS
            if (!isGpsEnabled(activity)) {

                Toast.makeText(activity,
                        "Activa el GPS para continuar",
                        Toast.LENGTH_LONG).show();

                Intent intent = new Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                );

                activity.startActivity(intent);

                return;
            }

            // Todo correcto
            if (callback != null) {
                callback.onAllPermissionsGranted();
            }

            return;
        }

        // ===============================
        // PEDIR PERMISOS FALTANTES
        // ===============================

        this.callback = callback;
        this.currentActivity = activity;

        List<String> permisos = new ArrayList<>();

        // Notificaciones
        if (!tieneNotificacion &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permisos.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Ubicación
        if (!tieneUbicacion) {
            permisos.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        ActivityCompat.requestPermissions(
                activity,
                permisos.toArray(new String[0]),
                RC_ALL
        );
    }

    /**
     * Pedir permiso de CÁMARA y GALERÍA juntos (siempre necesarios para fotos)
     */
    public void requestCameraAndGalleryPermissions(Activity activity, PermissionCallback callback) {
        boolean tieneCamera = hasCameraPermission(activity);
        boolean tieneGaleria = hasGalleryPermission(activity);

        // Si ya tiene ambos
        if (tieneCamera && tieneGaleria) {
            if (callback != null) callback.onAllPermissionsGranted();
            return;
        }

        this.callback = callback;
        this.currentActivity = activity;

        List<String> permisos = new ArrayList<>();

        if (!tieneCamera) {
            permisos.add(Manifest.permission.CAMERA);
        }

        if (!tieneGaleria) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permisos.add(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                permisos.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        ActivityCompat.requestPermissions(activity,
                permisos.toArray(new String[0]),
                RC_ALL);
    }


    /**
     * Pedir permiso de CÁMARA (diálogo nativo del sistema)
     */
    public void requestCameraPermission(Activity activity, PermissionCallback callback) {
        if (hasCameraPermission(activity)) {
            if (callback != null) callback.onPermissionGranted(RC_CAMERA);
            return;
        }

        this.callback = callback;
        this.currentActivity = activity;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                RC_CAMERA);
    }

    /**
     * Pedir permiso de GALERÍA (diálogo nativo del sistema)
     */
    public void requestGalleryPermission(Activity activity, PermissionCallback callback) {
        if (hasGalleryPermission(activity)) {
            if (callback != null) callback.onPermissionGranted(RC_GALLERY);
            return;
        }

        this.callback = callback;
        this.currentActivity = activity;

        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        ActivityCompat.requestPermissions(activity,
                new String[]{permission},
                RC_GALLERY);
    }

    /**
     * Pedir permiso de UBICACIÓN (diálogo nativo del sistema)
     */
    public void requestLocationPermission(Activity activity, PermissionCallback callback) {
        if (hasLocationPermission(activity)) {
            if (callback != null) callback.onPermissionGranted(RC_LOCATION);
            return;
        }

        this.callback = callback;
        this.currentActivity = activity;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                RC_LOCATION);
    }

    /**
     * Pedir TODOS los permisos de una vez (diálogo nativo del sistema)
     */
    public void requestAllPermissions(
            Activity activity,
            PermissionCallback callback
    ) {

        List<String> permissionsList = new ArrayList<>();

        // ===============================
        // NOTIFICACIONES
        // ===============================

        if (!hasNotificationPermission(activity)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissionsList.add(
                    Manifest.permission.POST_NOTIFICATIONS
            );
        }

        // ===============================
        // CÁMARA
        // ===============================

        if (!hasCameraPermission(activity)) {
            permissionsList.add(
                    Manifest.permission.CAMERA
            );
        }

        // ===============================
        // GALERÍA
        // ===============================

        if (!hasGalleryPermission(activity)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                permissionsList.add(
                        Manifest.permission.READ_MEDIA_IMAGES
                );

            } else {

                permissionsList.add(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                );
            }
        }

        // ===============================
        // UBICACIÓN
        // ===============================

        if (!hasLocationPermission(activity)) {

            permissionsList.add(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        }

        // ===============================
        // SI FALTAN PERMISOS
        // ===============================

        if (!permissionsList.isEmpty()) {

            this.callback = callback;
            this.currentActivity = activity;

            ActivityCompat.requestPermissions(
                    activity,
                    permissionsList.toArray(new String[0]),
                    RC_ALL
            );

            return;
        }

        // ===============================
        // SI YA TIENE PERMISOS
        // VERIFICAR GPS
        // ===============================

        if (!isGpsEnabled(activity)) {

            Toast.makeText(activity,
                    "Activa el GPS para continuar",
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
            );

            activity.startActivity(intent);

            return;
        }

        // ===============================
        // TODO CORRECTO
        // ===============================

        if (callback != null) {
            callback.onAllPermissionsGranted();
        }
    }

    // ==================== MANEJAR RESULTADO ====================

    /**
     * LLamar este método en onRequestPermissionsResult de tu Activity/Fragment
     */
    public void handlePermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case RC_NOTIFICATIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) callback.onPermissionGranted(RC_NOTIFICATIONS);
                } else {
                    if (callback != null) callback.onPermissionDenied(RC_NOTIFICATIONS);
                }
                break;

            case RC_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) callback.onPermissionGranted(RC_CAMERA);
                } else {
                    if (callback != null) callback.onPermissionDenied(RC_CAMERA);
                }
                break;

            case RC_GALLERY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) callback.onPermissionGranted(RC_GALLERY);
                } else {
                    if (callback != null) callback.onPermissionDenied(RC_GALLERY);
                }
                break;

            case RC_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (callback != null) callback.onPermissionGranted(RC_LOCATION);
                } else {
                    if (callback != null) callback.onPermissionDenied(RC_LOCATION);
                }
                break;

            case RC_ALL:
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted && callback != null) {
                    callback.onAllPermissionsGranted();
                } else if (callback != null) {
                    callback.onSomePermissionsDenied(permissions, grantResults);
                }
                break;
        }
    }

    // ==================== INTERFACE CALLBACK ====================

    public interface PermissionCallback {
        void onPermissionGranted(int requestCode);
        void onPermissionDenied(int requestCode);

        // Métodos opcionales (con default)
        default void onAllPermissionsGranted() {}
        default void onSomePermissionsDenied(String[] permissions, int[] grantResults) {}
    }

    // Callback simplificado (opcional)
    public static abstract class SimpleCallback implements PermissionCallback {
        @Override
        public void onPermissionGranted(int requestCode) {}

        @Override
        public void onPermissionDenied(int requestCode) {}
    }
}