package pt.ulisboa.tecnico.cmov.librarist.models;

public class Book {

    private final int id;
    private final String cover; // TODO see how this will be don
    private final String title;
    private Boolean notifActive;

    public Book(int id, String cover, String title, Boolean notifActive) {
        this.cover = cover;
        this.title = title;
        this.id = id;
        this.notifActive = notifActive;
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

    public Boolean isNotifActive() {
        return notifActive;
    }

    public void toggleNotifications() {
        this.notifActive = !this.notifActive;
    }
}
