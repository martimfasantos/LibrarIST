package pt.ulisboa.tecnico.cmov.librarist.models;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.caches.BookCache;

public class Library {

    private int id;
    private String name;
    private String address;

    private List<Integer> bookIds;

    public Library(int id, String name, String address, List<Integer> bookIds){
        this.id = id;
        this.name = name;
        this.address = address;
        this.bookIds = bookIds;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public List<Integer> getBookIds() {
        return bookIds;
    }
}
