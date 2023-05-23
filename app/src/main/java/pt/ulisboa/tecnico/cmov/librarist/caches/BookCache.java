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

    private void addBooks(Library library){
//        for book in libraries:
//            add book to list if book is not already there
//        TODO
//        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
//        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
