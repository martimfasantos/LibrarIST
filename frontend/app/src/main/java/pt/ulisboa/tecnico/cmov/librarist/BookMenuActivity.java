package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class BookMenuActivity extends AppCompatActivity {

    private final ServerConnection serverConnection = new ServerConnection();

    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_menu_books);

        // List all books
        listAllBooks();

        // Set up onclick method for the search button
        setupSearchButton();

        // Back Button
        setupBackButton();
    }


    /** -----------------------------------------------------------------------------
     *                                  BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupSearchButton() {
        ImageButton searchBtn = findViewById(R.id.search_book_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textFilter = (TextView) findViewById(R.id.book_title_input);
                String filter = (String) textFilter.getText().toString();
                try {
                    List<Book> filteredBooks = filterBooksByTitle(filter);
                    addBookItemsToView(filteredBooks);
                }  catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void setupBackButton(){
        ImageView back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    // Used when creating each element of the list of the libraries
    private void setupBookCardButton(CardView cardView) {
        cardView.setOnClickListener(v -> {
            int bookId = (int) v.getTag();
            Intent intent = new Intent(BookMenuActivity.this, BookInfoActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });
    }


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    @Override
    protected void onResume() {
        super.onResume();
        // Update view with changes
        listAllBooks();
    }

    private void listAllBooks() {

        List<Book> allBooks;
        // TODO if there is internet
        if (true){
            // Get all books from the server
            allBooks = new ArrayList<>(getAllBooks());
        } else {
            // If there is NO internet available
            allBooks = new ArrayList<>(booksCache.getBooks());
        }

        // Add books to the view
        addBookItemsToView(allBooks);
    }

    private List<Book> getAllBooks() {
        Log.d("GET ALL BOOKS", "GET ALL BOOKS");

        // Get all books ever registered in the system
        final List<Book> allBooks = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try {
                allBooks.addAll(serverConnection.getAllBooks());
                Log.d("GET ALL BOOKS", allBooks.toString());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_get_books));
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            messageDisplayer.showToast(getResources().getString(R.string.got_all_books));
        });

        // Start the thread
        thread.start();
        // Wait for thread to join
        try{
            thread.join();
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }

        return allBooks;
    }

    private void addBookItemsToView(List<Book> books) {
        LinearLayout parent = findViewById(R.id.div_books_list);
        parent.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        books.forEach(book -> {
            // Create new element for the book
            CardView child = (CardView) inflater.inflate(R.layout.book_menu_item, null);

            LinearLayout bookDiv = (LinearLayout) child.getChildAt(0);

            // Set text to the book title
            TextView cardText = (TextView) bookDiv.getChildAt(1);
            cardText.setText(book.getTitle());
            // Set tag to save the id
            child.setTag(book.getId());

            // Clickable Card
            setupBookCardButton(child);

            parent.addView(child);
        });
        Log.d("LIST ALL BOOKS", "ADDED TO VIEW");
    }

    private List<Book> filterBooksByTitle(String titleFilter) throws InterruptedException{
        final List<Book> filteredBooks = new ArrayList<>();

        // TODO if there is internet
        if (true){
            Thread thread = new Thread(() -> {
                try {
                    if (titleFilter.isEmpty()) {
                        filteredBooks.addAll(serverConnection.getAllBooks());
                    } else {
                        filteredBooks.addAll(serverConnection.filterBooksByTitle(titleFilter));
                    }
                    Log.d("FILTER BOOKS", "TITLE " + titleFilter);
                } catch (ConnectException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_filter_books));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Start the thread
            thread.start();
            // Wait for thread to join
            try{
                thread.join();
            } catch (InterruptedException e){
                throw new RuntimeException(e);
            }

        } else {
            // If there is NO internet available
            filteredBooks.addAll(booksCache.getBooks().stream()
                    .filter(book -> book.getTitle().contains(titleFilter))
                    .collect(Collectors.toList()));
        }
        return filteredBooks;
    }
}
