package pt.ulisboa.tecnico.cmov.librarist.caches;

import android.util.LruCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class LibraryCache {

    // Map of the libraries
    private final LruCache<Integer, Library> librariesCache;

    public LibraryCache(int cacheSize){
        this.librariesCache = new LruCache<>(cacheSize){
            @Override
            protected int sizeOf(Integer key, Library library) {
                return library.getSizeInBytes();
            }
        };
    }

    public void addLibrary(Library library){
        librariesCache.put(library.getId(), library);
    }

    public void removeLibrary(int libId){
        librariesCache.remove(libId);
    }

    public Library getLibrary(int libId){
        return librariesCache.get(libId);
    }

    public List<Library> getLibraries(){
        return new ArrayList<>(this.librariesCache.snapshot().values());
    }

    public List<Book> getBooksFromLibrary(int libraryID, BookCache bookCache){
        // Find Library
        Library library = null;
        for (Library lib : librariesCache.snapshot().values()){
            if (lib.getId() == libraryID){
                library = lib;
            }
        }
        // Library is not in cache / does not exist
        if (library == null){ return null; }

        // Get books for the library
        List<Book> books = new ArrayList<>();
        for (int bookId : library.getBookIds()) {
            Book book = bookCache.getBook(bookId);
            books.add(book);
        }
        return books;
    }

    public void clearCache() {
        librariesCache.evictAll();
    }
}
