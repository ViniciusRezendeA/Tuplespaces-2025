package pt.ulisboa.tecnico.tuplespaces.server.model;

public class Tuple {
    private String value;
    private int clientId;

    public Tuple(String value) {
        this.value = value;
        this.clientId = -1;
    }

    public String getValue() {
        return value;
    }

    public Boolean isLocked() {
        return clientId != -1;
    }

    public void lock(int clientId) {
        this.clientId = clientId;
    }

    public int getClientId() {
        return clientId;
    }

    public void unlock() {
        clientId = -1;
    }

}
