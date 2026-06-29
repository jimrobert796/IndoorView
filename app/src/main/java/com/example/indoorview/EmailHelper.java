package com.example.indoorview;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Clase encargada de gestionar el envío de correos de verificación
 * para el proyecto Indoor View. Modificada para evadir filtros estrictos.
 */
public class EmailHelper {

    // Credenciales de la cuenta robot de Gmail (Configuración de Google)
    private static final String CORREO_REMITENTE = EmailKeys.CORREO_REMITENTE;
    private static final String CONTRASENA_APP = EmailKeys.CONTRASENA_APP;

    public static void enviarCodigoVerificacion(String correoDestino, String codigoGenerado) {

        // El envío se realiza en un hilo separado (background) para evitar
        // que la interfaz de la app se congele.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Configuración del servidor SMTP de Gmail (Google)
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                // Sesión autenticada con el servidor de Google
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(CORREO_REMITENTE, CONTRASENA_APP);
                    }
                });

                // Creación del mensaje
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(CORREO_REMITENTE));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoDestino));

                // CAMBIO CLAVE: Asunto plano sin la palabra "" para evitar bloqueos por suplantación
                message.setSubject("Codigo de acceso - IndoorView");

                // CAMBIO CLAVE: Cuerpo directo sin enlaces ni palabras de alerta como "contraseña"
                String cuerpo = "Tu codigo de verificacion es: " + codigoGenerado;
                message.setText(cuerpo);

                // Envío final
                Transport.send(message);
                System.out.println("DEBUG: Correo enviado exitosamente a " + correoDestino);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("DEBUG: Error al enviar correo: " + e.getMessage());
            }
        });
    }
}