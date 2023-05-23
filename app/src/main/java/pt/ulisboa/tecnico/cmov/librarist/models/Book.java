package pt.ulisboa.tecnico.cmov.librarist.models;

import android.graphics.Bitmap;

public class Book {

    private final int id;
    private final String title;
    private final byte[] cover; // TODO see how this will be done

    // TODO private final String barCode;
    private Boolean activeNotif;

    public Book(int id, String title, byte[] cover, Boolean activeNotif) {
        this.title = title;
        this.cover = cover;
        this.id = id;
        this.activeNotif = activeNotif;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public byte[] getCover() {
        return cover;
    }

    public Boolean isActiveNotif() {
        return activeNotif;
    }

    public void toggleNotifications() {
        this.activeNotif = !this.activeNotif;
    }
}
