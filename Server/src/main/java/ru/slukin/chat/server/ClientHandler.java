package ru.slukin.chat.server;

import ru.slukin.chat.server.constants.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.util.Date;

public class ClientHandler {
    private final Server server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private boolean isAuthenticated;
    private String login;
    private String nickname;

    protected ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                socket.setSoTimeout(120000);
                clientAuthentication(server);
                clientServerExchange(server);
            } catch (SocketTimeoutException e) {
                sendMessage(Command.REQUEST_EXIT);
            } catch (IOException e) {
                System.out.println("User [ " + nickname + " ] disconnected.");
            } finally {
                disconnectClient();
                System.out.println("User [ " + nickname + " ] disconnected.");
            }
        }).start();
    }

    private void clientServerExchange(Server server) throws IOException {
        while (isAuthenticated) {
            String message = in.readUTF();
            if (message.startsWith(Command.REQUEST_SERVICE)) {
                executeCommand(message);
                continue;
            }
            server.sendBroadcastMessage(this, message);
        }
    }

    private void executeCommand(String command) {
        if (command.toLowerCase().startsWith(Command.REQUEST_EXIT)) {
            disconnectClient();
            return;
        }
        if (command.toLowerCase().startsWith(Command.REQUEST_PERSONAL_MESSAGE)) {
            String[] tokens = command.split("\\s+", 3);
            server.sendPrivateMessage(this, tokens[1], tokens[2]);
            return;
        }
        if (command.toLowerCase().startsWith(Command.REQUEST_IDENTITY)) {
            System.out.printf("Service request '%s' from user [ %s ].%n", Command.REQUEST_IDENTITY, nickname);
            sendMessage(String.format("Ты в чате под логином '%s'.", nickname));
            return;
        }
        if (command.toLowerCase().startsWith(Command.REQUEST_CHANGE_NICKNAME)) {
            String[] tokens = command.split("\\s+", 2);
            if (!server.isNicknameAuthenticated(tokens[1])) {
                System.out.println("User [ " + nickname + " ] changed the nickname to [ " + tokens[1] + " ].");
                server.sendBroadcastServiceMessage("Пользователь [ " + nickname + " ] изменил никнейм на '" + tokens[1] + "'.");
                nickname = tokens[1];
                server.broadcastClientList();
            } else {
                sendMessage("Никнейм [ " + tokens[1] + " ] уже занят!");
            }
        }
    }

    private void clientAuthentication(Server server) throws IOException {
        while (true) {
            String message = in.readUTF();
            System.out.println(message);
            if (message.startsWith(Command.REQUEST_SERVICE)) {
                if (message.equals(Command.REQUEST_EXIT)) {
                    sendMessage(Command.REQUEST_EXIT);
                    return;
                }
                if (message.startsWith(Command.REQUEST_AUTH)) {
                    String[] tokens = message.split("\\s", 3);
                    if (tokens.length < 3) {
                        continue;
                    }
                    String newNickname = server.getAuthService()
                            .getNicknameByLoginAndPassword(tokens[1], tokens[2]);
                    login = tokens[1];
                    if (newNickname != null) {
                        if (!server.isLoginAuthenticated(login)) {
                            nickname = newNickname;
                            sendMessage(Command.REQUEST_AUTH + nickname);
                            isAuthenticated = true;
                            server.subscribeClient(this);
                            socket.setSoTimeout(0);
                            return;
                        } else {
                            sendMessage("Учетная запись уже используется");
                        }
                    } else {
                        sendMessage("Логин или пароль не верны");
                    }
                }
                if (message.startsWith(Command.REQUEST_REG)) {
                    String[] tokens = message.split("\\s+");
                    if (tokens.length < 4) {
                        continue;
                    }
                    if (server.getAuthService().registration(tokens[1], tokens[2], tokens[3])) {
                        sendMessage(Command.REQUEST_REG_OK);
                    } else {
                        sendMessage(Command.REQUEST_REG_FALSE);
                    }
                }
            }
        }
    }

    public void sendMessage(String message) {
        try {
            if (message.startsWith("/")) {
                out.writeUTF(message);
            } else {
                out.writeUTF(DateFormat.getDateTimeInstance().format(new Date()));
                out.writeUTF(message + System.lineSeparator());
            }
        } catch (IOException e) {
            disconnectClient();
            System.out.println("User [ " + nickname + " ] disconnected.");
        }
    }

    private void disconnectClient() {
        server.unsubscribeClient(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Сокет уже закрыт: клиент был отключен ранее.");
            }
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
