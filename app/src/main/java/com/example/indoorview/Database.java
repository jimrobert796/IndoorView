package com.example.indoorview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;

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
                    c.getInt(c.getColumnIndexOrThrow("estado"))
            );
            lista.add(l);
        }
        c.close();
        return lista;
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






}