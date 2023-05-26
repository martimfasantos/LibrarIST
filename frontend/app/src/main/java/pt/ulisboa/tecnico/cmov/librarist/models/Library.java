package pt.ulisboa.tecnico.cmov.librarist.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;


public class Library {

    private final int id;
    private final String name;
    private final LatLng latLng;
    private final String address;

    private final byte[] photo;

    private List<Integer> bookIds;

    public Library(int id, String name, LatLng latLng, String address, List<Integer> bookIds, byte[] photo){
        this.id = id;
        this.name = name;
        this.latLng = latLng;
        this.address = address;
        this.bookIds = bookIds;
        this.photo = photo;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public double getLatitude () {
        return latLng.latitude;
    }

    public double getLongitude() {
        return latLng.longitude;
    }

    public String getAddress() {
        return address;
    }

    public List<Integer> getBookIds() {
        return bookIds;
    }

    public byte[] getPhoto() { return photo; }

    public void addBook(int bookId){
        if (!bookIds.contains(bookId)){
            bookIds.add(bookId);
        }
    }

    public void removeBook(int bookId){
        if (bookIds.contains(bookId)){
            bookIds.remove(bookId);
        }
    }
}
