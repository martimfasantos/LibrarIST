package pt.ulisboa.tecnico.cmov.librarist.models;

import android.graphics.Bitmap;

public class Book {

    private final int id;
    private final String title;
    private final byte[] cover;

    private final String barcode;
    private Boolean activeNotif;

    public Book(int id, String title, byte[] cover, String barcode, Boolean activeNotif) {
        this.id = id;
        this.title = title;
        this.cover = cover;
        this.barcode = barcode;
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

    public String getBarcode() {
        return barcode;
    }

    public Boolean isActiveNotif() {
        return activeNotif;
    }

    public void toggleNotifications() {
        this.activeNotif = !this.activeNotif;
    }
}
