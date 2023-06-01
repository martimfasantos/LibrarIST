package pt.ulisboa.tecnico.cmov.librarist.caches;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class BookCache {

    private final HashMap<Integer, Book> books = new HashMap<>();

    public BookCache(){}

    public List<Book> getBooks() {
        return new ArrayList<>(this.books.values());
    }

    public Book getBook(int bookId){
        return books.get(bookId);
    }

    public void addBook(Book book){
        books.put(book.getId(), book);
    }

    public void addBooks(List<Book> booksToAdd){
        for (Book book : booksToAdd){
            addBook(book);
        }
    }

    public void removeBook(int bookId){
        books.remove(bookId);
    }

}
