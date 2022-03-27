package ru.slukin.chat.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.slukin.chat.client.constants.Command;

public class RegController {
    @FXML
    public TextField loginField;
    @FXML
    public TextField nicknameField;
    @FXML
    public PasswordField passwordField;

    private ChatController chatController;

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    @FXML
    public void tryToRegister() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();
        chatController.registration(login, password, nickname);
    }

    public void result(String command) {
        if (command.equals(Command.REQUEST_REG_OK)) {
            System.out.println("Registration complete.");
        } else {
            System.out.println("Registration error.");
        }
    }
}
