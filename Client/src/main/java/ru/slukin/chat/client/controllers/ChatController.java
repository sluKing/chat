package ru.slukin.chat.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.slukin.chat.client.constants.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable {
    @FXML
    public VBox rightPanel;
    @FXML
    public TextField loginField, messageField;
    @FXML
    public TextArea textArea;
    @FXML
    public HBox loginPanel, messagePanel;
    @FXML
    public ListView<String> clientList;
    @FXML
    public TextField textField;
    @FXML
    public PasswordField passwordField;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private AuthController authController;
    private RegController regController;
    private Stage chatWindows, authWindows, regWindow;

    private String nickname;
    private boolean authenticated;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        this.loginPanel.setVisible(!authenticated);
        this.loginPanel.setManaged(!authenticated);
        this.messagePanel.setVisible(authenticated);
        this.messagePanel.setManaged(authenticated);
        this.clientList.setVisible(authenticated);
        this.clientList.setManaged(authenticated);
        this.rightPanel.setVisible(authenticated);
        this.rightPanel.setManaged(authenticated);

        if (!authenticated) {
            this.nickname = "";
        }

        textArea.clear();
        setTitle(nickname);
    }

    private void setTitle(String nickname) {
        String title;
        if (nickname.equals("")) {
            title = "iChat";
        } else {
            title = String.format("iChat [ %s ]", nickname);
        }
        Platform.runLater(() -> this.chatWindows.setTitle(title));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            chatWindows = (Stage) textField.getScene().getWindow();
            chatWindows.setOnCloseRequest(event -> {
                System.out.println("Bye!");
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF(Command.REQUEST_EXIT);
                    } catch (IOException e) {
                        System.err.println("The connection could not be terminated.");
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void createRegStage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/registration.fxml"));
            Parent root = fxmlLoader.load();

            regWindow = new Stage();
            regWindow.setTitle("Регистрация пользователя");
            regWindow.setScene(new Scene(root, 300, 300));

            regController = fxmlLoader.getController();
            regController.setChatController(this);

            regWindow.initStyle(StageStyle.UTILITY);
            regWindow.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            System.err.println("Failed to create registration window.");
        }
    }

/*    private void createAuthStage() {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/authorization.fxml"));
            Parent root = fxmlLoader.load();
            authWindows = new Stage();
            authWindows.setTitle("Авторизация пользователя");
            authWindows.setScene(new Scene(root, 300, 200));

            authController = fxmlLoader.getController();
            authController.setChatController(this);

            authWindows.initStyle(StageStyle.UTILITY);
            authWindows.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            System.err.println("Failed to create registration window.");
        }
    }*/

    public void tryToReg() {
        if (regWindow == null) {
            createRegStage();
        }
        regWindow.show();
    }

    public void registration(String login, String password, String nickname) {
        String message = String.format("%s%s %s %s", Command.REQUEST_REG, login, password, nickname);
        if (this.socket == null || this.socket.isClosed()) {
            connect();
        }
        try {
            this.out.writeUTF(message);
        } catch (IOException e) {
            System.err.println("Error sending the message.");
        }
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String message = String.format("%s%s %s", Command.REQUEST_AUTH, loginField.getText().trim(), passwordField.getText()).trim();
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showAlertMessage("Имя пользователя не может быть пустым");
            return;
        }
        try {
            this.out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            this.socket = new Socket(Command.SERVER_HOST, Command.SERVER_PORT);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    clientAuthentication();
                    exchangeClientServer();
                } catch (IOException e) {
                    System.err.println("Failed to receive message...");
                } finally {
                    disconnect();
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Unable to connect to the server...");
        }
    }

    public void clientAuthentication() throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith(Command.REQUEST_SERVICE)) {
                if (message.equals(Command.REQUEST_EXIT)) {
                    return;
                }
                if (message.startsWith(Command.REQUEST_AUTH_OK)) {
                    this.nickname = message.split("\\s+")[1];
                    setAuthenticated(true);
                    return;
                }
                if (message.startsWith(Command.REQUEST_REG_OK)) {
                    this.regController.result(message);
                    showAlertMessage("Регистрация прошла успешно");
                    return;
                }
                if (message.equals(Command.REQUEST_REG_FALSE)) {
                    this.regController.result(message);
                    showAlertMessage("Логин или никнейм уже заняты");
                    return;
                }
            } else {
                this.textArea.appendText(message + System.lineSeparator());
            }
        }
    }

    private void exchangeClientServer() throws IOException {
        while (authenticated) {
            String message = in.readUTF();
            if (message.startsWith(Command.REQUEST_SERVICE)) {
                if (message.equals(Command.REQUEST_EXIT)) {
                    return;
                }
                if (message.startsWith(Command.REQUEST_CLIENT_LIST)) {
                    String[] tokens = message.split("\\s+");
                    Platform.runLater(() -> {
                        this.clientList.getItems().clear();
                        for (int i = 1; i < tokens.length; i++) {
                            this.clientList.getItems().add(tokens[i]);
                        }
                    });
                }
            } else if (!message.isEmpty()) {
                textArea.appendText(message + System.lineSeparator());
            }
        }
    }

    public void sendMessage() {
        try {
            this.out.writeUTF(messageField.getText());
            this.messageField.clear();
            this.messageField.requestFocus();
        } catch (IOException e) {
            showAlertMessage("Не удалось отправить сообщение...");
        }
    }

    private void disconnect() {
        textArea.clear();
        setAuthenticated(false);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            showAlertMessage("Соединение с сервером уже закрыто!");
        }
    }

    private void showAlertMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    @FXML
    public void logout() {
        try {
            this.out.writeUTF(Command.REQUEST_EXIT);
        } catch (IOException e) {
            showAlertMessage("Не удалось выйти из учетной записи");
        }
    }

    @FXML
    public void clientListMouseAction() {
        String recipient = this.clientList.getSelectionModel().getSelectedItem();
        this.messageField.setText(String.format("%s%s", Command.REQUEST_PERSONAL_MESSAGE, recipient));
    }
}
