package ru.slukin.chat.server.auth;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {
    private class User {
        private final String login;
        private final String password;
        private final String nickname;

        public User(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private final List<User> users;

    public SimpleAuthService() {
        this.users = new ArrayList<>();
        this.users.add(new User("Bob", "100", "MegaBob"));
        this.users.add(new User("Jack", "100", "SuperJack"));
        this.users.add(new User("Max", "100", "UltraMax"));
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (User user : this.users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.nickname;
            }
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        for (User user : this.users) {
            if (user.login.equals(login) || user.password.equals(password)) {
                return false;
            }
        }
        this.users.add(new User(login, password, nickname));
        return true;
    }
}
