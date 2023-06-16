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

    private final List<Integer> bookIds;

    private boolean isFavorite;

    public Library(int id, String name, LatLng latLng, String address, byte[] photo,
                   List<Integer> bookIds, boolean favorite){
        this.id = id;
        this.name = name;
        this.latLng = latLng;
        this.address = address;
        this.photo = photo;
        this.bookIds = bookIds;
        this.isFavorite = favorite;
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

    public String getAddress() {
        return address;
    }

    public List<Integer> getBookIds() {
        return bookIds;
    }

    public byte[] getPhoto() { return photo; }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean status){
        this.isFavorite = status;
    }

    public void addBook(int bookId){
        if (!bookIds.contains(bookId)){
            bookIds.add(bookId);
        }
    }

    public void removeBook(int bookId){
        Integer bookIdObject = Integer.valueOf(bookId);
        bookIds.remove(bookIdObject);
    }

    public int getSizeInBytes() {
        int idSize = Integer.BYTES;
        int nameSize = name.getBytes().length;
        int latLngSize = 16; // Assuming LatLng object takes 16 bytes
        int addressSize = address.getBytes().length;
        int photoSize = photo.length;
        int bookIdsSize = bookIds.size() * Integer.BYTES;
        int favoriteSize = 1; // Assuming boolean takes 1 byte

        return idSize + nameSize + latLngSize + addressSize + photoSize + bookIdsSize + favoriteSize;
    }
}
