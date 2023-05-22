package pt.ulisboa.tecnico.cmov.librarist.models;

public class Library {

    public String name;
    public Double distance;
    public int id;

    public Library(String name, Double distance, int id) {
        this.name = name;
        this.distance = distance;
        this.id = id;
    }
}
