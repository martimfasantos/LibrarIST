package pt.ulisboa.tecnico.cmov.librarist.caches;

import android.util.LruCache;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;

public class BookCache {

    private final LruCache<Integer, Book> booksCache;

    public BookCache(int cacheSize){
        this.booksCache = new LruCache<>(cacheSize){
            @Override
            protected int sizeOf(Integer key, Book book) {
                return book.getSizeInBytes();
            }
        };
    }

    public List<Book> getBooks() {
        return new ArrayList<>(this.booksCache.snapshot().values());
    }

    public Book getBook(int bookId){
        return booksCache.get(bookId);
    }

    public void addBook(Book book){
        booksCache.put(book.getId(), book);
    }

    public void addBooks(List<Book> booksToAdd){
        for (Book book : booksToAdd){
            addBook(book);
        }
    }

    public void removeBook(int bookId){
        booksCache.remove(bookId);
    }

    public void addBookCover(int bookId, byte[] cover){
        booksCache.get(bookId).setCover(cover); // also updates the item in the cache
    }

    public boolean containsBook(int bookId){
        return booksCache.snapshot().containsKey(bookId);
    }

    public void clearCache() {
        booksCache.evictAll();
    }

}
