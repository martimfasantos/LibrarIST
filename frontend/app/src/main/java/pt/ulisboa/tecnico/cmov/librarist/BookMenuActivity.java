package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.getConnectionType;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;
import pt.ulisboa.tecnico.cmov.librarist.recyclerView.BookItem;
import pt.ulisboa.tecnico.cmov.librarist.recyclerView.BookMenuAdapter;

public class BookMenuActivity extends AppCompatActivity {

    private final ServerConnection serverConnection = new ServerConnection();
    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);
    private RecyclerView booksMenuRv;

    private final List<Book> booksInCurrentPages = new ArrayList<>();
    private int booksPage = 0;
    private boolean isFiltering = false;
    private String titleFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_menu_books);

        setupBooksRv();

        // Set up onclick method for the search button
        setupSearchButton();

        // Back Button
        setupBackButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        booksInCurrentPages.clear();
        booksPage = 0;
        isFiltering = false;
        callUpdateBooksInCurrentPages();
        setupAdapter(booksMenuRv);
    }

    /** -----------------------------------------------------------------------------
     *                                  BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupSearchButton() {
        ImageButton searchBtn = findViewById(R.id.search_book_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFiltering = true;
                TextView textFilter = (TextView) findViewById(R.id.book_title_input);
                titleFilter = (String) textFilter.getText().toString();
                RecyclerView booksMenuRV = findViewById(R.id.recycler_view_books_list);
                try {
                    booksInCurrentPages.clear();
                    booksPage = 0;
                    booksInCurrentPages.addAll(filterBooksByTitle(titleFilter));
                    setupAdapter(booksMenuRV);
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

    private void setupBooksRv() {
        booksMenuRv = findViewById(R.id.recycler_view_books_list);
        NestedScrollView booksMenuNSV = findViewById(R.id.book_menu_NSV);

        callUpdateBooksInCurrentPages();
        setupAdapter(booksMenuRv);

        // Define a layout manager for the recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        booksMenuRv.setLayoutManager(linearLayoutManager);

        // When the user reaches the bottom of the nested scroll view load more books
        booksMenuNSV.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    booksPage++;
                    if (isFiltering) {
                        booksInCurrentPages.addAll(filterBooksByTitle(titleFilter));
                    } else {
                        callUpdateBooksInCurrentPages();
                    }
                    setupAdapter(booksMenuRv);
                }
            }
        });
    }

    private void setupAdapter(RecyclerView booksMenuRv) {
        List<BookItem> bookItemList = booksInCurrentPages.stream()
                .map(book -> new BookItem(book.getTitle(), book.getId()))
                .collect(Collectors.toList());
        BookMenuAdapter bookMenuAdapter = new BookMenuAdapter(this, bookItemList);
        booksMenuRv.setAdapter(bookMenuAdapter);
    }


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void callUpdateBooksInCurrentPages() {

        String connection  = getConnectionType(this);
        if (connection.equals("NONE")){
            booksInCurrentPages.addAll(booksCache.getBooks());
        } else {
            updateBooksInCurrentPages();
        }
    }

    private void updateBooksInCurrentPages() {
        Log.d("GET BOOKS BY PAGE", "GET BOOKS BY PAGE");

        // Get all books in following page and save in the books lists
        Thread thread = new Thread(() -> {
            try {
                List<Book> rsp = serverConnection.getBooksByPage(booksPage);
                booksInCurrentPages.addAll(rsp);
                Log.d("GET BOOKS BY PAGE", rsp.toString());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_get_books));
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
    }

    private List<Book> filterBooksByTitle(String filter) {
        final List<Book> filteredBooks = new ArrayList<>();
        String connection  = getConnectionType(this);

        if (connection.equals("NONE")) {
            if (titleFilter.isEmpty()) {
                isFiltering = false;
                titleFilter = "";
                filteredBooks.addAll(booksCache.getBooks());
            } else {
                filteredBooks.addAll(booksCache.getBooks().stream()
                        .filter(book -> book.getTitle().contains(titleFilter))
                        .collect(Collectors.toList()));
            }
        } else {
            Thread thread = new Thread(() -> {
                try {
                    if (filter.isEmpty()) {
                        isFiltering = false;
                        titleFilter = "";
                        filteredBooks.addAll(serverConnection.getBooksByPage(booksPage));
                    } else {
                        filteredBooks.addAll(serverConnection.filterBooksByTitleByPage(titleFilter, booksPage));
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
        }
        return filteredBooks;
    }
}
