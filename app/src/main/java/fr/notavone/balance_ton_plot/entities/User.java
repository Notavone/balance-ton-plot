package fr.notavone.balance_ton_plot.entities;

public class User {

    private final String uuid;
    private String username;

    public User() {
        this(null, null);
    }

    public User(String uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
