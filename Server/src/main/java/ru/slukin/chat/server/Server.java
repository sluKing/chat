package ru.slukin.chat.server;

import ru.slukin.chat.server.auth.AuthService;
import ru.slukin.chat.server.auth.SimpleAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private Socket clientSocket;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server(int port) {
        this.clients = new CopyOnWriteArrayList<>();
        this.authService = new SimpleAuthService();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("The server started on the port %d.%n", port);
            while (!serverSocket.isClosed()) {
                waitAndProcessClientConnection(serverSocket);
            }
        } catch (IOException e) {
            System.err.printf("Failed to start on port %d.%n", port);
        }
    }

    private void waitAndProcessClientConnection(ServerSocket serverSocket) throws IOException {
        this.clientSocket = serverSocket.accept();
        System.out.println("Client connected (message from method 'wait an process').");
        new ClientHandler(this, this.clientSocket);
    }

    public void sendBroadcastMessage(ClientHandler sender, String message) {
        String outMessage = String.format("[ %s ]: %s", sender.getNickname(), message);
        for (ClientHandler client : this.clients) {
            client.sendMessage(outMessage);
        }
    }

    // Проверить работу метода: проследить ход действий по всему коду
    public void sendBroadcastServiceMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void sendPrivateMessage(ClientHandler sender, String recipient, String message) {
        String outMessage = String.format("[ %s ] -> [ %s ]: %s", sender.getNickname(), recipient, message);
        for (ClientHandler client : this.clients) {
            if (client.getNickname().equals(recipient)) {
                client.sendMessage(outMessage);
                if (!sender.getNickname().equals(recipient)) {
                    sender.sendMessage(outMessage);
                }
                return;
            }
        }
        sender.sendMessage("Невозможно отправить сообщение [ " + recipient + " ]. Пользователя нет в сети!");
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler client : this.clients) {
            if (client.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNicknameAuthenticated(String nickname) {
        for (ClientHandler client : this.clients) {
            if (client.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder clientBuilder = new StringBuilder("/client_list ");
        for (ClientHandler client : this.clients) {
            clientBuilder.append(client.getNickname()).append(" ");
        }
        clientBuilder.setLength(clientBuilder.length() - 1);
        String clientList = clientBuilder.toString();
        for (ClientHandler client : this.clients) {
            client.sendMessage(clientList);
        }
    }

    public void subscribeClient(ClientHandler client) {
        this.clients.add(client);
        broadcastClientList();
        sendBroadcastServiceMessage("Пользователь с никнеймом [ " + client.getNickname() + " ] вошел в чат.");
    }

    public void unsubscribeClient(ClientHandler client) {
        this.clients.remove(client);
        broadcastClientList();
        sendBroadcastServiceMessage("Пользователь с никнеймом [ " + client.getNickname() + " ] покинул чат.");
    }

    public AuthService getAuthService() {
        return authService;
    }
}
