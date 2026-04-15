package com.example.indoorview.models;

public class Usuarios {
    private int id_usuario;
    private int id_estado;
    private int id_tipo;
    private String nombres;
    private String apellidos;
    private String correo;
    private String carnet;
    private String contraseña;

    public Usuarios(int id_usuario, int id_estado, int id_tipo, String nombres, String apellidos, String correo, String carnet, String contraseña) {
        this.id_usuario = id_usuario;
        this.id_estado = id_estado;
        this.id_tipo = id_tipo;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.carnet = carnet;
        this.contraseña = contraseña;
    }

    public int getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(int id_usuario) {
        this.id_usuario = id_usuario;
    }

    public int getId_estado() {
        return id_estado;
    }

    public void setId_estado(int id_estado) {
        this.id_estado = id_estado;
    }

    public int getId_tipo() {
        return id_tipo;
    }

    public void setId_tipo(int id_tipo) {
        this.id_tipo = id_tipo;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCarnet() {
        return carnet;
    }

    public void setCarnet(String carnet) {
        this.carnet = carnet;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }
}
