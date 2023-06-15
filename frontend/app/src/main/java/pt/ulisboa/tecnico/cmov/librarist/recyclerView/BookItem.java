package pt.ulisboa.tecnico.cmov.librarist.recyclerView;

public class BookItem {

    private String title;
    private int id;

    public BookItem(String title, int id) {
        this.title = title;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }
}
