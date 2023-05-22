package pt.ulisboa.tecnico.cmov.librarist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.w3c.dom.Text;

import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class BookInfoActivity extends AppCompatActivity {

    private static final String BOOK_ID_MESSAGE = "bookId";
    private Book book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_info);
        Log.d("BookInfoActivity", "loaded layout");

        // Parse intent and its information
        parseIntent();

        // Setup View
        setupViewWithBookInfo();
        setupViewWithAvailableLibraries();
    }

    private void setupViewWithBookInfo() {
        // TODO set image
        TextView bookTitle = findViewById(R.id.book_info_title);
        bookTitle.setText(this.book.getTitle());
        setNotificationView(this.book.isActiveNotif());

        // Notifications Button
        setupNotificationButton();
        // Back Button
        setupBackButton();
    }

    private void setupViewWithAvailableLibraries() {
        // TODO get libraries from cache

        List<Library> availableLibraries = List.of(
                new Library("Library 1", 2.5, 0),
                new Library("Library 2", 3.0, 0));

        // TODO sort by distance

        // TODO create card view for each library
        addLibrariesToView(availableLibraries);

    }


    /** -----------------------------------------------------------------------------
     *                                  BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupNotificationButton() {
        ImageButton notifBtn = findViewById(R.id.book_info_notif_btn);
        notifBtn.setOnClickListener(view -> {
            Book book = (Book) view.getTag();
            book.toggleNotifications();
            setNotificationView(book.isActiveNotif());
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


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void parseIntent(){
        Intent intent = getIntent();
        int bookId = intent.getIntExtra(BOOK_ID_MESSAGE, 0);

        // TODO call backend to get book with bookId
        Book book0 = new Book(0, "img", "The Playbook", false);
        Book book1 = new Book(1, "img", "Little Women", true);
        this.book = bookId == 0 ? book0 : book1;
    }

    private void setNotificationView(Boolean active) {
        ImageButton notif_btn = findViewById(R.id.book_info_notif_btn);
        TextView notifMessage = findViewById(R.id.book_info_notif_message);

        if (active) {
            notif_btn.setImageResource(R.drawable.bell_filled);
            notifMessage.setText(R.string.book_notif_enabled);
        } else {
            notif_btn.setImageResource(R.drawable.bell_unfilled);
            notifMessage.setText(R.string.book_notif_disabled);
        }

        notif_btn.setTag(this.book);
    }

    private void addLibrariesToView(List<Library> libraries) {
        LinearLayout parent = findViewById(R.id.available_libraries_linear_layout);
        LayoutInflater inflater = getLayoutInflater();

        libraries.forEach(library -> {
            CardView child = (CardView) inflater.inflate(R.layout.book_info_available_lib, null);

            LinearLayout libDiv = (LinearLayout) child.getChildAt(0);
            TextView libName = (TextView) libDiv.getChildAt(0);
            libName.setText(library.name);
            TextView libDist = (TextView) libDiv.getChildAt(1);
            libDist.setText(String.format("%.2fKm", library.distance));

            child.setTag(String.valueOf(library.id));
            // TODO call library info intent

            parent.addView(child);
        });
    }

}
