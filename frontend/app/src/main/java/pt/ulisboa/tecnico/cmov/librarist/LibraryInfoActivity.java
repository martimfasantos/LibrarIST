package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.getConnectionType;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.locationPermissionGranted;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.mMap;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.markerMap;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.DEFAULT_ZOOM;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.cmov.librarist.popups.CreateBookPopUp;
import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class LibraryInfoActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Library library;

    private GoogleMap libraryMap;

    private ActivityResultLauncher<ScanOptions> barCodeLauncherCheckIn;
    private ActivityResultLauncher<ScanOptions> barCodeLauncherCheckOut;

    private CreateBookPopUp currentCreateBookPopUp;

    private final ServerConnection serverConnection = new ServerConnection();

    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_info);

        // Parse intent and its information
        parseIntent();

        // Setup View
        setupViewWithLibraryInfo();
    }

    private void setupViewWithLibraryInfo(){
        // Set text from intent into Library Name Title text view
        TextView nameView = findViewById(R.id.library_name_title);
        nameView.setText(library.getName());

        // Construct a PlacesClient (for map)
        Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));
        // The entry point to the Places API.
        Places.createClient(this);

        // Initializes the map
        initMap();

        // Change image to library's photo
        ImageView imageView = findViewById(R.id.library_photo);

        if (library.getPhoto() != null){
            imageView.setImageDrawable(new BitmapDrawable(getResources(),
                    BitmapFactory.decodeByteArray(library.getPhoto(), 0, library.getPhoto().length)));
        } else {
            imageView.setImageResource(R.drawable.image_placeholder);
        }

        // Back Button
        setupBackButton();

        // Report Button
        setupReportButton();

        // Add/Remove Favorites Button
        setupAddRemFavButton();

        // Setup Bar code Launcher
        setupBarCodeLaunchers();

        // Check-in book Button
        setupCheckInButton();

        // Check-out book Button
        setupCheckOutButton();

        // List available books
        listAvailableBooks();

    }

    private void setupBarCodeLaunchers(){
        //  Dialog after scan result for check in
        barCodeLauncherCheckIn = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null){
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Scanned Code")
                        .setMessage(result.getContents());
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                        try {
                            checkInBook(result.getContents());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        dialog.dismiss();
                    }
                }).show();
            }
        });

        //  Dialog after scan result for check in
        barCodeLauncherCheckOut = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null){
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Scanned Code")
                        .setMessage(result.getContents());
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            checkOutBook(result.getContents(), library.getId());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        dialog.dismiss();
                    }
                }).show();
            }
        });
    }

    // The only Activity that uses this is the *Image Picker* on the createBookPopUp class
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == ImagePicker.REQUEST_CODE) {
            Uri currentBookCoverURI = data.getData();
            currentCreateBookPopUp.changeUploadImageIcon(currentBookCoverURI);
        }
    }

    /** -----------------------------------------------------------------------------
     *                                 MAP FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert supportMapFragment != null;
        supportMapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        libraryMap = googleMap;

        // Set map theme
        setMapTheme();

        // Center map in this library
        centerCamera();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
    }

    public void setMapTheme() {
        // Check if dark mode is enabled
        boolean isDarkModeEnabled = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkModeEnabled) {
            libraryMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.maps_theme_night));
        }
    }

    private void centerCamera() {

        // Create a CameraPosition with desired properties
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(library.getLatLng())
                .zoom(DEFAULT_ZOOM)
                .build();

        // Set image resource for the library marker
        int imageResource;
        if (library.isFavorite()){
            imageResource = R.drawable.marker_library_fav;
        } else {
            imageResource = R.drawable.marker_library;
        }

        // Add a marker in desired location and move the camera smoothly
        libraryMap.clear();
        libraryMap.addMarker(new MarkerOptions()
                .position(library.getLatLng())
                .icon(BitmapDescriptorFactory.fromResource(imageResource)));
        // Animate the camera movement
        libraryMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void updateLocationUI() {
        if (libraryMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                libraryMap.setMyLocationEnabled(true);
                libraryMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                libraryMap.setMyLocationEnabled(false);
                libraryMap.getUiSettings().setMyLocationButtonEnabled(false);
             }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }



    /** -----------------------------------------------------------------------------
     *                                  BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupReportButton() {
        ImageView report_btn = findViewById(R.id.library_report);
        report_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(() -> {
                    try {
                        serverConnection.reportLibrary(library.getId());
                    } catch (ConnectException e) {
                        messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                        return;
                    } catch (SocketTimeoutException e) {
                        messageDisplayer.showToast(getResources().getString(R.string.error_reporting_book));
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d("REPORT BOOK", String.valueOf(library.getId()));

                    // Remove book from the libraries cache
                    libraryCache.removeLibrary(library.getId());

                    // Close current activity
                    finish();
                });

                // Start the thread
                thread.start();
                // Wait for thread to join
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Remove marker from map
                Marker marker = markerMap.get(library.getId());
                assert marker != null;
                marker.remove();
                markerMap.remove(library.getId());
            }
        });
    }

    private void setupAddRemFavButton(){
        CardView add_remove_favorites_btn = findViewById(R.id.library_add_remove_favorites_btn);
        add_remove_favorites_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ImageView favoriteButton = findViewById(R.id.favorite_library);
                boolean selected = favoriteButton.getTag().equals("selected");

                if (!selected){
                    try {
                        addLibraryToFavorites();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else { // if it was already selected
                    try {
                        removeLibraryFromFavorites();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        // Update the Star Favorite button
        updateFavoriteButtonIcon();
    }

    private void setupCheckInButton(){
        CardView check_in_book_btn = findViewById(R.id.library_check_in_book_btn);
        check_in_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode(getResources().getString(R.string.check_in_book));
            }
        });
    }

    private void setupCheckOutButton(){
        CardView check_out_book_btn = findViewById(R.id.library_check_out_book_btn);
        check_out_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode(getResources().getString(R.string.check_out_book));
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

    // Used when creating each element of the list of the available books
    private void setupBookCardButton(CardView cardView) {
        cardView.setOnClickListener(v -> {
            int bookId = Integer.parseInt(v.getTag().toString());

            Intent intent = new Intent(LibraryInfoActivity.this, BookInfoActivity.class);
            intent.putExtra("bookId", bookId);
            // putCurrentCoordinates(intent, currentCoordinates);
            startActivity(intent);
        });
    }


    /** -----------------------------------------------------------------------------
     *                                OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    @Override
    protected void onResume() {
        super.onResume();
        // Update view with changes
        listAvailableBooks();
    }

    private void parseIntent() {

        // Get the message from the intent
        Intent intent = getIntent();
        int libraryId = intent.getIntExtra("libId", -1);

        String connection  = getConnectionType(this);
        if (connection.equals("NONE")) {
            // Try to get library from cache
            Library lib = libraryCache.getLibrary(libraryId);

            // If library is not in cache
            if (lib == null){
                messageDisplayer.showToast(getResources().getString(R.string.turn_on_internet));
                finish();
            // If information is incomplete
            } else if (lib.getName().isEmpty() || lib.getAddress().isEmpty()){
                messageDisplayer.showToast(getResources().getString(R.string.error_processing));
                finish();
            // if the library is in cache
            } else {
                this.library = lib;
            }
        // Get updated library info (internet available)
        } else {
            getLibraryInfo(libraryId, connection);
        }
    }

    private void getLibraryInfo(int libraryId, String connection){
        Log.d("GET LIBRARY", "GET LIBRARY");

        // Get library information from the server
        Thread thread = new Thread(() -> {
            try {
                if (connection.equals("WIFI")){
                    this.library = serverConnection.getLibrary(libraryId);
                } else if (connection.equals("MOBILE DATA")) {
                    this.library = serverConnection.getLibraryNoPhoto(libraryId);
                } else {
                    messageDisplayer.showToast(getResources().getString(R.string.turn_on_internet));
                }
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_get_library));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Start the thread
        thread.start();
        try {
            // Wait for the thread to complete
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanCode(String action){
        // Define bar code settings
        ScanOptions options = new ScanOptions()
                .setPrompt(action)
                .setBeepEnabled(true)
                .setOrientationLocked(true)
                .setCaptureActivity(ScanBarCodeActivity.class);

        // Launch the Bar Code scanner
        if (action.equals(getResources().getString(R.string.check_in_book))){
            barCodeLauncherCheckIn.launch(options);
        } else {
            barCodeLauncherCheckOut.launch(options);
        }
    }

    private void checkInBook(String barcode) throws InterruptedException {
        Log.d("CHECKIN", "BARCODE NOT NULL");

        // Get book if exists in the backend
        AtomicInteger bookId = new AtomicInteger(-1);
        Thread thread = new Thread(() -> {
            try {
                bookId.set(serverConnection.findBook(barcode));
                Log.d("CHECKIN", "BOOK ID " + bookId.get());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_find_book));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Start the thread
        thread.start();
        // Wait for thread to join
        thread.join();

        Log.d("BOOKID", String.valueOf(bookId.get()));

        // New book in the system
        if (bookId.get() == -1) {
            currentCreateBookPopUp = new CreateBookPopUp(
                    LibraryInfoActivity.this, barcode, library.getId());
        } else {
            Thread _thread = new Thread(() -> {
                try {
                    serverConnection.checkInBook(barcode, library.getId());
                    Log.d("CHECKIN", "BOOK ID " + bookId.get());
                } catch (ConnectException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_check_in_book));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Start the thread
            _thread.start();
            // Wait for thread to join
            _thread.join();

            messageDisplayer.showToast(getResources().getString(R.string.book_check_in));

            // Update available books
            listAvailableBooks();
        }

    }

    private void checkOutBook(String barcode, int libraryId) throws InterruptedException {
        Log.d("CHECKOUT", "BARCODE NOT NULL");

        // Get book if exists in the backend
        AtomicInteger bookId = new AtomicInteger(-1);
        Thread thread = new Thread(() -> {
            try {
                bookId.set(serverConnection.findBookInLibrary(barcode, libraryId));
                Log.d("CHECKOUT", "BOOK ID " + bookId.get());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_find_book));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Start the thread
        thread.start();
        // Wait for thread to join
        thread.join();

        Log.d("BOOKID", String.valueOf(bookId.get()));

        // New book in the system
        if (bookId.get() == -1) {
            messageDisplayer.showToast(getResources().getString(R.string.book_not_from_library));
        } else {
            // Create library on the backend
            Thread _thread = new Thread(() -> {
                try {
                    serverConnection.checkOutBook(barcode, libraryId);
                    Log.d("CHECKOUT", "BOOK ID " + bookId.get());
                } catch (ConnectException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                    return;
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_check_out_book));
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                messageDisplayer.showToast(getResources().getString(R.string.book_check_out));
            });

            // Start the thread
            _thread.start();
            // Wait for thread to join
            _thread.join();

            // Update available books
            listAvailableBooks();
        }
    }

    private void addLibraryToFavorites() throws InterruptedException {

        // Add library to favorites in the backend
        Thread _thread = new Thread(() -> {
            try {
                serverConnection.addLibraryToFavorites(library.getId());
                Log.d("ADD TO FAVORITES", "LIBRARY " + library.getId());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_add_favorite_lib));
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.library.setFavorite(true);
            // If library is in cache, update as well
            if(libraryCache.getLibrary(library.getId()) != null){
                libraryCache.getLibrary(library.getId()).setFavorite(true);
            }

            messageDisplayer.showToast(getResources().getString(R.string.library_added_favorites));
        });

        // Start the thread
        _thread.start();
        // Wait for thread to join
        _thread.join();

        // If library is in cache, update locally as well
        this.library.setFavorite(true);
        if (libraryCache.getLibrary(library.getId()) != null){
            libraryCache.getLibrary(library.getId()).setFavorite(true);
        }

        // Retrieve the marker you want to modify
        Marker marker = markerMap.get(library.getId());
        // Set the new icon for the marker
        if (marker != null) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_library_fav));
        }

        Log.d("SIZE OF MAP MARKERS", markerMap.entrySet().toString());

        // Update the Star Favorite button
        updateFavoriteButtonIcon();

        // Center map with the changed marker in this view
        centerCamera();
    }

    private void removeLibraryFromFavorites() throws InterruptedException {

        // Add library to favorites in the backend
        Thread _thread = new Thread(() -> {
            try {
                serverConnection.removeLibraryFromFavorites(library.getId());
                Log.d("REMOVE FROM FAVORITES", "LIBRARY " + library.getId());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_rem_favorite_lib));
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            messageDisplayer.showToast(getResources().getString(R.string.library_rem_favorites));
        });

        // Start the thread
        _thread.start();
        // Wait for thread to join
        _thread.join();

        this.library.setFavorite(false);
        // If library is in cache, update as well
        if (libraryCache.getLibrary(library.getId()) != null){
            libraryCache.getLibrary(library.getId()).setFavorite(false);
        }

        // Retrieve the marker you want to modify
        Marker marker = markerMap.get(library.getId());
        // Set the new icon for the marker
        if (marker != null) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_library));
        }

        // Update the Star Favorite button
        updateFavoriteButtonIcon();

        // Center map with the changed marker in this view
        centerCamera();

    }

    private void updateFavoriteButtonIcon(){
        ImageView favoriteButton = findViewById(R.id.favorite_library);
        if (library.isFavorite()){
            favoriteButton.setImageResource(R.drawable.star_selected);
            favoriteButton.setTag("selected");

        } else {
            favoriteButton.setImageResource(R.drawable.star_unselected);
            favoriteButton.setTag("unselected");
        }
    }

    public void listAvailableBooks() {

        String connection = getConnectionType(this);

        List<Book> books;
        if (connection.equals("NONE")){
            // If there is NO internet available get from cache
            books = getAvailableBooksFromCache();
        } else {
            books = new ArrayList<>(getAvailableBooks());
        }

        // Add books to the view
        addBookItemsToView(books);
    }

    private List<Book> getAvailableBooks() {
        Log.d("GET AVAILABLE BOOKS", "GET BOOKS");

        // Get all books ever registered in the system
        final List<Book> books = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try {
                books.addAll(serverConnection.getBooksFromLibrary(library.getId()));
                Log.d("GET AVAILABLE BOOKS", books.toString());
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

        return books;
    }

    private List<Book> getAvailableBooksFromCache() {
        return booksCache.getBooks().stream()
                .filter(book -> libraryCache.getLibrary(library.getId())
                        .getBookIds().contains(book.getId()))
                .collect(Collectors.toList());
    }

    private void addBookItemsToView(List<Book> books) {
        LinearLayout parent = findViewById(R.id.available_books_linear_layout);
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
    }
}
