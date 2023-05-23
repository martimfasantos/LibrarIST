package pt.ulisboa.tecnico.cmov.librarist.caches;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class LibraryCache {

    // List of libraries
    private List<Library> libraries;

    public LibraryCache(){
        libraries = new ArrayList<>();
    }

    private void addLibrary(Library library){
        libraries.add(library);

        // TODO add all the books that this library has and add them to the BookCache
    }

    public List<Book> getBooksFromLibrary(int libraryID, BookCache bookCache){
        // Find Library
        Library library = null;
        for (Library lib : libraries){
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
}
