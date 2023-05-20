package pt.ulisboa.tecnico.cmov.librarist.models;

public class Book {

    private final int id;
    private final String cover; // TODO see how this will be don
    private final String title;
    private Boolean activeNotif;

    public Book(int id, String cover, String title, Boolean activeNotif) {
        this.cover = cover;
        this.title = title;
        this.id = id;
        this.activeNotif = activeNotif;
    }

    public String getCover() {
        return cover;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public Boolean isActiveNotif() {
        return activeNotif;
    }

    public void toggleNotifications() {
        this.activeNotif = !this.activeNotif;
    }
}
