package pt.ulisboa.tecnico.cmov.librarist.caches;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class BookCache {

    private List<Book> books;

    public BookCache(){
        books = new ArrayList<>();
    }

    public List<Book> getBooks() {
        return books;
    }

    public Book getBook(int bookId){
        for (Book book : books){
            if (book.getId() == bookId){
                return book;
            }
        }
        return null;
    }

    public void addBook(Book book){
        if (!books.contains(book)){
            books.add(book);
        }
    }

    public void addBooks(List<Book> booksToAdd){
        for (Book book : booksToAdd){
            addBook(book);
        }
    }

    public void removeBook(int bookId){
        if (books.contains(bookId)){
            books.remove(bookId);
        }
    }

}
