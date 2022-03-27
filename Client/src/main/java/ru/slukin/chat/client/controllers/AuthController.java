package ru.slukin.chat.client.controllers;

import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.slukin.chat.client.constants.Command;

public class AuthController {
    @FXML
    public TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button authButton;

    private ChatController chatController;

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    @FXML
    public void executeAuth() {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
        }
        String message = String.format("%s%s %s", Command.REQUEST_AUTH, login, password);
    }

    public void sendMessage() {
        chatController.sendMessage();
    }
}
