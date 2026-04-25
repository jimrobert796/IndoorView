package com.example.indoorview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Eventos;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;
import com.example.indoorview.models.Usuarios;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class Database extends SQLiteOpenHelper {

    private static final String DB_NAME = "db_indoorView_pruebas.db";
    private static final int DB_VERSION = 1;
    private static Database instancia;
    private final Context context;

    public static Database getInstance(Context context) {
        if (instancia == null) {
            instancia = new Database(context.getApplicationContext());
        }
        return instancia;
    }

    private Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    // IMPORTANTE: onCreate NO crea la BD, solo la copia desde assets
    @Override
    public void onCreate(SQLiteDatabase db) {
        // No crear tablas, solo copiar BD existente
        Log.d("Database", "onCreate: copiando BD desde assets");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Para futuras migraciones
        Log.d("Database", "onUpgrade: actualizando BD");
    }

    //  Mtodo para copiar la BD desde assets (se llama al abrir la BD)
    private void copyDatabaseFromAssets() {
        File dbFile = context.getDatabasePath(DB_NAME);

        // Si la BD ya existe, no copiar
        if (dbFile.exists()) {
            Log.d("Database", "BD ya existe en: " + dbFile.getAbsolutePath());
            return;
        }

        try {
            // Crear carpeta si no existe
            dbFile.getParentFile().mkdirs();

            // Copiar desde assets
            InputStream is = context.getAssets().open("databases/" + DB_NAME);
            FileOutputStream os = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            os.flush();
            os.close();
            is.close();

            Log.d("Database", "BD copiada exitosamente desde assets");

        } catch (IOException e) {
            Log.e("Database", "Error al copiar BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //  Sobrescribir getWritableDatabase para copiar BD antes de abrir
    @Override
    public SQLiteDatabase getWritableDatabase() {
        copyDatabaseFromAssets();
        return super.getWritableDatabase();
    }

    //  Sobrescribir getReadableDatabase para copiar BD antes de abrir
    @Override
    public SQLiteDatabase getReadableDatabase() {
        copyDatabaseFromAssets();
        return super.getReadableDatabase();
    }

    // CRUD - Lugar
    public long insertLugar(Lugar lugar) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", lugar.getNombre());
        cv.put("descripcion", lugar.getDescripcion());
        cv.put("url_imagenes", lugar.getUrl_imagenes());
        cv.put("geojson", lugar.getGeojson());
        cv.put("estado", 1);
        return db.insert("lugar", null, cv);
    }

    public List<Lugar> getLugares() {
        List<Lugar> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM lugar WHERE estado = 1", null);
        while (c.moveToNext()) {
            Lugar l = new Lugar(
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getString(c.getColumnIndexOrThrow("descripcion")),
                    c.getString(c.getColumnIndexOrThrow("url_imagenes")),
                    c.getString(c.getColumnIndexOrThrow("geojson")),
                    c.getString(c.getColumnIndexOrThrow("color")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(l);
        }
        c.close();
        return lista;
    }

    // Actualizar Lugar
    public int updateLugar(int id, String nombre, String descripcion, String color, String url_imagenes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("descripcion", descripcion);
        cv.put("color", color);
        cv.put("url_imagenes", url_imagenes);  // Añadir las URLs de imágenes
        // Nota: No modificamos url_imagenes ni geojson aquí
        return db.update("lugar", cv, "id_lugar = ? AND estado = 1", new String[]{String.valueOf(id)});
    }


    // ─────────────────────────────────────────
    // CRUD - Pisos
    // ─────────────────────────────────────────
    public List<Pisos> getPisosByLugar(int idLugar) {
        List<Pisos> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM pisos WHERE id_lugar = ? AND activo = 1 ORDER BY numero",
                new String[]{String.valueOf(idLugar)}
        );
        while (c.moveToNext()) {
            Pisos p = new Pisos(
                    c.getInt(c.getColumnIndexOrThrow("id_piso")),
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getInt(c.getColumnIndexOrThrow("numero")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getInt(c.getColumnIndexOrThrow("activo"))
            );
            lista.add(p);
        }
        c.close();
        return lista;
    }

    // ─────────────────────────────────────────
    // CRUD - Espacio
    // ─────────────────────────────────────────
    public long insertEspacio(Espacio espacio) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_lugar", espacio.getId_lugar());
        cv.put("id_piso", espacio.getId_piso());
        cv.put("nombre", espacio.getNombre());
        cv.put("descripcion", espacio.getDescripcion());
        cv.put("url_imagenes", espacio.getUrl_imagenes());
        cv.put("estado", 1);
        return db.insert("espacio", null, cv);
    }


    public Espacio getEspacioById(int idEspacio) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM espacio WHERE id_espacio = ? AND estado = 1",
                new String[]{String.valueOf(idEspacio)}
        );

        Espacio espacio = null;

        if (c.moveToFirst()) {
            espacio = cursorToEspacio(c);
        }

        c.close();

        return espacio;
    }

    public List<Espacio> getEspaciosByLugar(int idLugar) {
        List<Espacio> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM espacio WHERE id_lugar = ? AND estado = 1",
                new String[]{String.valueOf(idLugar)}
        );
        while (c.moveToNext()) {
            lista.add(cursorToEspacio(c));
        }
        c.close();
        return lista;
    }


    public List<Espacio> getEspaciosByPiso(int idLugar, int idPiso) {
        List<Espacio> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM espacio WHERE id_lugar = ? AND id_piso = ? AND estado = 1",
                new String[]{String.valueOf(idLugar), String.valueOf(idPiso)}
        );
        while (c.moveToNext()) {
            lista.add(cursorToEspacio(c));
        }
        c.close();
        return lista;
    }

    private Espacio cursorToEspacio(Cursor c) {
        Espacio e = new Espacio(
                c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                c.getInt(c.getColumnIndexOrThrow("id_piso")),
                c.getString(c.getColumnIndexOrThrow("nombre")),
                c.getString(c.getColumnIndexOrThrow("descripcion")),
                c.getString(c.getColumnIndexOrThrow("url_imagenes")),
                c.getInt(c.getColumnIndexOrThrow("estado"))
        );
        return e;
    }
    public int updateEspacio(int idEspacio, String nombre, String descripcion, String url_imagenes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("descripcion", descripcion);
        cv.put("url_imagenes", url_imagenes);

        return db.update("espacio", cv, "id_espacio = ? AND estado = 1",
                new String[]{String.valueOf(idEspacio)});
    }

    // Versión completa si necesitas actualizar más campos
    public int updateEspacioCompleto(int idEspacio, int idLugar, int idPiso,
                                     String nombre, String descripcion,
                                     String urlImagenes, String color) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_lugar", idLugar);
        cv.put("id_piso", idPiso);
        cv.put("nombre", nombre);
        cv.put("descripcion", descripcion);
        cv.put("url_imagenes", urlImagenes);
        cv.put("color", color);

        return db.update("espacio", cv, "id_espacio = ? AND estado = 1",
                new String[]{String.valueOf(idEspacio)});
    }

    // ─────────────────────────────────────────
    // CRUD - Geometria
    // ─────────────────────────────────────────
    public long insertGeometria(Geometria geo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_espacio", geo.getId_espacio());
        cv.put("id_lugar", geo.getId_lugar());
        cv.put("id_piso", geo.getId_piso());
        cv.put("tipo", geo.getTipo());
        cv.put("vertices", geo.getVertices());
        cv.put("color", geo.getColor());
        return db.insert("geometria", null, cv);
    }

    public Geometria getGeometriaByLugar(int idLugar) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM geometria WHERE id_lugar = ? AND id_espacio IS NULL",
                new String[]{String.valueOf(idLugar)}
        );
        Geometria geo = null;
        if (c.moveToFirst()) geo = cursorToGeometria(c);
        c.close();
        return geo;
    }

    public Geometria getGeometriaByEspacio(int idEspacio) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM geometria WHERE id_espacio = ?",
                new String[]{String.valueOf(idEspacio)}
        );
        Geometria geo = null;
        if (c.moveToFirst()) geo = cursorToGeometria(c);
        c.close();
        return geo;
    }

    private Geometria cursorToGeometria(Cursor c) {
        Geometria g = new Geometria(
            c.getInt(c.getColumnIndexOrThrow("id_geometria")),
            c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                c.getInt(c.getColumnIndexOrThrow("id_lugar")),
            c.getInt(c.getColumnIndexOrThrow("id_piso")),
            c.getString(c.getColumnIndexOrThrow("tipo")),
            c.getString(c.getColumnIndexOrThrow("vertices")),
            c.getString(c.getColumnIndexOrThrow("color"))
        );
        return g;
    }

    // Actualizar color de una geometría
    public int updateGeometriaColor(int idGeometria, String color) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("color", color);

        return db.update("geometria", cv, "id_geometria = ?",
                new String[]{String.valueOf(idGeometria)});
    }

    // Actualizar geometría completa
    public int updateGeometria(int idGeometria, String tipo, String vertices, String color) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("tipo", tipo);
        cv.put("vertices", vertices);
        cv.put("color", color);

        return db.update("geometria", cv, "id_geometria = ?",
                new String[]{String.valueOf(idGeometria)});
    }

    // Obtener geometrías por id_espacio
    public List<Geometria> getGeometriasByEspacio(int idEspacio) {
        List<Geometria> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM geometria WHERE id_espacio = ?",
                new String[]{String.valueOf(idEspacio)});
        while (c.moveToNext()) {
            lista.add(cursorToGeometria(c));
        }
        c.close();
        return lista;
    }

    // Obtener geometrías por id_lugar
    public List<Geometria> getGeometriasByLugar(int idLugar) {
        List<Geometria> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM geometria WHERE id_lugar = ?",
                new String[]{String.valueOf(idLugar)});
        while (c.moveToNext()) {
            lista.add(cursorToGeometria(c));
        }
        c.close();
        return lista;
    }

    // ==========================================
// CRUD - EVENTOS
// ==========================================

    // 1. INSERTAR evento
    public long insertarEvento(Eventos evento) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_lugar", evento.getId_lugar());
        cv.put("id_espacio", evento.getId_espacio());
        cv.put("nombre", evento.getNombre());
        cv.put("descripcion", evento.getDescripcion());
        cv.put("latitud", evento.getLatitud());
        cv.put("longitud", evento.getLongitud());
        cv.put("fecha_inicio", evento.getFecha_inicio());
        cv.put("fecha_fin", evento.getFecha_fin());
        cv.put("estado", evento.getEstado());

        long id = db.insert("eventos", null, cv);
        db.close();
        return id;
    }

    // 2. OBTENER todos los eventos activos
    public List<Eventos> getEventos() {
        List<Eventos> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM eventos WHERE estado = 1", null);

        while (c.moveToNext()) {
            Eventos e = new Eventos(
                    c.getInt(c.getColumnIndexOrThrow("id_evento")),
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getString(c.getColumnIndexOrThrow("descripcion")),
                    c.getString(c.getColumnIndexOrThrow("latitud")),
                    c.getString(c.getColumnIndexOrThrow("longitud")),
                    c.getString(c.getColumnIndexOrThrow("fecha_inicio")),
                    c.getString(c.getColumnIndexOrThrow("fecha_fin")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(e);
        }
        c.close();
        db.close();
        return lista;
    }

    // 3. OBTENER evento por ID
    public Eventos getEventoById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM eventos WHERE id_evento = ?",
                new String[]{String.valueOf(id)});

        Eventos evento = null;
        if (c.moveToFirst()) {
            evento = new Eventos(
                    c.getInt(c.getColumnIndexOrThrow("id_evento")),
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getString(c.getColumnIndexOrThrow("descripcion")),
                    c.getString(c.getColumnIndexOrThrow("latitud")),
                    c.getString(c.getColumnIndexOrThrow("longitud")),
                    c.getString(c.getColumnIndexOrThrow("fecha_inicio")),
                    c.getString(c.getColumnIndexOrThrow("fecha_fin")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
        }
        c.close();
        db.close();
        return evento;
    }

    // 4. OBTENER eventos por lugar (edificio)
    public List<Eventos> getEventosByLugar(int idLugar) {
        List<Eventos> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM eventos WHERE id_lugar = ? AND estado = 1",
                new String[]{String.valueOf(idLugar)});

        while (c.moveToNext()) {
            Eventos e = new Eventos(
                    c.getInt(c.getColumnIndexOrThrow("id_evento")),
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getString(c.getColumnIndexOrThrow("descripcion")),
                    c.getString(c.getColumnIndexOrThrow("latitud")),
                    c.getString(c.getColumnIndexOrThrow("longitud")),
                    c.getString(c.getColumnIndexOrThrow("fecha_inicio")),
                    c.getString(c.getColumnIndexOrThrow("fecha_fin")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(e);
        }
        c.close();
        db.close();
        return lista;
    }

    // 5. OBTENER eventos por espacio (aula)
    public List<Eventos> getEventosByEspacio(int idEspacio) {
        List<Eventos> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM eventos WHERE id_espacio = ? AND estado = 1",
                new String[]{String.valueOf(idEspacio)});

        while (c.moveToNext()) {
            Eventos e = new Eventos(
                    c.getInt(c.getColumnIndexOrThrow("id_evento")),
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getString(c.getColumnIndexOrThrow("descripcion")),
                    c.getString(c.getColumnIndexOrThrow("latitud")),
                    c.getString(c.getColumnIndexOrThrow("longitud")),
                    c.getString(c.getColumnIndexOrThrow("fecha_inicio")),
                    c.getString(c.getColumnIndexOrThrow("fecha_fin")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(e);
        }
        c.close();
        db.close();
        return lista;
    }

    // 6. OBTENER eventos actuales (fecha_inicio <= hoy <= fecha_fin)
    public List<Eventos> getEventosActuales() {
        List<Eventos> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        long hoy = System.currentTimeMillis();

        Cursor c = db.rawQuery("SELECT * FROM eventos WHERE estado = 1 AND fecha_inicio <= ? AND fecha_fin >= ?",
                new String[]{String.valueOf(hoy), String.valueOf(hoy)});

        while (c.moveToNext()) {
            Eventos e = new Eventos(
                    c.getInt(c.getColumnIndexOrThrow("id_evento")),
                    c.getInt(c.getColumnIndexOrThrow("id_lugar")),
                    c.getInt(c.getColumnIndexOrThrow("id_espacio")),
                    c.getString(c.getColumnIndexOrThrow("nombre")),
                    c.getString(c.getColumnIndexOrThrow("descripcion")),
                    c.getString(c.getColumnIndexOrThrow("latitud")),
                    c.getString(c.getColumnIndexOrThrow("longitud")),
                    c.getString(c.getColumnIndexOrThrow("fecha_inicio")),
                    c.getString(c.getColumnIndexOrThrow("fecha_fin")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(e);
        }
        c.close();
        db.close();
        return lista;
    }

    // 7. ACTUALIZAR evento
    public int actualizarEvento(Eventos evento) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_lugar", evento.getId_lugar());
        cv.put("id_espacio", evento.getId_espacio());
        cv.put("nombre", evento.getNombre());
        cv.put("descripcion", evento.getDescripcion());
        cv.put("latitud", evento.getLatitud());
        cv.put("longitud", evento.getLongitud());
        cv.put("fecha_inicio", evento.getFecha_inicio());
        cv.put("fecha_fin", evento.getFecha_fin());
        cv.put("estado", evento.getEstado());

        int rows = db.update("eventos", cv, "id_evento = ?",
                new String[]{String.valueOf(evento.getId_evento())});
        db.close();
        return rows;
    }

    // 8. ELIMINAR evento (soft delete - cambiar estado a 0)
    public void eliminarEvento(int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", 0);
        db.update("eventos", cv, "id_evento = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 9. RESTAURAR evento (cambiar estado a 1)
    public void restaurarEvento(int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", 1);
        db.update("eventos", cv, "id_evento = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ==========================================
    // CRUD - USUARIOS
    // ==========================================

    // 1. INSERTAR usuario
    public long insertarUsuario(Usuarios usuario) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_tipo", usuario.getId_tipo());
        cv.put("nombres", usuario.getNombres());
        cv.put("apellidos", usuario.getApellidos());
        cv.put("correo", usuario.getCorreo());
        cv.put("carnet", usuario.getCarnet());
        cv.put("contraseña", usuario.getContraseña());
        cv.put("estado", usuario.getEstado());

        long id = db.insert("usuarios", null, cv);
        db.close();
        return id;
    }

    // 2. OBTENER todos los usuarios activos (estado = 1)
    public List<Usuarios> getUsuarios() {
        List<Usuarios> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE estado = 1", null);

        while (c.moveToNext()) {
            Usuarios u = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(u);
        }
        c.close();
        db.close();
        return lista;
    }

    // 3. OBTENER todos los usuarios (incluyendo inactivos)
    public List<Usuarios> getUsuariosTodos() {
        List<Usuarios> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios", null);

        while (c.moveToNext()) {
            Usuarios u = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(u);
        }
        c.close();
        db.close();
        return lista;
    }

    // 4. OBTENER usuario por ID
    public Usuarios getUsuarioById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE id_usuario = ?",
                new String[]{String.valueOf(id)});

        Usuarios usuario = null;
        if (c.moveToFirst()) {
            usuario = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
        }
        c.close();
        db.close();
        return usuario;
    }

    // 5. OBTENER usuario por email (para login)
    public Usuarios getUsuarioByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE correo = ? AND estado = 1",
                new String[]{email});

        Usuarios usuario = null;
        if (c.moveToFirst()) {
            usuario = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
        }
        c.close();
        db.close();
        return usuario;
    }

    // 6. OBTENER usuario por carnet
    public Usuarios getUsuarioByCarnet(String carnet) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE carnet = ? AND estado = 1",
                new String[]{carnet});

        Usuarios usuario = null;
        if (c.moveToFirst()) {
            usuario = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
        }
        c.close();
        db.close();
        return usuario;
    }

    // 7. OBTENER usuarios por tipo (admin, estudiante, etc.)
    public List<Usuarios> getUsuariosByTipo(int idTipo) {
        List<Usuarios> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE id_tipo = ? AND estado = 1",
                new String[]{String.valueOf(idTipo)});

        while (c.moveToNext()) {
            Usuarios u = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(u);
        }
        c.close();
        db.close();
        return lista;
    }

    // 8. ACTUALIZAR usuario
    public int actualizarUsuario(Usuarios usuario) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id_tipo", usuario.getId_tipo());
        cv.put("nombres", usuario.getNombres());
        cv.put("apellidos", usuario.getApellidos());
        cv.put("correo", usuario.getCorreo());
        cv.put("carnet", usuario.getCarnet());
        cv.put("contraseña", usuario.getContraseña());
        cv.put("estado", usuario.getEstado());

        int rows = db.update("usuarios", cv, "id_usuario = ?",
                new String[]{String.valueOf(usuario.getId_usuario())});
        db.close();
        return rows;
    }

    // 9. ELIMINAR usuario (soft delete - cambiar estado a 0)
    public void eliminarUsuario(int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", 0);
        db.update("usuarios", cv, "id_usuario = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 10. RESTAURAR usuario (cambiar estado a 1)
    public void restaurarUsuario(int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", 1);
        db.update("usuarios", cv, "id_usuario = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 11. VERIFICAR login (email + contraseña) CONCEPTO NO FINAL
    public Usuarios login(String email, String contraseña) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE correo = ? AND contraseña = ? AND estado = 1",
                new String[]{email, contraseña});

        Usuarios usuario = null;
        if (c.moveToFirst()) {
            usuario = new Usuarios(
                    c.getInt(c.getColumnIndexOrThrow("id_usuario")),
                    c.getInt(c.getColumnIndexOrThrow("id_tipo")),
                    c.getString(c.getColumnIndexOrThrow("nombres")),
                    c.getString(c.getColumnIndexOrThrow("apellidos")),
                    c.getString(c.getColumnIndexOrThrow("correo")),
                    c.getString(c.getColumnIndexOrThrow("carnet")),
                    c.getString(c.getColumnIndexOrThrow("contraseña")),
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
        }
        c.close();
        db.close();
        return usuario;
    }





}