package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.currentLocation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class BookInfoActivity extends AppCompatActivity {

    private Book book;

    private final ServerConnection serverConnection = new ServerConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_info);
        Log.d("BookInfoActivity", "loaded layout");

        // Parse intent and its information
        parseIntent();

        // Setup View
        setupViewWithBookInfo();

        // Available libraries
        setupViewWithAvailableLibraries();

    }

    private void setupViewWithBookInfo() {
        TextView bookTitle = findViewById(R.id.book_info_title);
        bookTitle.setText(this.book.getTitle());

        ImageView bookCover = findViewById(R.id.book_info_cover_img);
        Bitmap bmp = BitmapFactory.decodeByteArray(book.getCover(), 0, book.getCover().length);
        bookCover.setImageBitmap(Bitmap.createScaledBitmap(bmp, bookCover.getWidth(),
                bookCover.getHeight(), false));

        setNotificationView(this.book.isActiveNotif());

        // Back Button
        setupBackButton();

        // Notifications Button
        setupNotificationButton();

        // List libraries where the book is available
        listAvailableLibraries();
    }

    private void setupViewWithAvailableLibraries() {
        // TODO get libraries from cache

        /*
        List<Library> availableLibraries = List.of(
                new Library(0, "Library 1", new LatLng(40, -20),
                        "I don't know", new ArrayList<>()),
                new Library(2, "Library 3", new LatLng(42, -21),
                        "I don't know2", new ArrayList<>()),
                new Library(1, "Library 2", new LatLng(38.736946, -9.142685),
                        "Lisboa, Portugal", new ArrayList<>()));

        // TODO create card view for each library
        addLibrariesToView(availableLibraries);
        */
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

    // Used when creating each element of the list of the libraries
    private void setupLibraryCardButton(CardView cardView) {
        cardView.setOnClickListener(v -> {
            int libId = Integer.parseInt(v.getTag().toString());

            Intent intent = new Intent(BookInfoActivity.this, LibraryInfoActivity.class);
            intent.putExtra("libId", libId);
            startActivity(intent);
        });
    }


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void parseIntent(){
        Intent intent = getIntent();
        this.book = booksCache.getBook(intent.getIntExtra("bookId", -1));
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

    private void listAvailableLibraries() {

        List<Library> libraries;
        // TODO if there is internet
        if (true){
            // Get all books from the server
            libraries = new ArrayList<>(getAvailableLibraries());
        } else {
            // If there is NO internet available
            // TODO search in cache
            libraries = new ArrayList<>();
        }

        // Add books to the view
        addLibraryItemsToView(libraries);
    }

    private List<Library> getAvailableLibraries() {
        Log.d("GET AVAILABLE LIBRARIES", "GET LIBRARIES");

        // Get all books ever registered in the system
        final List<Library> libraries = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try {
                libraries.addAll(serverConnection.getLibrariesWithBook(this.book));
                Log.d("GET AVAILABLE LIBRARIES", libraries.toString());
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

        Toast.makeText(getApplicationContext(), "Got all books!", Toast.LENGTH_SHORT).show();

        return libraries;
    }

    @SuppressLint("DefaultLocale")
    private void addLibraryItemsToView(List<Library> libraries) {
        LinearLayout parent = findViewById(R.id.available_libraries_linear_layout);
        parent.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        libraries.forEach(library -> {
            CardView child = (CardView) inflater.inflate(R.layout.book_info_available_lib, null);

            LinearLayout layout = (LinearLayout) child.getChildAt(0);
            LinearLayout libDiv = (LinearLayout) layout.getChildAt(0);

            // Library name
            TextView libName = (TextView) libDiv.getChildAt(0);
            libName.setText(library.getName());

            // Get distance from current location
            float[] result = new float[10];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    library.getLatLng().latitude, library.getLatLng().longitude, result);
            // Convert result from meters to km
            float distance = result[0] / 1000;
            // Set distance to view
            TextView libDist = (TextView) libDiv.getChildAt(1);
            libDist.setText(String.format("%.2fkm", distance));

            child.setTag(library.getId());

            // Clickable Card
            setupLibraryCardButton(child);

            // sort Libraries by distance
            int insertionIndex = 0;
            for (int i = 0; i < parent.getChildCount(); i++) {
                CardView card = (CardView) parent.getChildAt(i);
                TextView view = card.findViewById(R.id.book_available_library_distance);
                String text = view.getText().toString();
                double dist = Double.parseDouble(text.substring(0, text.length() - 2));
                if (distance > dist){
                    insertionIndex++;
                } else {
                    break;
                }
            }
            parent.addView(child, insertionIndex);
        });
    }

}
