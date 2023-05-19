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

        Intent intent = getIntent();
        int bookId = intent.getIntExtra(BOOK_ID_MESSAGE, 0);

        // TODO call backend to get book with bookId
        Book book0 = new Book(0, "img", "The Playbook", false);
        Book book1 = new Book(1, "img", "Little Women", true);
        this.book = bookId == 0 ? book0 : book1;
        setupViewWithBookInfo();
    }

    private void setupViewWithBookInfo() {
        // TODO set image
        TextView bookTitle = findViewById(R.id.book_info_title);
        bookTitle.setText(this.book.getTitle());
        setNotificationView(this.book.isNotifActive());
        setupNotificationButton();
    }

    private void setNotificationView(Boolean active) {
        ImageButton notifBtn = findViewById(R.id.book_info_notif_btn);
        TextView notifMessage = findViewById(R.id.book_info_notif_message);
        if (active) {
            notifBtn.setImageResource(R.drawable.bell);
            notifMessage.setText(R.string.book_notif_enabled);
        } else {
            notifBtn.setImageResource(R.drawable.bell_unfilled);
            notifMessage.setText(R.string.book_notif_enabled);
        }
        notifBtn.setTag(this.book);
    }

    private void setupNotificationButton() {
        ImageButton notifBtn = findViewById(R.id.book_info_notif_btn);
        notifBtn.setOnClickListener(view -> {
            Book book = (Book) view.getTag();
            book.toggleNotifications();
            setNotificationView(book.isNotifActive());
        });
    }
}
