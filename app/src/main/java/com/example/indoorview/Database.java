package com.example.indoorview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;


import com.example.indoorview.models.Lugar;
import com.example.indoorview.models.Pisos;
import com.example.indoorview.models.Espacio;
import com.example.indoorview.models.Geometria;
import com.example.indoorview.models.Usuarios;
//import com.example.indoorview.models.Evento;


import java.util.ArrayList;
import java.util.List;

// Lo mejorare despues ahorita es concepto

public class Database {
    // extends SQLiteOpenHelper
    // NI EN PEDO ME PONGO A RECONTRA INVESTIGAR
    /*
    

    // ==========================================
    // NOMBRE Y VERSIÓN DE LA BASE DE DATOS
    // ==========================================
    private static final String DATABASE_NAME = "db_indoorView.db";
    private static final int DATABASE_VERSION = 1;

    // ==========================================
    // TABLA: lugar (edificios)
    // ==========================================
    public static final String TABLE_LUGAR = "lugar";
    public static final String COL_LUGAR_ID = "id_lugar";
    public static final String COL_LUGAR_NOMBRE = "nombre";
    public static final String COL_LUGAR_DESCRIPCION = "descripcion";
    public static final String COL_LUGAR_URL_IMAGENES = "url_imagenes";
    public static final String COL_LUGAR_LATITUD = "latitud";
    public static final String COL_LUGAR_LONGITUD = "longitud";
    public static final String COL_LUGAR_GEOJSON = "geojson";
    public static final String COL_LUGAR_ESTADO = "estado";

    // ==========================================
    // TABLA: pisos
    // ==========================================
    public static final String TABLE_PISOS = "pisos";
    public static final String COL_PISO_ID = "id_piso";
    public static final String COL_PISO_ID_LUGAR = "id_lugar";
    public static final String COL_PISO_NUMERO = "numero";
    public static final String COL_PISO_NOMBRE = "nombre";
    public static final String COL_PISO_ACTIVO = "activo";

    // ==========================================
    // TABLA: espacio (aulas, baños, etc)
    // ==========================================
    public static final String TABLE_ESPACIO = "espacio";
    public static final String COL_ESPACIO_ID = "id_espacio";
    public static final String COL_ESPACIO_ID_LUGAR = "id_lugar";
    public static final String COL_ESPACIO_ID_PISO = "id_piso";
    public static final String COL_ESPACIO_NOMBRE = "nombre";
    public static final String COL_ESPACIO_DESCRIPCION = "descripcion";
    public static final String COL_ESPACIO_URL_IMAGENES = "url_imagenes";
    public static final String COL_ESPACIO_LATITUD = "latitud";
    public static final String COL_ESPACIO_LONGITUD = "longitud";
    public static final String COL_ESPACIO_ESTADO = "estado";

    // ==========================================
    // TABLA: geometria (polígonos)
    // ==========================================
    public static final String TABLE_GEOMETRIA = "geometria";
    public static final String COL_GEOMETRIA_ID = "id_geometria";
    public static final String COL_GEOMETRIA_ID_ESPACIO = "id_espacio";
    public static final String COL_GEOMETRIA_ID_PISO = "id_piso";
    public static final String COL_GEOMETRIA_TIPO = "tipo";
    public static final String COL_GEOMETRIA_VERTICES = "vertices";
    public static final String COL_GEOMETRIA_COLOR = "color";

    // ==========================================
    // TABLA: usuarios
    // ==========================================
    public static final String TABLE_USUARIOS = "usuarios";
    public static final String COL_USUARIO_ID = "id_usuario";
    public static final String COL_USUARIO_ID_ESTADO = "id_estado";
    public static final String COL_USUARIO_ID_TIPO = "id_tipo";
    public static final String COL_USUARIO_NOMBRES = "nombres";
    public static final String COL_USUARIO_APELLIDOS = "apellidos";
    public static final String COL_USUARIO_CARNET = "carnet";
    public static final String COL_USUARIO_CONTRASENA = "contraseña";

    // ==========================================
    // TABLA: tipo_usuario
    // ==========================================
    public static final String TABLE_TIPO_USUARIO = "tipo_usuario";
    public static final String COL_TIPO_ID = "id_tipo";
    public static final String COL_TIPO_NOMBRE = "nombre";

    // ==========================================
    // TABLA: estado
    // ==========================================
    public static final String TABLE_ESTADO = "estado";
    public static final String COL_ESTADO_ID = "id_estado";
    public static final String COL_ESTADO_ESTADO = "estado";
    
    /*
    
     

    // ==========================================
    // TABLA: eventos
    // ==========================================
    public static final String TABLE_EVENTOS = "eventos";
    public static final String COL_EVENTO_ID = "id_evento";
    public static final String COL_EVENTO_ID_LUGAR = "id_lugar";
    public static final String COL_EVENTO_ID_ESPACIO = "id_espacio";
    public static final String COL_EVENTO_NOMBRE = "nombre";
    public static final String COL_EVENTO_DESCRIPCION = "descripcion";
    public static final String COL_EVENTO_LATITUD = "latitud";
    public static final String COL_EVENTO_LONGITUD = "longitud";
    public static final String COL_EVENTO_FECHA_INICIO = "fecha_inicio";
    public static final String COL_EVENTO_FECHA_FIN = "fecha_fin";
    public static final String COL_EVENTO_ACTIVO = "activo";

    // ==========================================
    // TABLA: acciones
    // ==========================================
    public static final String TABLE_ACCIONES = "acciones";
    public static final String COL_ACCION_ID = "id_accion";
    public static final String COL_ACCION_NOMBRE = "nombre";

    // ==========================================
    // TABLA: registro_acciones
    // ==========================================
    public static final String TABLE_REGISTRO_ACCIONES = "registro_acciones";
    public static final String COL_REGISTRO_ID = "id_registro";
    public static final String COL_REGISTRO_ID_USUARIO = "id_usuario";
    public static final String COL_REGISTRO_ID_EVENTO = "id_evento";
    public static final String COL_REGISTRO_ID_ACCION = "id_accion";
    public static final String COL_REGISTRO_DESCRIPCION = "descripcion";
    public static final String COL_REGISTRO_FECHA_HORA = "fecha_hora";

    // ==========================================
    // TABLA: sync_queue
    // ==========================================
    public static final String TABLE_SYNC_QUEUE = "sync_queue";
    public static final String COL_SYNC_ID = "id_sinc";
    public static final String COL_SYNC_TABLA = "tabla";
    public static final String COL_SYNC_OPERACION = "operacion";
    public static final String COL_SYNC_ID_REGISTRO = "id_registro";
    public static final String COL_SYNC_DATOS_JSON = "datos_json";
    public static final String COL_SYNC_TIMESTAMP = "tinestamp";
    public static final String COL_SYNC_INTENTOS = "intentos";
    public static final String COL_SYNC_SINCRONIZADO = "sincronizado";
    


    private Context context;

    // Constructor
    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // La base de datos ya está creada y se copia desde assets
        // Este metodo se deja vacío porque usamos una BD preexistente
        Log.d("Database", "onCreate llamado - BD preexistente");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Para futuras migraciones
        Log.d("Database", "onUpgrade llamado - Versión " + oldVersion + " a " + newVersion);
    }

    // ==========================================
    // CRUD para LUGAR (Edificios)
    // ==========================================

    public int insertarLugar(Lugar lugar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LUGAR_NOMBRE, lugar.getNombre());
        values.put(COL_LUGAR_DESCRIPCION, lugar.getDescripcion());
        values.put(COL_LUGAR_URL_IMAGENES, lugar.getUrl_imagenes());
        values.put(COL_LUGAR_LATITUD, lugar.getLatitud());
        values.put(COL_LUGAR_LONGITUD, lugar.getLongitud());
        values.put(COL_LUGAR_GEOJSON, lugar.getGeojson());
        values.put(COL_LUGAR_ESTADO, lugar.isEstado() ? 1 : 0);

        int id = db.insert(TABLE_LUGAR, null, values);
        db.close();
        return id;
    }

    public List<Lugar> obtenerTodosLosLugares() {
        List<Lugar> lugares = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LUGAR + " WHERE " + COL_LUGAR_ESTADO + " = 1", null);

        if (cursor.moveToFirst()) {
            do {
                Lugar lugar = new Lugar();
                lugar.setId_lugar(cursor.getInt(cursor.getColumnIndexOrThrow(COL_LUGAR_ID)));
                lugar.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_NOMBRE)));
                lugar.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_DESCRIPCION)));
                lugar.setUrl_imagenes(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_URL_IMAGENES)));
                lugar.setLatitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_LATITUD)));
                lugar.setLongitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_LONGITUD)));
                lugar.setGeojson(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_GEOJSON)));
                lugar.setEstado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_LUGAR_ESTADO)) == 1);
                lugares.add(lugar);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lugares;
    }

    public Lugar obtenerLugarPorId(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LUGAR + " WHERE " + COL_LUGAR_ID + " = ? AND " + COL_LUGAR_ESTADO + " = 1", new String[]{String.valueOf(id)});

        Lugar lugar = null;
        if (cursor.moveToFirst()) {
            lugar = new Lugar();
            lugar.setId_lugar(cursor.getInt(cursor.getColumnIndexOrThrow(COL_LUGAR_ID)));
            lugar.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_NOMBRE)));
            lugar.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_DESCRIPCION)));
            lugar.setUrl_imagenes(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_URL_IMAGENES)));
            lugar.setLatitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_LATITUD)));
            lugar.setLongitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_LONGITUD)));
            lugar.setGeojson(cursor.getString(cursor.getColumnIndexOrThrow(COL_LUGAR_GEOJSON)));
            lugar.setEstado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_LUGAR_ESTADO)) == 1);
        }
        cursor.close();
        db.close();
        return lugar;
    }

    public int actualizarLugar(Lugar lugar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LUGAR_NOMBRE, lugar.getNombre());
        values.put(COL_LUGAR_DESCRIPCION, lugar.getDescripcion());
        values.put(COL_LUGAR_URL_IMAGENES, lugar.getUrl_imagenes());
        values.put(COL_LUGAR_LATITUD, lugar.getLatitud());
        values.put(COL_LUGAR_LONGITUD, lugar.getLongitud());
        values.put(COL_LUGAR_GEOJSON, lugar.getGeojson());
        values.put(COL_LUGAR_ESTADO, lugar.isEstado() ? 1 : 0);

        int rows = db.update(TABLE_LUGAR, values, COL_LUGAR_ID + " = ?", new String[]{String.valueOf(lugar.getIdLugar())});
        db.close();
        return rows;
    }

    public void eliminarLugar(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Soft delete
        ContentValues values = new ContentValues();
        values.put(COL_LUGAR_ESTADO, 0);
        db.update(TABLE_LUGAR, values, COL_LUGAR_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ==========================================
    // CRUD para PISOS
    // ==========================================

    public int insertarPisos(Pisos piso) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PISO_ID_LUGAR, piso.getId_lugar());
        values.put(COL_PISO_NUMERO, piso.getNumero());
        values.put(COL_PISO_NOMBRE, piso.getNombre());
        values.put(COL_PISO_ACTIVO, piso.isActivo() ? 1 : 0);

        int id = db.insert(TABLE_PISOS, null, values);
        db.close();
        return id;
    }

    public List<Pisos> obtenerPisossPorLugar(int idLugar) {
        List<Pisos> pisos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PISOS + " WHERE " + COL_PISO_ID_LUGAR + " = ? AND " + COL_PISO_ACTIVO + " = 1 ORDER BY " + COL_PISO_NUMERO, new String[]{String.valueOf(idLugar)});

        if (cursor.moveToFirst()) {
            do {
                Pisos piso = new Pisos();
                piso.setId_piso(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PISO_ID)));
                piso.setId_lugar(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PISO_ID_LUGAR)));
                piso.setNumero(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PISO_NUMERO)));
                piso.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COL_PISO_NOMBRE)));
                piso.setActivo(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PISO_ACTIVO)) == 1);
                pisos.add(piso);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return pisos;
    }

    // ==========================================
    // CRUD para ESPACIO (Aulas, baños, etc)
    // ==========================================

    public int insertarEspacio(Espacio espacio) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ESPACIO_ID_LUGAR, espacio.getId_lugar());
        values.put(COL_ESPACIO_ID_PISO, espacio.getId_piso());
        values.put(COL_ESPACIO_NOMBRE, espacio.getNombre());
        values.put(COL_ESPACIO_DESCRIPCION, espacio.getDescripcion());
        values.put(COL_ESPACIO_URL_IMAGENES, espacio.getUrl_imagenes());
        values.put(COL_ESPACIO_LATITUD, espacio.getLatitud());
        values.put(COL_ESPACIO_LONGITUD, espacio.getLongitud());
        values.put(COL_ESPACIO_ESTADO, espacio.isEstado() ? 1 : 0);

        int id = db.insert(TABLE_ESPACIO, null, values);
        db.close();
        return id;
    }

    public List<Espacio> obtenerEspaciosPorPisos(int idPisos) {
        List<Espacio> espacios = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ESPACIO + " WHERE " + COL_ESPACIO_ID_PISO + " = ? AND " + COL_ESPACIO_ESTADO + " = 1", new String[]{String.valueOf(idPisos)});

        if (cursor.moveToFirst()) {
            do {
                Espacio espacio = new Espacio();
                espacio.setEstado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ID)));
                espacio.setId_lugar(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ID_LUGAR)));
                espacio.setId_piso(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ID_PISO)));
                espacio.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_NOMBRE)));
                espacio.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_DESCRIPCION)));
                espacio.setUrl_imagenes(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_URL_IMAGENES)));
                espacio.setLatitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_LATITUD)));
                espacio.setLongitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_LONGITUD)));
                espacio.setEstado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ESTADO)) == 1);
                espacios.add(espacio);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return espacios;
    }

    public Espacio obtenerEspacioPorId(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ESPACIO + " WHERE " + COL_ESPACIO_ID + " = ? AND " + COL_ESPACIO_ESTADO + " = 1", new String[]{String.valueOf(id)});

        Espacio espacio = null;
        if (cursor.moveToFirst()) {
            espacio = new Espacio();
            espacio.setId_espacio(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ID)));
            espacio.setId_lugar(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ID_LUGAR)));
            espacio.setId_piso(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ID_PISO)));
            espacio.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_NOMBRE)));
            espacio.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_DESCRIPCION)));
            espacio.setUrl_imagenes(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_URL_IMAGENES)));
            espacio.setLatitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_LATITUD)));
            espacio.setLongitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_ESPACIO_LONGITUD)));
            espacio.setEstado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ESPACIO_ESTADO)) == 1);
        }
        cursor.close();
        db.close();
        return espacio;
    }

    // ==========================================
    // CRUD para GEOMETRIA (Polígonos)
    // ==========================================

    public int insertarGeometria(Geometria geometria) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_GEOMETRIA_ID_ESPACIO, geometria.getId_espacio());
        values.put(COL_GEOMETRIA_ID_PISO, geometria.getId_piso());
        values.put(COL_GEOMETRIA_TIPO, geometria.getTipo());
        values.put(COL_GEOMETRIA_VERTICES, geometria.getVertices());
        values.put(COL_GEOMETRIA_COLOR, geometria.getColor());

        int id = db.insert(TABLE_GEOMETRIA, null, values);
        db.close();
        return id;
    }

    public Geometria obtenerGeometriaPorEspacio(int idEspacio) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GEOMETRIA + " WHERE " + COL_GEOMETRIA_ID_ESPACIO + " = ?", new String[]{String.valueOf(idEspacio)});

        Geometria geometria = null;
        if (cursor.moveToFirst()) {
            geometria = new Geometria();
            geometria.setId_geometria(cursor.getInt(cursor.getColumnIndexOrThrow(COL_GEOMETRIA_ID)));
            geometria.setId_espacio(cursor.getInt(cursor.getColumnIndexOrThrow(COL_GEOMETRIA_ID_ESPACIO)));
            geometria.setId_piso(cursor.getInt(cursor.getColumnIndexOrThrow(COL_GEOMETRIA_ID_PISO)));
            geometria.setTipo(cursor.getString(cursor.getColumnIndexOrThrow(COL_GEOMETRIA_TIPO)));
            geometria.setVertices(cursor.getString(cursor.getColumnIndexOrThrow(COL_GEOMETRIA_VERTICES)));
            geometria.setColor(cursor.getString(cursor.getColumnIndexOrThrow(COL_GEOMETRIA_COLOR)));
        }
        cursor.close();
        db.close();
        return geometria;
    }

    // ==========================================
    // CRUD para USUARIOS
    // ==========================================

    public int insertarUsuario(Usuarios usuario) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USUARIO_ID_ESTADO, usuario.getId_estado());
        values.put(COL_USUARIO_ID_TIPO, usuario.getId_tipo());
        values.put(COL_USUARIO_NOMBRES, usuario.getNombres());
        values.put(COL_USUARIO_APELLIDOS, usuario.getApellidos());
        values.put(COL_USUARIO_CARNET, usuario.getCarnet());
        values.put(COL_USUARIO_CONTRASENA, usuario.getContraseña());

        int id = db.insert(TABLE_USUARIOS, null, values);
        db.close();
        return id;
    }

    public Usuarios obtenerUsuarioPorCarnet(String carnet) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USUARIOS + " WHERE " + COL_USUARIO_CARNET + " = ?", new String[]{carnet});

        Usuarios usuario = null;
        if (cursor.moveToFirst()) {
            usuario = new Usuarios();
            usuario.setId_usuario(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USUARIO_ID)));
            usuario.setId_estado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USUARIO_ID_ESTADO)));
            usuario.setId_tipo(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USUARIO_ID_TIPO)));
            usuario.setNombres(cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO_NOMBRES)));
            usuario.setApellidos(cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO_APELLIDOS)));
            usuario.setCarnet(cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO_CARNET)));
            usuario.setContraseña(cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO_CONTRASENA)));
        }
        cursor.close();
        db.close();
        return usuario;
    }

    // ==========================================
    // CRUD para EVENTOS
    // ==========================================
    
    /*
    public int insertarEvento(Evento evento) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENTO_ID_LUGAR, evento.getIdLugar());
        values.put(COL_EVENTO_ID_ESPACIO, evento.getIdEspacio());
        values.put(COL_EVENTO_NOMBRE, evento.getNombre());
        values.put(COL_EVENTO_DESCRIPCION, evento.getDescripcion());
        values.put(COL_EVENTO_LATITUD, evento.getLatitud());
        values.put(COL_EVENTO_LONGITUD, evento.getLongitud());
        values.put(COL_EVENTO_FECHA_INICIO, evento.getFechaInicio());
        values.put(COL_EVENTO_FECHA_FIN, evento.getFechaFin());
        values.put(COL_EVENTO_ACTIVO, evento.isActivo() ? 1 : 0);

        int id = db.insert(TABLE_EVENTOS, null, values);
        db.close();
        return id;
    }

    public List<Evento> obtenerEventosActivos() {
        List<Evento> eventos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTOS + " WHERE " + COL_EVENTO_ACTIVO + " = 1 ORDER BY " + COL_EVENTO_FECHA_INICIO, null);

        if (cursor.moveToFirst()) {
            do {
                Evento evento = new Evento();
                evento.setIdEvento(cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENTO_ID)));
                int idLugar = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENTO_ID_LUGAR));
                int idEspacio = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENTO_ID_ESPACIO));
                evento.setIdLugar(cursor.isNull(cursor.getColumnIndexOrThrow(COL_EVENTO_ID_LUGAR)) ? null : idLugar);
                evento.setIdEspacio(cursor.isNull(cursor.getColumnIndexOrThrow(COL_EVENTO_ID_ESPACIO)) ? null : idEspacio);
                evento.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENTO_NOMBRE)));
                evento.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENTO_DESCRIPCION)));
                evento.setLatitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENTO_LATITUD)));
                evento.setLongitud(cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENTO_LONGITUD)));
                evento.setFechaInicio(cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENTO_FECHA_INICIO)));
                evento.setFechaFin(cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENTO_FECHA_FIN)));
                evento.setActivo(cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENTO_ACTIVO)) == 1);
                eventos.add(evento);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return eventos;
    }
  */
}