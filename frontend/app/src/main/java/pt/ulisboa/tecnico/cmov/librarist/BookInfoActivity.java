package pt.ulisboa.tecnico.cmov.librarist;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class BookInfoActivity extends AppCompatActivity {

    private static final String BOOK_ID_MESSAGE = "bookId";
    private static final String LOCATION_LAT_MESSAGE = "currentLocationLatitude";
    private static final String LOCATION_LON_MESSAGE = "currentLocationLongitude";

    private Book book;

    private LatLng currentCoordinates;

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


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void parseIntent(){
        Intent intent = getIntent();
        int bookId = intent.getIntExtra(BOOK_ID_MESSAGE, 0);
        double latitude = intent.getDoubleExtra(LOCATION_LAT_MESSAGE, -1);
        double longitude = intent.getDoubleExtra(LOCATION_LON_MESSAGE, -1);

        currentCoordinates = new LatLng(latitude, longitude);

        // TODO call backend to get book with bookId
        Book book0 = new Book(0,"The Playbook", Base64.decode(String.valueOf(R.drawable.book_cover), Base64.DEFAULT), false);
        Book book1 = new Book(1, "Little Women", Base64.decode(String.valueOf(R.drawable.book_cover), Base64.DEFAULT), true);
        this.book = bookId == 0 ? book0 : book1;
    }

    public void putCurrentCoordinates(Intent intent, LatLng latLng){
        intent.putExtra(LOCATION_LAT_MESSAGE, latLng.latitude);
        intent.putExtra(LOCATION_LON_MESSAGE, latLng.longitude);
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

    @SuppressLint("DefaultLocale")
    private void addLibrariesToView(List<Library> libraries) {
        LinearLayout parent = findViewById(R.id.available_libraries_linear_layout);
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
            Location.distanceBetween(currentCoordinates.latitude, currentCoordinates.longitude,
                    library.getLatLng().latitude, library.getLatLng().longitude, result);
            // Convert result from meters to km
            float distance = result[0] / 1000;
            // Set distance to view
            TextView libDist = (TextView) libDiv.getChildAt(1);
            libDist.setText(String.format("%.2fkm", distance));

            child.setTag(String.valueOf(library.getId()));
            // TODO call library info intent

            // TODO sort Libaries by distance
            int insertionIndex = 1;
            for (int i = 1; i < parent.getChildCount(); i++) {
                CardView card = (CardView) parent.getChildAt(i);
                TextView view = parent.findViewById(R.id.book_available_library_distance);
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
