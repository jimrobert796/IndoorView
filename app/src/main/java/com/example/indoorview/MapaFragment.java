package com.example.indoorview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.maps.CameraBoundsOptions;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.CoordinateBounds;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.generated.FillLayer;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapaFragment extends Fragment {


    // ════════════════════════════════════════════════════════════════
    // VARIABLES DE MAPA Y UI
    // ════════════════════════════════════════════════════════════════
    private MapView mapView;
    private MapManager mapManager;
    private Database db;
    private SearchManager searchManager;
    private FirebaseHelper firebaseHelper;

    private DetectarInternet detectarInternet;

    // UI Elements
    private Button btnLugar, btnEspacios, btnCerrar, btnDeshacer, btnFinalizar, btnHabilitar;
    private TextView tvModo;
    private Spinner spinnerPisos;

    // ════════════════════════════════════════════════════════════════
    // MODOS DE OPERACIÓN
    // ════════════════════════════════════════════════════════════════
    private static final int MODO_NINGUNO = 0;
    private static final int MODO_LUGAR = 1;
    private static final int MODO_ESPACIO = 2;
    private int modoActual = MODO_NINGUNO;

    private boolean modoEdicionActivo = false;

    // Manager permanente para UGB pin
    private PointAnnotationManager managerPermanente;

    private ActivityResultLauncher<Intent> camaraLauncher;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    private EditText etBuscar;
    private ImageView btnLimpiarBusqueda;
    private RecyclerView rvResultados;
    private SearchResultAdapter searchAdapter;

    long idLugar;
    long pisoId;

    String NombLugar;
    String NombPiso;
    String NombEspacio;



    // CARGADOR DE DATOS PROCESADOS YA GUARDADOS EN BD PARA SINCRONIZCION AUTOMATICA DE MAPA
    private ObtenerProcesarDatos.OnDatosCargatosListener listener;
    // Variables para datos de sesión
    private boolean usuarioLog;
    private int usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private int usuarioTipo;
    private String usuarioCarnet;
    private String usuarioCorreo;


    private CloudinaryHelper cloudinaryHelper;
    private String imagenSubidaUrl = "";
    private boolean imagenSubidaExitosa = false;

    private Dialog loadingDialog; // Para la pantalla de carga


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        // UI
        mapView = view.findViewById(R.id.mapView);
        btnLugar = view.findViewById(R.id.btnEditar);
        btnEspacios = view.findViewById(R.id.btnEspacios);
        btnCerrar = view.findViewById(R.id.btnCerrar);
        btnDeshacer = view.findViewById(R.id.btnDeshacer);
        btnFinalizar = view.findViewById(R.id.btnFinalizar);
        btnHabilitar = view.findViewById(R.id.btnHabilitar);
        tvModo = view.findViewById(R.id.tvModo);
        spinnerPisos = view.findViewById(R.id.spnPisos);
        db = Database.getInstance(getActivity());

        // Para la busqueda
        etBuscar = view.findViewById(R.id.etBuscar);
        btnLimpiarBusqueda = view.findViewById(R.id.btnLimpiarBusqueda);
        rvResultados = view.findViewById(R.id.rvResultados);


        // OBTENER DATOS DE SESIÓN
        obtenerDatosSesion();

        // CONTROLAR VISIBILIDAD SEGÚN TIPO DE USUARIO
        if (usuarioTipo == 1) {  // Estudiante
            // Ocultar botones de edición
            btnHabilitar.setVisibility(View.GONE);
            btnLugar.setVisibility(View.GONE);
            btnEspacios.setVisibility(View.GONE);
            btnCerrar.setVisibility(View.GONE);
            btnDeshacer.setVisibility(View.GONE);
            btnFinalizar.setVisibility(View.GONE);
            tvModo.setVisibility(View.GONE);
        } else {  // Administrador
            btnHabilitar.setVisibility(View.VISIBLE);
            // Los demás botones se mostrarán según el flujo normal
        }



        // Inicaliza el mapa e eventos
        inicializarLaunchers();


        if (detectarInternet.hayConexionInternet()){


            loadingDialog = new Dialog(getContext());
            View loadingView = LayoutInflater.from(getContext()).inflate(R.layout.progress_loading, null);
            loadingDialog.setContentView(loadingView);  // ← ESTO FALTA
            loadingDialog.setCancelable(false);

            // Mostrar diálogo de carga
            loadingDialog.show();

            // Cambiar mensaje
            TextView tvMessage = loadingView.findViewById(R.id.tv_loading_message);
            tvMessage.setText("Cargando mapa...\nEsto puede tardar unos momentos");

            // SINCRONIZACIÓN AUTOMÁTICA AL INICIAR
            new android.os.Handler().postDelayed(() -> {
                sincronizarDatos();
            }, 1500); // Esperar 1.5s a que cargue la UI
        }

        mapManager.verificarConexionBD();
        searchManager = new SearchManager(db);
        spinnerPisos.setVisibility(View.GONE);

        // Se inicializa la busqueda que es algo que si o si debe estar
        configurarBusqueda();

        /* AHORITA NO USAREMOS ESO

        // Cargar datos automáticamente al abrir la app
        new android.os.Handler().postDelayed(() -> {
            cargarDatosDelServidor();
        }, 1000); // Esperar 1 segundo a que cargue

         */

        return view;
    }


    /**
     * Sincronizar datos desde Firebase a BD local
     */
    private void sincronizarDatos() {
        Log.d("SYNC_INICIO", "════════════════════════════════════════════");
        Log.d("SYNC_INICIO", "🔄 INICIANDO SINCRONIZACIÓN");
        Log.d("SYNC_INICIO", "════════════════════════════════════════════");

        // Verificar conexión
        DetectarInternet detector = new DetectarInternet(getContext());
        if (!detector.hayConexionInternet()) {
            Log.w("SYNC_INICIO", "⚠️ Sin conexión a internet");;
            return;
        }

        // Crear SyncManager
        SyncManager syncManager = new SyncManager(getContext(), db, firebaseHelper);

        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onProgress(String message, int progress, int total) {
                Log.d("SYNC_PROGRESO", message + " (" + progress + "/" + total + ")");
            }

            @Override
            public void onComplete(String message) {
                Log.d("SYNC_COMPLETO", "✅ " + message);
                Toast.makeText(getContext(), "✅ Sincronización completa", Toast.LENGTH_SHORT).show();

                // Recargar mapa con datos sincronizados
                new android.os.Handler().postDelayed(() -> {
                    loadingDialog.dismiss();
                    reInicarMapa();
                    mapManager.actualizarVisibilidadPinesPorZoom(0.0);
                }, 500);
            }

            @Override
            public void onError(String error) {
                Log.e("SYNC_ERROR", "❌ " + error);
                loadingDialog.dismiss();
                reInicarMapa();
            }
        });

        // Iniciar sincronización
        syncManager.syncAllMapWithClean();
    }


    /**
     * Mostrar un progress dialog (opcional)
     */
    private android.app.ProgressDialog progressDialog;

    private void mostrarProgressDialog(String mensaje) {
        if (progressDialog == null) {
            progressDialog = new android.app.ProgressDialog(getContext());
            progressDialog.setTitle("Sincronizando");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
        }

        progressDialog.setMessage(mensaje);
        progressDialog.show();
    }

    private void cerrarProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void inicializarLaunchers() {

        cloudinaryHelper = new CloudinaryHelper();
        firebaseHelper = new FirebaseHelper();
        detectarInternet = new DetectarInternet(getContext());




        // Launcher para la cámara
        camaraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (mapManager != null) {
                            mapManager.procesarResultadoCamara(result.getData());
                        }
                    } else {
                        Toast.makeText(getContext(), "Foto cancelada", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Launcher para la galería
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (mapManager != null) {
                            mapManager.procesarResultadoGaleria(result.getData());
                        }
                    } else {
                        Toast.makeText(getContext(), "No se seleccionó imagen", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Crear MapManager con los launchers
        mapManager = new MapManager(mapView, db, requireContext(), spinnerPisos,
                camaraLauncher, galeriaLauncher);

        //  NUEVO: Conectar listener de CRUD

        mapManager.setFlujoCRUDListener(new MapManager.OnFlujoCRUDListener() {
            // REEMPLAZA onLugarGuardado() en MapaFragment.java


            @Override
            public void onLugarGuardado(String nombre, String descripcion, String urlImagenes, String color) {
                // Convertir puntos a GeoJSON
                String geojson = obtenerGeojsonDePuntos(mapManager.puntosLugarActual);

                // Asignamos que el para firebase NombLugar
                setNombLugar(nombre);


                Log.d("FLUJO_CRUD", "════════════════════════════════════════════");
                Log.d("FLUJO_CRUD", "📍 onLugarGuardado() INICIADO");
                Log.d("FLUJO_CRUD", "  Nombre: " + nombre);
                Log.d("FLUJO_CRUD", "════════════════════════════════════════════");

                //GUARDAR EN BD LOCAL
                idLugar = db.insertLugar(nombre, descripcion, urlImagenes, geojson, color);
                crearPrimerPisoPorDefecto();
                flujoPostLugar();

                Log.d("FLUJO_CRUD", "✓ Lugar guardado en BD: " + nombre + " (ID: " + idLugar + ")");


                // DETECTAMOS SI HAY CONEXION A INTERNET PARA PROSEGUIR
                if (detectarInternet.hayConexionInternet()) {

                    //SUBIR A CLOUDINARY SI HAY IMÁGENES
                    try {
                        if (urlImagenes == null || urlImagenes.isEmpty()) {
                            Log.d("CLOUDINARY", "No hay imagenes que subir");
                        }else{
                            Log.d("CLOUDINARY", "Se encontraron imaganes a subir");
                        }

                        subirImagenesACloudinarySecuencial(urlImagenes, 0, new ArrayList<>(), new CloudinaryUploadCallback() {
                            @Override
                            public void onCompletado(String urlsCloudinary) {
                                Log.d("CLOUDINARY", "Imágenes subidas exitosamente: " + urlsCloudinary);

                                // Guardar en firebase y continuar
                                guardarLugarFirebase(nombre, descripcion, urlsCloudinary, color, geojson);

                                // Intentar guardar el piso automaticamente en firebase
                                guardarPisoDeffaultFirebase(nombre);

                            }

                            @Override
                            public void onError(String error) {
                                Log.e("CLOUDINARY", "Error: " + error);
                            }
                        });
                    } catch (Exception e) {
                        Log.e("CLOUDINARY", "Error: " + e.getMessage());
                    }
                }else {
                    Toast.makeText(getContext(), "Sin conexión a internet", Toast.LENGTH_SHORT).show();
                }
            }

            private void guardarLugarFirebase(String nombre, String descripcion, String urlImagenes,
                                                  String color, String geojson) {

                Log.d("PRUEBA_FIREBASE", "════════════════════════════════════════════");
                Log.d("PRUEBA_FIREBASE", "🚀 Guardando lugar en Firebase");
                Log.d("PRUEBA_FIREBASE", "  Nombre: " + nombre);
                Log.d("PRUEBA_FIREBASE", "  URL imágenes: " + urlImagenes);
                Log.d("PRUEBA_FIREBASE", "════════════════════════════════════════════");


                if (firebaseHelper != null) {
                    firebaseHelper.guardarLugarEnFirestore(
                            nombre,           // nombre
                            descripcion,      // descripción
                            urlImagenes,      // URLs (locales o Cloudinary)
                            color,            // color
                            geojson,          // geojson
                            nombre,           // lugarId
                            1,                // estado
                            new FirebaseHelper.FirebaseCallback() {
                                @Override
                                public void onSuccess(String mensaje) {
                                    Log.d("PRUEBA_FIREBASE", "✅✅✅ LUGAR GUARDADO EN FIREBASE ✅✅✅");
                                    Log.d("PRUEBA_FIREBASE", "  Mensaje: " + mensaje);
                                    Log.d("PRUEBA_FIREBASE", "  URL guardada: " + urlImagenes);

                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(),
                                                "✅ Lugar guardado en Firebase",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("PRUEBA_FIREBASE", " ERROR EN FIREBASE ");
                                    Log.e("PRUEBA_FIREBASE", "  Error: " + error);

                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(),
                                                "Error en Firebase: " + error,
                                                Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                    );
                } else {
                    Log.e("PRUEBA_FIREBASE", "❌ firebaseHelper es NULL");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ FirebaseHelper no inicializado", Toast.LENGTH_LONG).show();
                    });
                }
            }


            private void guardarPisoDeffaultFirebase(String nombre) {

                Log.d("PRUEBA_FIREBASE", "════════════════════════════════════════════");
                Log.d("PRUEBA_FIREBASE", "🚀 Guardando piso en Firebase");
                Log.d("PRUEBA_FIREBASE", "  Nombre: " + nombre);
                Log.d("PRUEBA_FIREBASE", "════════════════════════════════════════════");

                if (firebaseHelper != null) {
                    firebaseHelper.crearPisoEnFirestore(
                            nombre,           // Id
                            1,
                            "Primera Planta",
                            1,
                            new FirebaseHelper.FirebaseCallback() {
                                @Override
                                public void onSuccess(String mensaje) {
                                    Log.d("PRUEBA_FIREBASE", "✅✅✅ LUGAR GUARDADO EN FIREBASE ✅✅✅");
                                    Log.d("PRUEBA_FIREBASE", "  Mensaje: " + mensaje);

                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(),
                                                "✅ Lugar guardado en Firebase",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("PRUEBA_FIREBASE", " ERROR EN FIREBASE ");
                                    Log.e("PRUEBA_FIREBASE", "  Error: " + error);

                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(),
                                                "Error en Firebase: " + error,
                                                Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                    );
                } else {
                    Log.e("PRUEBA_FIREBASE", "❌ firebaseHelper es NULL");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ FirebaseHelper no inicializado", Toast.LENGTH_LONG).show();
                    });
                }
            }


            public void guardarPisoFirebase(String nombreLugar, int numero, String nombrePiso) {

                Log.d("PRUEBA_FIREBASE", "════════════════════════════════════════════");
                Log.d("PRUEBA_FIREBASE", "🚀 Guardando piso en Firebase");
                Log.d("PRUEBA_FIREBASE", "  Nombre: " + nombrePiso);
                Log.d("PRUEBA_FIREBASE", "════════════════════════════════════════════");

                if (firebaseHelper != null) {
                    firebaseHelper.crearPisoEnFirestore(
                            nombreLugar,           // Id
                            numero,
                            nombrePiso,
                            1,
                            new FirebaseHelper.FirebaseCallback() {
                                @Override
                                public void onSuccess(String mensaje) {
                                    Log.d("PRUEBA_FIREBASE", "✅✅✅ LUGAR GUARDADO EN FIREBASE ✅✅✅");
                                    Log.d("PRUEBA_FIREBASE", "  Mensaje: " + mensaje);

                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(),
                                                "✅ Lugar guardado en Firebase",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("PRUEBA_FIREBASE", " ERROR EN FIREBASE ");
                                    Log.e("PRUEBA_FIREBASE", "  Error: " + error);

                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(),
                                                "Error en Firebase: " + error,
                                                Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                    );
                } else {
                    Log.e("PRUEBA_FIREBASE", "❌ firebaseHelper es NULL");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "❌ FirebaseHelper no inicializado", Toast.LENGTH_LONG).show();
                    });
                }
            }


            private void guardarEspacioEnFirebase(String lugarId, String pisoId, String nombre, String descripcion, String urlImagenes,
                                                  String color, String verticesJson) {

                // Llamar a la función crearEspacioEnFirestore
                firebaseHelper.crearEspacioEnFirestore(
                        lugarId,      // lugarId (ej: "MiEdificio")
                        pisoId,     // pisoId (ej: "Primer Piso")
                        nombre,         // espacioId (ej: "Oficina 101")
                        nombre,         // nombre visible
                        descripcion,    // descripción
                        urlImagenes,    // URLs de las imágenes
                        1,              // estado (1 = activo)
                        new FirebaseHelper.FirebaseCallback() {
                            @Override
                            public void onSuccess(String mensaje) {
                                // ÉXITO: El espacio se guardó en Firebase
                                Log.d("EXITO", "Espacio guardado: " + mensaje);

                                // Aquí puedes guardar la geometría después del espacio
                                guardarGeometriaEnFirebase(lugarId, pisoId, nombre, verticesJson, color);
                            }

                            @Override
                            public void onError(String error) {
                                // ❌ ERROR: Falló al guardar en Firebase
                                Log.e("ERROR", "No se pudo guardar: " + error);
                            }
                        }
                );
            }

            // Función para guardar la geometría
            private void guardarGeometriaEnFirebase(String lugarId ,String pisoId ,String espacioId, String verticesJson,
                                                    String color) {
                firebaseHelper.guardarGeometriaEspacio(
                        lugarId,      // lugarId
                        pisoId,     // pisoId
                        espacioId,  // espacioId
                        verticesJson,   // GeoJSON
                        color,          // color
                        new FirebaseHelper.FirebaseCallback() {
                            @Override
                            public void onSuccess(String mensaje) {
                                Log.d("EXITO", "Geometría guardada: " + mensaje);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("ERROR", "Error geometría: " + error);
                            }
                        }
                );
            }



            private void crearPrimerPisoPorDefecto() {
                String nombrePiso = "Primera Planta";
                // Asignamos el nombre a la variable para su uso posterior
                setNombPiso(nombrePiso);
                int numeroPiso = 1;

                pisoId = db.insertPiso((int) idLugar, numeroPiso, nombrePiso);

                if (pisoId != -1) {
                    Log.d("FLUJO_PISO", "✓ Piso creado: " + nombrePiso + " (ID: " + pisoId + ")");
                    Toast.makeText(getContext(), "✓ Piso creado: " + nombrePiso, Toast.LENGTH_SHORT).show();

                    // ✅ CARGAR LOS PISOS EN EL SPINNER
                    mapManager.cargarPisos((int) idLugar);

                } else {
                    Log.e("FLUJO_PISO", "❌ Error al crear el piso");
                    Toast.makeText(getContext(), "Error al crear el piso", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onEspacioGuardado(String nombre, String descripcion, String urlImagenes, String color) {
                int id_creado = (int) idLugar;
                int idPiso = 1;

                // Asignamos el idNombre espácio
                setNombEspacio(nombre);


                if (spinnerPisos.getVisibility() == View.VISIBLE) {
                    List<Integer> pisosId = (List<Integer>) spinnerPisos.getTag();
                    if (pisosId != null && spinnerPisos.getSelectedItemPosition() >= 0) {
                        idPiso = pisosId.get(spinnerPisos.getSelectedItemPosition());
                    }
                }




                try {
                    // ════════════════════════════════════════════════════════════════
                    // 1. CREAR ESPACIO
                    // ════════════════════════════════════════════════════════════════

                    long espacioId = db.insertEspacio(id_creado, (int) pisoId, nombre, descripcion, urlImagenes);

                    if (espacioId == -1) {
                        Toast.makeText(getContext(), "Error al guardar espacio", Toast.LENGTH_SHORT).show();
                        Log.e("FLUJO_CRUD", "insertEspacio retornó -1");
                        return;
                    }

                    Log.d("FLUJO_CRUD", "✓ Espacio #" + espacioId + ": " + nombre);
                    Log.d("FLUJO_CRUD", "Nombre: "+ nombre);
                    Log.d("FLUJO_CRUD", "Descripcion: "+ descripcion);
                    Log.d("FLUJO_CRUD", "Url: "+ urlImagenes);
                    Log.d("FLUJO_CRUD", "color: "+ color);

                    // ════════════════════════════════════════════════════════════════
                    // 2. CREAR GEOMETRÍA
                    // ════════════════════════════════════════════════════════════════
                    String verticesJson = obtenerGeojsonDePuntos(mapManager.puntosEspacioActual);

                    int espac = (int) espacioId;

                    long geometriaId = db.insertGeometria(espac, id_creado, (int) pisoId, verticesJson, color);

                    if (geometriaId == -1) {
                        Toast.makeText(getContext(), "Error al guardar geometría", Toast.LENGTH_SHORT).show();
                        Log.e("FLUJO_CRUD", "insertGeometria retornó -1");
                        // NO return - el espacio ya está creado, al menos
                    }


                    // DETECTAMOS SI HAY CONEXION A INTERNET PARA PROSEGUIR
                    if (detectarInternet.hayConexionInternet()) {

                        // Verificar toca subir la el espacio con su geometria sabiendo su id verdad
                        try {
                            if (urlImagenes == null || urlImagenes.isEmpty()) {
                                Log.d("CLOUDINARY", "No hay imagenes que subir");
                            }else{
                                Log.d("CLOUDINARY", "Se encontraron imaganes a subir");
                            }
                            subirImagenesEspacioSecuencial(urlImagenes, nombre, 0, new ArrayList<>(), new CloudinaryUploadCallback() {
                                @Override
                                public void onCompletado(String urlsCloudinary) {
                                    Log.d("CLOUDINARY", "Imágenes de espacio subidas: " + urlsCloudinary);

                                    // Guardar en Firebase con URLs de Cloudinary
                                    guardarEspacioEnFirebase(getNombLugar(), getNombPiso(), getNombEspacio(), descripcion, urlsCloudinary, color, verticesJson);
                                }

                                @Override
                                public void onError(String error) {
                                    // Servidor o internet inexistente
                                    Log.e("CLOUDINARY", "❌ Error subiendo imágenes: " + error);

                                }
                            });
                        } catch (Exception e) {
                            // Sin imágenes o ya son de Cloudinary
                            Log.d("CLOUDINARY", "⚠️ Error: "+ e.getMessage());
                        }
                    }else {
                        Toast.makeText(getContext(), "Sin conexión a internet", Toast.LENGTH_SHORT).show();
                    }



                    Log.d("FLUJO_CRUD", "✓ Geometría #" + geometriaId + " | Color: " + color);
                    Log.d("FLUJO_CRUD", "Id espacio: "+ espac);
                    Log.d("FLUJO_CRUD", "Id lugar: "+ id_creado);
                    Log.d("FLUJO_CRUD", "Id piso: "+ pisoId);
                    Log.d("FLUJO_CRUD", "vertices: "+ verticesJson);
                    Log.d("FLUJO_CRUD", "color: "+ color);

                    // ════════════════════════════════════════════════════════════════
                    // 3. LOG FINAL Y CONTINUAR
                    // ════════════════════════════════════════════════════════════════
                    Log.d("FLUJO_CRUD", "════════════════════════════════════════════");
                    Log.d("FLUJO_CRUD", "Espacio creado exitosamente:");
                    Log.d("FLUJO_CRUD", "  ID Espacio: " + espacioId);
                    Log.d("FLUJO_CRUD", "  ID Lugar: " + id_creado);
                    Log.d("FLUJO_CRUD", "  ID Piso: " + idPiso);
                    Log.d("FLUJO_CRUD", "  Nombre: " + nombre);
                    Log.d("FLUJO_CRUD", "  Color: " + color);
                    Log.d("FLUJO_CRUD", "════════════════════════════════════════════");

                    Toast.makeText(getContext(), "✓ Espacio guardado: " + nombre, Toast.LENGTH_SHORT).show();

                    flujoPostEspacio();

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FLUJO_CRUD", "Excepción en onEspacioGuardado: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        // Lanzar metodos de configuraciones previas
        configurarSpinner(); // Preparar spinner
        configurarMapa(); // Preparar el mapa
        configurarListenerZoom(); // Preparar configuraciones de zooom
        configurarBotones(); // Funcionamiento de botones

        ocultarEdicion();
    }


    // ════════════════════════════════════════════════════════════════
    // 🔍 CONFIGURACIÓN DE BÚSQUEDA
    // ════════════════════════════════════════════════════════════════


    // Interfaz para callback de Cloudinary
    public interface CloudinaryUploadCallback {
        void onCompletado(String urlsCloudinary);
        void onError(String error);
    }

    private void subirImagenesEspacioSecuencial(String urlsLocales,
                                                String nombreEspacio,
                                                int index,
                                                List<String> resultados,
                                                CloudinaryUploadCallback callback) {
        if (urlsLocales == null || urlsLocales.isEmpty()) {
            if (callback != null) callback.onCompletado("");
            return;
        }

        String[] urls = urlsLocales.split(",");

        if (index >= urls.length) {
            String resultado = String.join(",", resultados);
            Log.d("CLOUDINARY_ESPACIO", "📦 SECUENCIAL - Espacio '" + nombreEspacio + "' completado: " + resultado);
            if (callback != null) callback.onCompletado(resultado);
            return;
        }

        String rutaLocal = urls[index].trim();

        if (rutaLocal.isEmpty()) {
            resultados.add("");
            subirImagenesEspacioSecuencial(urlsLocales, nombreEspacio, index + 1, resultados, callback);
            return;
        }

        File imagenFile = new File(rutaLocal);

        if (!imagenFile.exists()) {
            Log.e("CLOUDINARY_ESPACIO", "❌ Imagen no existe: " + rutaLocal);
            resultados.add(rutaLocal);
            subirImagenesEspacioSecuencial(urlsLocales, nombreEspacio, index + 1, resultados, callback);
            return;
        }

        Log.d("CLOUDINARY_ESPACIO", "📤 [SECUENCIAL] Subiendo imagen " + (index + 1) + " para: " + nombreEspacio);

        cloudinaryHelper.subirImagen(imagenFile, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onResult(boolean success, String url, String publicId) {
                if (success) {
                    Log.d("CLOUDINARY_ESPACIO", "✅ Imagen " + (index + 1) + " subida");
                    resultados.add(url);
                } else {
                    Log.e("CLOUDINARY_ESPACIO", "❌ Error imagen " + (index + 1));
                    resultados.add(rutaLocal);
                }
                // ✅ Subir SIGUIENTE (secuencial)
                subirImagenesEspacioSecuencial(urlsLocales, nombreEspacio, index + 1, resultados, callback);
            }
        });
    }

    // Método para subir imágenes a Cloudinary
    private void subirImagenesACloudinarySecuencial(String urlsLocales,
                                                    int index,
                                                    List<String> resultados,
                                                    CloudinaryUploadCallback callback) {
        if (urlsLocales == null || urlsLocales.isEmpty()) {
            if (callback != null) callback.onCompletado("");
            return;
        }

        String[] urls = urlsLocales.split(",");

        // Caso base: todas procesadas
        if (index >= urls.length) {
            String resultado = String.join(",", resultados);
            Log.d("CLOUDINARY_UPLOAD", "📦 SECUENCIAL - Todas subidas: " + resultado);
            if (callback != null) callback.onCompletado(resultado);
            return;
        }

        String rutaLocal = urls[index].trim();

        if (rutaLocal.isEmpty()) {
            resultados.add("");
            subirImagenesACloudinarySecuencial(urlsLocales, index + 1, resultados, callback);
            return;
        }

        File imagenFile = new File(rutaLocal);

        if (!imagenFile.exists()) {
            Log.e("CLOUDINARY_UPLOAD", "❌ Imagen no existe: " + rutaLocal);
            resultados.add(rutaLocal);
            subirImagenesACloudinarySecuencial(urlsLocales, index + 1, resultados, callback);
            return;
        }

        Log.d("CLOUDINARY_UPLOAD", "📤 [SECUENCIAL] Subiendo imagen " + (index + 1) + "/" + urls.length);

        cloudinaryHelper.subirImagen(imagenFile, new CloudinaryHelper.UploadCallback() {
            @Override
            public void onResult(boolean success, String url, String publicId) {
                if (success) {
                    Log.d("CLOUDINARY_UPLOAD", "✅ Imagen " + (index + 1) + " subida: " + url);
                    resultados.add(url);
                } else {
                    Log.e("CLOUDINARY_UPLOAD", "❌ Error imagen " + (index + 1));
                    resultados.add(rutaLocal);
                }
                // ✅ Subir SIGUIENTE (secuencial)
                subirImagenesACloudinarySecuencial(urlsLocales, index + 1, resultados, callback);
            }
        });
    }
    private void configurarBusqueda() {
        // Configurar RecyclerView
        rvResultados.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new SearchResultAdapter(new ArrayList<>(), getContext());
        rvResultados.setAdapter(searchAdapter);

        // Callback cuando se selecciona un resultado
        searchAdapter.setOnResultClickListener(result -> {
            onResultadoBusquedaSeleccionado(result);
        });

        // TextWatcher para cambios en el EditText
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                realizarBusqueda(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Botón limpiar
        btnLimpiarBusqueda.setOnClickListener(v -> {
            etBuscar.setText("");
            ocultarResultados();
        });

        // Mostrar/Ocultar botón limpiar según haya texto
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnLimpiarBusqueda.setVisibility(
                        s.length() > 0 ? View.VISIBLE : View.GONE
                );
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Realizar búsqueda con el texto ingresado
     */
    private void realizarBusqueda(String query) {
        Log.d("SEARCH_UI", "Buscando: " + query);

        if (query.trim().isEmpty()) {
            ocultarResultados();
            return;
        }

        List<SearchManager.SearchResult> resultados = searchManager.buscar(query);

        if (resultados.isEmpty()) {
            Log.d("SEARCH_UI", "Sin resultados");
            ocultarResultados();
            Toast.makeText(getContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar resultados
        mostrarResultados(resultados);
    }

    /**
     * Mostrar lista de resultados
     */
    private void mostrarResultados(List<SearchManager.SearchResult> resultados) {
        searchAdapter.actualizarResultados(resultados);
        rvResultados.setVisibility(View.VISIBLE);
        Log.d("SEARCH_UI", "Mostrando " + resultados.size() + " resultados");
    }

    /**
     * Ocultar lista de resultados
     */
    private void ocultarResultados() {
        rvResultados.setVisibility(View.GONE);
        searchManager.limpiar();
        searchAdapter.actualizarResultados(new ArrayList<>());
    }

    /**
     * Callback cuando se selecciona un resultado
     * Simula un click en el pin del lugar/espacio
     */
    private void onResultadoBusquedaSeleccionado(SearchManager.SearchResult result) {
        Log.d("SEARCH_SELECTED", "Resultado seleccionado: " + result.nombre + " (" + result.tipo + ")");

        // Limpiar búsqueda
        etBuscar.setText("");
        ocultarResultados();

        // Simular click en el pin
        if ("LUGAR".equals(result.tipo)) {
            // Simular click en lugar
            mapManager.seleccionarLugarPorBusqueda(result.jsonData);
        } else if ("ESPACIO".equals(result.tipo)) {
            // Simular click en espacio
            mapManager.seleccionarEspacioPorBusqueda(result.jsonData);
        }
    }


    //Configurar el spinner de pisos
    private void configurarSpinner() {
        spinnerPisos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                List<Integer> pisosId = (List<Integer>) spinnerPisos.getTag();

                if (pisosId != null && position < pisosId.size()) {
                    int pisoIdReal = pisosId.get(position);

                    if (mapManager.lugarSeleccionado != -1) {
                        mapManager.limpiarEspacios();
                        mapManager.mostrarEspaciosPorPiso(mapManager.lugarSeleccionado, pisoIdReal);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Configurar el mapa
    private void configurarMapa() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {

                    // Posicionar cámara
                    mapView.getMapboxMap().setCamera(
                            new CameraOptions.Builder()
                                    .center(Point.fromLngLat(-88.41783453298294, 13.342296805328829))
                                    .zoom(18.0)
                                    .build()
                    );

                    // Límites de zoom
                    mapView.getMapboxMap().setBounds(
                            new CameraBoundsOptions.Builder()
                                    .minZoom(16.0)
                                    .maxZoom(22.0)
                                    .build()
                    );

                    // Desactivar edificios por defecto de OpenStreetMap
                    style.setStyleLayerProperty(
                            "building",
                            "visibility",
                            Value.valueOf("none")
                    );
                    style.setStyleLayerProperty(
                            "building-extrusion",
                            "visibility",
                            Value.valueOf("none")
                    );

                    // Quitar lo que seria areas verdes e colores incecesarios del mapa en si
                    style.setStyleLayerProperty(
                            "landuse",
                            "visibility",
                            Value.valueOf("none")
                    );
                    style.setStyleLayerProperty(
                            "landuse-overlay",
                            "visibility",
                            Value.valueOf("none")
                    );style.setStyleLayerProperty(
                            "pitch-outline",
                            "visibility",
                            Value.valueOf("none")
                    );


            // cargar el los detalles
            mapManager.cargarLineStringDetalles();

            // Cargar polígonos de lugares
            mapManager.cargarPoligonosLugar();

            // Hacer que la visibilidad se desabilite por defaul
            mapManager.actualizarVisibilidadPinesPorZoom(0.0);

            // Crear manager permanente para UGB pin
            AnnotationPlugin annotationPlugin = mapView.getPlugin(Plugin.Mapbox.MAPBOX_ANNOTATION_PLUGIN_ID);
            managerPermanente = (PointAnnotationManager) annotationPlugin
                    .createAnnotationManager(AnnotationType.PointAnnotation, null);

            // Agregar pin UGB permanente para pruebas
            //agregarPinUGB();

            // Configurar click listener para agregar puntos
            configurarClickMapa(style);
        });
    }

    // NUEVO METODO: Configurar listener de cambios de zoom
    private void configurarListenerZoom() {
        mapView.getMapboxMap().addOnCameraChangeListener(cameraChangedCallback -> {
            // Obtener nivel de zoom actual
            double zoomActual = mapView.getMapboxMap().getCameraState().getZoom();

            Log.d("ZOOM_PINS", "Zoom actual: " + zoomActual);

            // Actualizar visibilidad de pines según zoom
            mapManager.actualizarVisibilidadPinesPorZoom(zoomActual);
        });
    }


    /**
     * Agregar pin UGB en el mapa SE utilizo para pruebas
    private void agregarPinUGB() {
        mapManager.agregarPinPermanente(
                Point.fromLngLat(-88.41783453298294, 13.342296805328829),
                "UGB", "#0080ff"
        );
    }
      */

    /**
     * Configurar listener de clicks en el mapa
     */
    private void configurarClickMapa(Style style) {
        GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
            if (modoActual == MODO_LUGAR) {
                // ✅ VALIDAR que el punto esté dentro del UGB
                if (!mapManager.puntoDentroDelUGB(point)) {
                    Toast.makeText(getContext(),
                            "⚠️ Este punto está FUERA del límite UGB permitido",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                mapManager.agregarPunto(point, style);
                tvModo.setText("Puntos: " + mapManager.obtenerCantidadPuntos() +
                        " — toca Cerrar para terminar");
                return true;

            } else if (modoActual == MODO_ESPACIO) {
                // ✅ VALIDAR que el punto esté dentro del UGB
                if (!mapManager.puntoDentroDelUGB(point)) {
                    Toast.makeText(getContext(),
                            "⚠️ Este punto está FUERA del límite UGB permitido",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (mapManager.puntoDentroDeLugar(point)) {
                    mapManager.agregarPunto(point, style);
                    tvModo.setText("Puntos: " + mapManager.obtenerCantidadPuntos() +
                            " — toca Cerrar para terminar");
                } else {
                    Toast.makeText(getContext(),
                            "El punto está fuera del lugar",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    // ════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE BOTONES
    // ════════════════════════════════════════════════════════════════

    /**
     * Configurar todos los listeners de botones
     */
    private void configurarBotones() {
        configurarBtnHabilitar();
        configurarBtnLugar();
        configurarBtnEspacios();
        configurarBtnCerrar();
        configurarBtnDeshacer();
        configurarBtnFinalizar();
    }

    /**
     * Botón HABILITAR - Activar/Desactivar modo edición
     */
    private void configurarBtnHabilitar() {
        if (usuarioTipo != 2) {
            btnHabilitar.setVisibility(View.GONE);
            return;
        }


        btnHabilitar.setOnClickListener(v -> {
            if (modoEdicionActivo && mapManager.isModoEdicion()) {
                mapManager.mostrarDialogoConfirmacion(
                        "Desabilitar Modo Edición",
                        "Estás a punto de desabilitar el modo edición.\n\n" +
                                "¿Deseas continuar?",
                        "Sí, desahabilitar",
                        () -> {
                            // Desactivar modo edición
                            desactivarModoEdicion();
                            reInicarMapa();

                        }
                );
            } else {
                if (detectarInternet.hayConexionInternet()){
                    mapManager.mostrarDialogoConfirmacion(
                            "Modo Edición",
                            "Estás a punto de activar el modo edición.\n\n" +
                                    "Podrás realizar los siguientes cambios:\n" +
                                    "• Agregar nuevos lugares\n" +
                                    "• Modificar información del lugar\n" +
                                    "• Cambiar el color del contorno\n" +
                                    "• Actualizar imágenes\n\n" +
                                    "Recuerda guardar los cambios antes de salir.\n\n" +
                                    "¿Deseas continuar?",
                            "Sí, activar",
                            () -> {
                                //ACTIVAR MODO EDICIÓN DENTRO DEL CALLBACK
                                activarModoEdicion();
                            }
                    );
                }else {
                    mostrarDialogoNecesitaConexion("habilitar el modo edicion");
                }

            }
        });
    }
    private void mostrarDialogoNecesitaConexion(String accion) {
        new AlertDialog.Builder(getContext())
                .setTitle("Conexión Requerida")
                .setMessage("Para " + accion + " necesitas tener conexión a internet.\n\n" +
                        "Por favor, verifica tu conexión y vuelve a intentarlo.")
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show();
    }


    /**
     * Botón LUGAR - Iniciar dibujo de lugar
     */
    private void configurarBtnLugar() {

        if (usuarioTipo != 2) {
            btnLugar.setVisibility(View.GONE);
            return;
        }


        btnLugar.setOnClickListener(v -> {
            if (modoActual == MODO_LUGAR) {
                // Cancelar modo lugar - con confirmación si hay puntos
                int puntosDibujados = mapManager.obtenerCantidadPuntos();

                if (puntosDibujados >= 0) {
                    mapManager.mostrarDialogoConfirmacion(
                            "Cancelar dibujo",
                            "Has dibujado " + puntosDibujados + " punto" +
                                    (puntosDibujados != 1 ? "s" : "") + ".\n\n" +
                                    "Si cancelas, perderás todo el progreso.\n\n" +
                                    "¿Deseas cancelar el dibujo del LUGAR?",
                            "Sí, cancelar",
                            () -> {
                                cancelarModo();
                                tvModo.setText("Dibujo de lugar cancelado");
                                Toast.makeText(getContext(), "Dibujo cancelado", Toast.LENGTH_SHORT).show();
                            }
                    );
                } else {
                    // Sin puntos, cancelar directamente
                    cancelarModo();
                    Toast.makeText(getContext(), "Modo lugar desactivado", Toast.LENGTH_SHORT).show();
                }

            } else if (modoActual == MODO_NINGUNO) {
                mapManager.mostrarDialogoConfirmacion(
                        "Agregar un lugar",
                        "Esta seguro de agregar un nuevo lugar en la institucion?",
                        "si",
                        () ->{
                            iniciarModoLugar();
                        }
                );

            } else if (modoActual == MODO_ESPACIO) {
                // Cambiar de espacio a lugar - con confirmación
                int puntosDibujados = mapManager.obtenerCantidadPuntos();
                String mensaje = puntosDibujados > 0
                        ? "Actualmente estás dibujando un ESPACIO con " + puntosDibujados + " punto" +
                        (puntosDibujados != 1 ? "s" : "") + ".\n\n" +
                        "Si cambias a LUGAR, perderás el progreso del espacio.\n\n" +
                        "¿Deseas cambiar a modo LUGAR?"
                        : "Actualmente estás en modo ESPACIO.\n\n¿Deseas cambiar a modo LUGAR?";

                mapManager.mostrarDialogoConfirmacion(
                        "Cambiar a LUGAR",
                        mensaje,
                        "Sí",
                        () -> {
                            cancelarModo();
                            iniciarModoLugar();
                        }
                );

            } else {
                // No hay modo activo - activar lugar
                // Verificar si estamos en modo edición
                if (!modoEdicionActivo) {
                    mapManager.mostrarDialogoConfirmacion(
                            "Modo Edición",
                            "Para dibujar un LUGAR, primero debes activar el modo edición.\n\n" +
                                    "¿Deseas activar el modo edición?",
                            "Sí, activar",
                            () -> {
                                activarModoEdicion();
                                // Después de activar, iniciar modo lugar
                                iniciarModoLugar();
                            }
                    );
                }
            }
        });
    }

    /**
     * Botón ESPACIOS - Iniciar dibujo de espacios
     */
    private void configurarBtnEspacios() {
        if (usuarioTipo != 2) {
            btnLugar.setVisibility(View.GONE);
            return;
        }


        btnEspacios.setOnClickListener(v -> {
            if (modoActual == MODO_ESPACIO) {
                // Cancelar modo espacios
                cancelarModo();
            } else {
                // Activar modo espacios
                iniciarModoEspacios();
            }
        });
    }

    /**
     * Botón CERRAR - Cerrar polígono (Lugar o Espacio)
     */
    private void configurarBtnCerrar() {
        btnCerrar.setOnClickListener(v -> {
            // Verificar si hay puntos suficientes
            if (mapManager.obtenerCantidadPuntos() < 3) {
                Toast.makeText(getContext(),
                        "Mínimo 3 puntos para finalizar.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String titulo = modoActual == MODO_LUGAR ? "Finalizar Lugar" : "Finalizar Espacio";
            String mensaje = modoActual == MODO_LUGAR
                    ? "¿Estás seguro de finalizar la geometría del LUGAR?\n\n" +
                    "Puntos dibujados: " + mapManager.obtenerCantidadPuntos()
                    : "¿Estás seguro de finalizar la geometría del ESPACIO?\n\n" +
                    "Puntos dibujados: " + mapManager.obtenerCantidadPuntos();

            mapManager.mostrarDialogoConfirmacion(
                    titulo,
                    mensaje,
                    "Sí, finalizar",
                    () -> {
                        mapView.getMapboxMap().getStyle(style -> {
                            if (modoActual == MODO_LUGAR) {
                                mapManager.cerrarLugar(style);
                            } else if (modoActual == MODO_ESPACIO) {
                                mapManager.cerrarEspacio(style);
                            }
                        });
                    }
            );
        });
    }

    /**
     * Botón DESHACER - Quitar último punto
     */
    private void configurarBtnDeshacer() {
        btnDeshacer.setOnClickListener(v -> {
            mapManager.deshacerPunto();
            tvModo.setText("Puntos: " + mapManager.obtenerCantidadPuntos());
        });
    }

    /**
     * Botón FINALIZAR - Terminar edición
     */
    private void configurarBtnFinalizar() {
        btnFinalizar.setOnClickListener(v -> {
            modoActual = MODO_NINGUNO;
            mapManager.limpiarVérticesTemporales();
            btnLugar.setText("Agregar Lugar");
            btnFinalizar.setVisibility(View.GONE);
            btnEspacios.setVisibility(View.GONE);
            tvModo.setText("¡Listo! Lugar y espacios guardados.");
            Toast.makeText(getContext(), "Mapa guardado correctamente", Toast.LENGTH_LONG).show();
        });
    }

    // ════════════════════════════════════════════════════════════════
    // LÓGICA DE MODOS
    // ════════════════════════════════════════════════════════════════

    /**
     * Activar modo edición
     */
    private void activarModoEdicion() {
        modoEdicionActivo = true;
        btnHabilitar.setText("DESABILITAR");
        mapManager.setModoEdicion(true);

        // Mostrar solo botón Lugar inicialmente
        btnLugar.setVisibility(View.VISIBLE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);

        tvModo.setText("Modo edición activado - Toca 'Lugar' para comenzar");
        Toast.makeText(getContext(), "Modo edición activado", Toast.LENGTH_SHORT).show();
    }

    /**
     * Desactivar modo edición
     */
    private void desactivarModoEdicion() {
        modoEdicionActivo = false;
        mapManager.setModoEdicion(false);
        btnHabilitar.setText("HABILITAR");

        // Ocultar todos los botones de edición
        ocultarEdicion();

        // Salir de cualquier modo activo
        if (modoActual != MODO_NINGUNO) {
            modoActual = MODO_NINGUNO;
            btnLugar.setText("Agregar Lugar");
            mapManager.limpiarVérticesTemporales();
        }

        tvModo.setText("");
        Toast.makeText(getContext(), "Modo edición desactivado", Toast.LENGTH_SHORT).show();
    }

    /**
     * Iniciar modo LUGAR
     */
    private void iniciarModoLugar() {
        modoActual = MODO_LUGAR;
        btnLugar.setText("Cancelar");
        btnEspacios.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.VISIBLE);
        btnDeshacer.setVisibility(View.VISIBLE);
        btnHabilitar.setEnabled(false);
        mapManager.limpiarVérticesTemporales();
        Toast.makeText(getContext(), "Dibuje por puntos", Toast.LENGTH_SHORT).show();
        tvModo.setText("Dibuja el LUGAR — toca el mapa punto a punto");
    }

    /**
     * Iniciar modo ESPACIOS
     */
    private void iniciarModoEspacios() {
        modoActual = MODO_ESPACIO;
        btnEspacios.setText("Salir espacios");
        btnCerrar.setVisibility(View.VISIBLE);
        btnDeshacer.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.GONE);
        mapManager.limpiarVérticesTemporales();

        mapManager.limpiarPinesTemporales();

        tvModo.setText("Dibuja ESPACIOS dentro del Lugar " + mapManager.obtenerLugarActualId());
    }
    /**
     * Cancelar modo actual
     */
    private void cancelarModo() {
        modoActual = MODO_NINGUNO;
        btnLugar.setText("Agregar Lugar");
        btnEspacios.setText("Espacios");
        btnHabilitar.setEnabled(true);
        // Ocultar botones de dibujo
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);

        // IMPORTANTE: Mantener visible Finalizar si estamos en modo edición
        if (!modoEdicionActivo) {
            btnFinalizar.setVisibility(View.GONE);
        }

        mapManager.limpiarVérticesTemporales();
        tvModo.setText("");
    }

    /**
     * Actualizar UI después de cerrar un lugar
     */
    private void actualizarUIAlCerrarLugar() {
        modoActual = MODO_NINGUNO;
        btnLugar.setText("Lugar");
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.VISIBLE);
        btnFinalizar.setVisibility(View.VISIBLE);
        tvModo.setText("Lugar guardado. Dibuja espacios o finaliza.");
    }

    // ════════════════════════════════════════════════════════════════
    // UTILIDADES DE UI
    // ════════════════════════════════════════════════════════════════

    /**
     * Ocultar todos los botones de edición
     */
    private void ocultarEdicion() {
        btnLugar.setVisibility(View.GONE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);
    }

    /**
     * Ocultar botones de edición (Cerrar y Deshacer)
     */
    private void ocultarBotonesEdicion() {
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
    }

    // ════════════════════════════════════════════════════════════════
// FLUJO: AGREGAR LUGAR CON ESPACIOS
// ════════════════════════════════════════════════════════════════

    /**
     * Llamado después de que cerrarLugar() completa
     * Muestra diálogo: ¿Deseas agregar espacios?
     */
    private void flujoPostLugar() {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Lugar Guardado")
                .setMessage("¿Deseas agregar espacios a este lugar?")
                .setPositiveButton("Sí", (d, w) -> iniciarModoEspacios())
                .setNegativeButton("No", (d, w) -> flujoPostEspacios_NO())
                .create();

        dialog.show();
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        pos.setTextColor(Color.parseColor("#2196F3"));
        neg.setTextColor(Color.parseColor("#2196F3"));
    }

    /**
     * Llamado después de cerrarEspacio()
     * Muestra diálogo: ¿Otro espacio?
     */
    private void flujoPostEspacio() {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Espacio Guardado")
                .setMessage("¿Deseas agregar otro espacio?")
                .setPositiveButton("Sí", (d, w) -> {
                    // Volver a dibujar otro espacio
                    modoActual = MODO_ESPACIO;
                    mapManager.limpiarVérticesTemporales();
                    tvModo.setText("Dibuja el siguiente espacio...");
                })
                .setNegativeButton("No", (d, w) -> flujoPostEspacios_NO())
                .create();

        dialog.show();
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        pos.setTextColor(Color.parseColor("#2196F3"));
        neg.setTextColor(Color.parseColor("#2196F3"));
    }

    /**
     * Si NO a "¿Agregar espacios?" o "¿Otro espacio?"
     * Pregunta si desea agregar otro piso
     */
    private void flujoPostEspacios_NO() {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Piso Completado")
                .setMessage("¿Deseas agregar otro piso a este lugar?")
                .setPositiveButton("Sí", (d, w) -> mostrarDialogoNuevoPiso())
                .setNegativeButton("No", (d, w) -> flujoFinalizar())
                .create();

        dialog.show();
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        pos.setTextColor(Color.parseColor("#2196F3"));
        neg.setTextColor(Color.parseColor("#2196F3"));
    }

    /**
     * Mostrar diálogo para crear un nuevo piso
     */
    private void mostrarDialogoNuevoPiso() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Nuevo Piso");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Mostrar información del próximo número de piso
        android.widget.TextView tvInfo = new android.widget.TextView(getContext());
        int nextNumero = obtenerSiguienteNumeroPiso();
        tvInfo.setText("Se creará el PISO #" + nextNumero);
        tvInfo.setTextColor(Color.parseColor("#2196F3"));
        tvInfo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvInfo.setPadding(55, 0, 0, 15);
        layout.addView(tvInfo);

        android.widget.TextView tvNombre = new android.widget.TextView(getContext());
        tvNombre.setText("Nombre del Piso o planta:");
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNombre.setPadding(20, 0, 0, 15);;
        layout.addView(tvNombre);

        android.widget.EditText etNombre = new android.widget.EditText(getContext());
        etNombre.setHint("Ej: Segundo Piso, Planta Alta, etc.");
        // Sugerir nombre automático
        etNombre.setText("Piso " + nextNumero);
        etNombre.setPadding(20, 0, 0, 20);
        layout.addView(etNombre);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa un nombre para el piso", Toast.LENGTH_SHORT).show();
                return;
            }

            // Asignamos el nombre del piso como id
            setNombPiso(nombre);

            int numero = obtenerSiguienteNumeroPiso();

            // Insertar piso
            pisoId = db.insertPiso((int) idLugar, numero, nombre);

            // INTENTAR MANDAR EL PISO A FIREBASE



            // Intentar mandarlo a firebase
            firebaseHelper.crearPisoEnFirestore(getNombLugar(), numero, getNombPiso(), 1, new FirebaseHelper.FirebaseCallback() {
                @Override
                public void onSuccess(String mensaje) {
                    Log.d("FIREBASE_PISO", "✅ ÉXITO: " + mensaje);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Piso guardado en Firebase", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("FIREBASE_PISO", "❌ ERROR: " + error);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "⚠️ Error al guardar piso: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

            if (pisoId != -1) {
                Log.d("FLUJO_PISO", "✓ Piso creado: " + nombre + " (#" + numero + ") ID: " + pisoId);
                Toast.makeText(getContext(), "✓ Piso creado: " + nombre + " (Piso " + numero + ")", Toast.LENGTH_SHORT).show();

                // Actualizar spinner de pisos
                mapManager.cargarPisos(mapManager.obtenerLugarActualId());


                mapManager.limpiarGeometriaTemporalEspacios();

                // 4LIMPIEZA DE PINS PERMANENTES (de dibujo)
                mapManager.limpiarPinesTemporales();

                // Volver a dibujar espacios en el nuevo piso
                iniciarModoEspacios();
            } else {
                Toast.makeText(getContext(), "Error al crear el piso", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        // Mostrar el diálogo y luego personalizar los colores de los botones
        AlertDialog dialog = builder.show();

        // AQUÍ ES DONDE DEBES AGREGAR LOS COLORES A LOS BOTONES
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        pos.setTextColor(Color.parseColor("#2196F3"));
        neg.setTextColor(Color.parseColor("#2196F3"));
    }

    // Metodo auxiliar para obtener el siguiente número de piso
    private int obtenerSiguienteNumeroPiso() {
        List<Pisos> pisosExistentes = db.getPisosByLugar((int) idLugar);
        int maxNumero = 0;

        for (Pisos piso : pisosExistentes) {
            if (piso.getNumero() > maxNumero) {
                maxNumero = piso.getNumero();
            }
        }

        return maxNumero + 1;
    }
    /**
     * Crear el primer piso por defecto automáticamente
     */
    private void crearPrimerPisoPorDefecto() {
        String nombrePiso = "Primera Planta";
        int numeroPiso = 1;

        pisoId = db.insertPiso((int) idLugar, numeroPiso, nombrePiso);

        if (pisoId != -1) {
            Log.d("FLUJO_PISO", "✓ Primer piso creado por defecto: " + nombrePiso + " (ID: " + pisoId + ")");
            Toast.makeText(getContext(), "✓ Piso creado: " + nombrePiso, Toast.LENGTH_SHORT).show();

            Log.d("FLUJO_CRUD", "✓ Piso #" + pisoId);
            Log.d("FLUJO_CRUD", "Id Piso: "+ pisoId);
            Log.d("FLUJO_CRUD", "Id lugar: "+ idLugar);
            Log.d("FLUJO_CRUD", "Nombre: "+ nombrePiso);

            // Actualizar spinner de pisos
            mapManager.cargarPisos((int) idLugar);


        } else {
            Log.e("FLUJO_PISO", "Error al crear el piso por defecto");
            Toast.makeText(getContext(), "Error al crear el piso", Toast.LENGTH_SHORT).show();
            flujoFinalizar();
        }
    }

    /**
     * Manejar la creación del primer piso (para cuando el usuario dice "No" a espacios)
     */

    /**
     * Finalizar todo el proceso
     */
    private void flujoFinalizar() {
        Log.d("FLUJO_FINALIZAR", "════════════════════════════════════════════");
        Log.d("FLUJO_FINALIZAR", "INICIANDO FLUJO DE FINALIZACIÓN");
        Log.d("FLUJO_FINALIZAR", "════════════════════════════════════════════");

        // Resetear estado de modos
        modoActual = MODO_NINGUNO;

        // LIMPIEZA COMPLETA DE GEOMETRÍA TEMPORAL
        mapManager.limpiarGeometriaTemporalCompleta();

        // LIMPIEZA DE PUNTOS TEMPORALES
        mapManager.limpiarVérticesTemporales();

        // 4LIMPIEZA DE PINS PERMANENTES (de dibujo)
        mapManager.limpiarPinesTemporales();

        // Re iniciar el mapa con los datos correctos
        reInicarMapa();

        // Actualizar UI
        btnLugar.setText("Agregar Lugar");
        btnHabilitar.setEnabled(true);
        btnLugar.setVisibility(View.VISIBLE);
        btnEspacios.setVisibility(View.GONE);
        btnCerrar.setVisibility(View.GONE);
        btnDeshacer.setVisibility(View.GONE);
        btnFinalizar.setVisibility(View.GONE);

        // Mantener modo edición activo (importante!)
        // NO desactivar: modoEdicionActivo se mantiene true

        // 7Mensajes al usuario
        tvModo.setText("✓ Lugar guardado. Toca 'Agregar Lugar' para agregar otro");
        Toast.makeText(getContext(), "✓ Lugar completado. Puedes agregar otro", Toast.LENGTH_LONG).show();

        Log.d("FLUJO_FINALIZAR", "════════════════════════════════════════════");
        Log.d("FLUJO_FINALIZAR", "FLUJO FINALIZADO - MODO EDICIÓN ACTIVO");
        Log.d("FLUJO_FINALIZAR", "════════════════════════════════════════════");
    }



    /**
     * Convertir lista de puntos a GeoJSON
     */
    private String obtenerGeojsonDePuntos(List<Point> puntos) {
        if (puntos == null || puntos.isEmpty()) {
            mapManager.mostrarDialogoConfirmacion("ERROR", "HEY ESTAN FALLANDO EL GEOJSON", "SI",null);

            return "[]";
        }

        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < puntos.size(); i++) {
            Point p = puntos.get(i);
            coords.append("[").append(p.longitude())
                    .append(",").append(p.latitude()).append("]");
            if (i < puntos.size() - 1) coords.append(",");
        }

        return "[[" + coords.toString() + "]]";
    }

    private int obtenerUltimoEspacioId(int idLugar) {
        List<Espacio> espacios = db.getEspaciosByLugar(idLugar);
        if (!espacios.isEmpty()) {
            return espacios.get(espacios.size() - 1).getId_espacio();
        }
        return -1;
    }

    private void reInicarMapa(){
        mapManager.limpiarGeometriaTemporalCompleta();
        mapManager.limpiarVérticesTemporales();
        mapManager.limpiarPinesTemporales();
        mapManager.limpiarEspacios();
        mapManager.limpiarLugares();
        mapManager.limpiarTodo();
        mapManager.cargarPoligonosLugar();
        mapManager.resetearContadores();
    }

    // Getters y setter de los nombres de ids
    // String NombLugar
    public String getNombLugar() {
        return NombLugar;
    }

    public void setNombLugar(String nombLugar) {
        this.NombLugar = nombLugar;
    }

    // String NombPiso
    public String getNombPiso() {
        return NombPiso;
    }

    public void setNombPiso(String nombPiso) {
        this.NombPiso = nombPiso;
    }

    // String NombEspacio
    public String getNombEspacio() {
        return NombEspacio;
    }

    public void setNombEspacio(String nombEspacio) {
        this.NombEspacio = nombEspacio;
    }



    // Obtener datos de la sesion
    private void obtenerDatosSesion() {
        // CORRECCIÓN: Usar requireContext() en lugar de getActivity()
        SharedPreferences prefs = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        // O también: getActivity().getSharedPreferences("sesion", Context.MODE_PRIVATE);

        usuarioLog = prefs.getBoolean("isLoggedIn", false);
        usuarioId = prefs.getInt("usuario_id", -1);
        usuarioNombre = prefs.getString("usuario_nombre", "");
        usuarioApellidos = prefs.getString("usuario_apellidos", "");
        usuarioTipo = prefs.getInt("usuario_tipo", 1);
        usuarioCarnet = prefs.getString("usuario_carnet", "");
        usuarioCorreo = prefs.getString("usuario_correo", "");
    }

    /*
    // Lifecycle methods (descomentar si necesitas)
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    */

}