package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Map;

public class Aplicacion extends Application {
    private final Label estado = new Label();
    private SessionFactory sesiones;

    @Override
    public void start(Stage ventana) {
        VBox contenido = new VBox(10, estado);
        contenido.setPadding(new Insets(12));
        ventana.setTitle("Base de datos de clase");
        ventana.setScene(new Scene(contenido, 800, 450));
        ventana.show();

        String contrasena = System.getenv("DB_PASSWORD");
        if (contrasena == null || contrasena.isEmpty()) {
            contrasena = pedirContrasena(ventana);
        }
        if (contrasena == null) {
            estado.setText("Conexión cancelada.");
            return;
        }

        try {
            conectar(contrasena);
            estado.setText("Conexión a MySQL establecida.");
        } catch (RuntimeException error) {
            estado.setText("No se pudo conectar. Revisa MySQL, la base de datos, el usuario y la contraseña.");
            error.printStackTrace();
        }
    }

    private void conectar(String contrasena) {
        Map<String, String> entorno = System.getenv();
        String url = entorno.getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/hello_jdbc");
        String usuario = entorno.getOrDefault("DB_USER", "root");
        Configuration configuracion = new Configuration();
        configuracion.setProperty("hibernate.connection.url", url);
        configuracion.setProperty("hibernate.connection.username", usuario);
        configuracion.setProperty("hibernate.connection.password", contrasena);
        configuracion.setProperty("hibernate.connection.pool_size", "1");
        sesiones = configuracion.buildSessionFactory();
    }

    private String pedirContrasena(Stage ventana) {
        Dialog<String> dialogo = new Dialog<>();
        dialogo.initOwner(ventana);
        dialogo.setTitle("Conexión a MySQL");
        dialogo.setHeaderText("Introduce la contraseña de MySQL");
        PasswordField campo = new PasswordField();
        campo.setPromptText("Contraseña");
        dialogo.getDialogPane().setContent(campo);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.setOnShown(evento -> campo.requestFocus());
        dialogo.setResultConverter(boton -> {
            if (boton == ButtonType.OK) {
                return campo.getText();
            }
            return null;
        });
        return dialogo.showAndWait().orElse(null);
    }

    @Override
    public void stop() {
        if (sesiones != null) {
            sesiones.close();
        }
    }
}
