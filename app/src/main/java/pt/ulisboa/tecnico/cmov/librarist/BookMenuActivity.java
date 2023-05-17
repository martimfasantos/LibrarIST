package pt.ulisboa.tecnico.cmov.librarist;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;

import java.util.List;

public class BookMenuActivity extends AppCompatActivity {

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_books);
        Log.d("BookMenuActivity", "loaded layout");

        // TODO GET LIST OF BOOKS

        List<String> bookList = List.of("The Playbook", "Little Women");
        addBookItemsToView(bookList);

        // TODO SEARCH
        setupSearchButton();
    }


    private void addBookItemsToView(List<String> books) {
        LinearLayout parent = findViewById(R.id.div_books_list);
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < books.size(); i++) {
            // Create new element for the book
            CardView child = (CardView) inflater.inflate(R.layout.book_menu_item, null);
            child.setId(ViewCompat.generateViewId());

            // Set text to the book title
            TextView cardText = (TextView) child.getChildAt(1);
            cardText.setText(books.get(i));

            parent.addView(child);
        }
    }

    private void setupSearchButton() {

    }
}
