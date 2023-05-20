package pt.ulisboa.tecnico.cmov.librarist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;

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
    }

    private void setupViewWithBookInfo() {
        // TODO set image
        TextView bookTitle = findViewById(R.id.book_info_title);
        bookTitle.setText(this.book.getTitle());
        setNotificationView(this.book.isActiveNotif());

        // Notifications Button
        setupNotificationButton();
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

}
