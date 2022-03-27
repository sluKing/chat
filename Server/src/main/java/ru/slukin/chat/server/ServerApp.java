package ru.slukin.chat.server;

public class ServerApp {
    private static final int PORT = 8189;

    public static void main(String[] args) {
        new Server(PORT);
    }
}
