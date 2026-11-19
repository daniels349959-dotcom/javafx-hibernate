package org.example;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Aplicacion extends Application {
    private final TableView<List<String>> tabla = new TableView<>();
    private final Label estado = new Label();
    private SessionFactory sesiones;

    @Override
    public void start(Stage ventana) {
        ComboBox<String> tablas = new ComboBox<>();
        Label etiqueta = new Label("Tabla:");
        etiqueta.setLabelFor(tablas);
        tabla.setPlaceholder(new Label("No hay filas para mostrar"));
        VBox contenido = new VBox(10, new HBox(8, etiqueta, tablas), tabla, estado);
        contenido.setPadding(new Insets(12));
        VBox.setVgrow(tabla, Priority.ALWAYS);
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
            tablas.getItems().addAll(leerNombresDeTablas());
            tablas.setOnAction(evento -> {
                String nombre = tablas.getValue();
                if (nombre != null) {
                    mostrarTabla(nombre);
                }
            });
            if (tablas.getItems().isEmpty()) {
                estado.setText("La base de datos no tiene tablas.");
            } else {
                tablas.getSelectionModel().selectFirst();
            }
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

    private List<String> leerNombresDeTablas() {
        List<String> nombres = new ArrayList<>();
        String sql = "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'";
        try (Session sesion = sesiones.openSession()) {
            sesion.doWork((Connection conexion) -> {
                try (Statement consulta = conexion.createStatement();
                     ResultSet resultado = consulta.executeQuery(sql)) {
                    while (resultado.next()) {
                        nombres.add(resultado.getString(1));
                    }
                }
            });
        }
        nombres.sort(String::compareToIgnoreCase);
        return nombres;
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

    private void mostrarTabla(String nombre) {
        tabla.getColumns().clear();
        tabla.getItems().clear();
        String sql = "SELECT * FROM `" + nombre.replace("`", "``") + "`";
        try (Session sesion = sesiones.openSession()) {
            sesion.doWork((Connection conexion) -> {
                try (Statement consulta = conexion.createStatement();
                     ResultSet resultado = consulta.executeQuery(sql)) {
                    ResultSetMetaData metadatos = resultado.getMetaData();
                    int totalColumnas = metadatos.getColumnCount();
                    for (int i = 1; i <= totalColumnas; i++) {
                        int indice = i - 1;
                        String titulo = metadatos.getColumnLabel(i);
                        TableColumn<List<String>, String> columna = new TableColumn<>(titulo);
                        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().get(indice)));
                        columna.setPrefWidth(170);
                        tabla.getColumns().add(columna);
                    }
                    List<List<String>> filas = new ArrayList<>();
                    while (resultado.next()) {
                        List<String> fila = new ArrayList<>();
                        for (int i = 1; i <= totalColumnas; i++) {
                            fila.add(resultado.getString(i));
                        }
                        filas.add(fila);
                    }
                    tabla.getItems().setAll(filas);
                }
            });
            estado.setText(tabla.getItems().size() + " filas");
        } catch (RuntimeException error) {
            estado.setText("No se pudo leer la tabla " + nombre + ".");
            error.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (sesiones != null) {
            sesiones.close();
        }
    }
}
