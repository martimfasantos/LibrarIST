package pt.ulisboa.tecnico.cmov.librarist.models;

import java.util.List;

public class Book {

    private final int id;
    private final String title;
    private byte[] cover;
    private final String barcode;
    private Boolean activeNotif;
    private List<Integer> rates;

    public Book(int id, String title, byte[] cover, String barcode,
                Boolean activeNotif, List<Integer> rates) {
        this.id = id;
        this.title = title;
        this.cover = cover;
        this.barcode = barcode;
        this.activeNotif = activeNotif;
        this.rates = rates;
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

    public void setCover(byte [] cover) {
        this.cover = cover;
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

    public int getSizeInBytes() {
        int idSize = Integer.BYTES;  // Size of an integer in bytes
        int titleSize = title.getBytes().length;
        int coverSize = (cover != null) ? cover.length : 0;
        int barcodeSize = barcode.getBytes().length;
        int activeNotifSize = 1;  // Assuming Boolean takes 1 byte
        int ratesSize = rates.size() * Integer.BYTES;  // Assuming each Integer takes 4 bytes

        return idSize + titleSize + coverSize +
                barcodeSize + activeNotifSize + ratesSize;
    }

    public List<Integer> getRates() { return this.rates; }

    public void setRates(List<Integer> rates) {
        this.rates = rates;
    }
}
