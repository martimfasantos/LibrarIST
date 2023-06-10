package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.locationPermissionGranted;
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

import pt.ulisboa.tecnico.cmov.librarist.popups.CreateBookPopUp;
import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class LibraryInfoActivity extends AppCompatActivity implements OnMapReadyCallback {

    private int libraryId;
    private String libraryName;

    private LatLng libraryLatLng;
    private String libraryAddress;

    private byte[] libraryPhoto;

    private boolean isFavorited;

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
        nameView.setText(libraryName);

        // Construct a PlacesClient (for map)
        Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));
        // The entry point to the Places API.
        Places.createClient(this);

        // Initializes the map
        initMap();

        // Change image to library's photo
        ImageView imageView = findViewById(R.id.library_photo);
        imageView.setImageDrawable(new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(libraryPhoto, 0, libraryPhoto.length)));

        // Back Button
        setupBackButton();

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
                            checkOutBook(result.getContents(), libraryId);
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
                .target(libraryLatLng)
                .zoom(DEFAULT_ZOOM)
                .build();

        // Set image resource for the library marker
        int imageResource;
        if (isFavorited){
            imageResource = R.drawable.marker_library_fav;
        } else {
            imageResource = R.drawable.marker_library;
        }

        // Add a marker in desired location and move the camera smoothly
        libraryMap.clear();
        libraryMap.addMarker(new MarkerOptions()
                .position(libraryLatLng)
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
                scanCode("Check In a Book");
            }
        });
    }

    private void setupCheckOutButton(){
        CardView check_out_book_btn = findViewById(R.id.library_check_out_book_btn);
        check_out_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode("Check Out a Book");
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

    private void parseIntent(){

        // Get the message from the intent
        Intent intent = getIntent();
        this.libraryId = intent.getIntExtra("libId", -1);

        Library lib = libraryCache.getLibrary(libraryId);

        // If the library is in cache
        if (lib != null){
            this.libraryName = lib.getName();
            this.libraryLatLng = lib.getLatLng();
            this.libraryAddress = lib.getAddress();
            this.libraryPhoto = lib.getPhoto();
            this.isFavorited = lib.isFavorite();

            if (this.libraryName.isEmpty() || this.libraryAddress.isEmpty()){
                messageDisplayer.showToast("There was an error processing your request");
                finish();
            }

        } else { // Library need to be retrieved from the server
            Thread thread = new Thread(() -> {
                try {
                    serverConnection.getLibrary(libraryId);
                } catch (ConnectException e) {
                    messageDisplayer.showToast("Couldn't connect to the server!");
                    return;
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast("Couldn't get this library!");
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Book should now be in cache
                Library _lib = libraryCache.getLibrary(libraryId);

                this.libraryName = _lib.getName();
                this.libraryLatLng = _lib.getLatLng();
                this.libraryAddress = _lib.getAddress();
                this.libraryPhoto = _lib.getPhoto();
                this.isFavorited = _lib.isFavorite();
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
    }

    private void scanCode(String action){
        // Define bar code settings
        ScanOptions options = new ScanOptions()
                .setPrompt(action)
                .setBeepEnabled(true)
                .setOrientationLocked(true)
                .setCaptureActivity(ScanBarCodeActivity.class);

        // Launch the Bar Code scanner
        if (action.equals("Check In a Book")){
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
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast("Couldn't find book!");
                return;
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
            currentCreateBookPopUp = new CreateBookPopUp(LibraryInfoActivity.this, barcode, libraryId);
        } else {
            Thread _thread = new Thread(() -> {
                try {
                    serverConnection.checkInBook(barcode, libraryId);
                    Log.d("CHECKIN", "BOOK ID " + bookId.get());
                } catch (ConnectException e) {
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast("Couldn't check in book!");
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Start the thread
            _thread.start();
            // Wait for thread to join
            _thread.join();

            messageDisplayer.showToast("Book checked in!");

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
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast( "Couldn't find book!");
                return;
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
            messageDisplayer.showToast("Book is not from this library!");
        } else {
            // Create library on the backend
            Thread _thread = new Thread(() -> {
                try {
                    serverConnection.checkOutBook(barcode, libraryId);
                    Log.d("CHECKOUT", "BOOK ID " + bookId.get());
                } catch (ConnectException e) {
                    messageDisplayer.showToast("Couldn't connect to the server!");
                    return;
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast("Couldn't check out book!");
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                messageDisplayer.showToast("Book checked out!");
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
                serverConnection.addLibraryToFavorites(libraryId);
                Log.d("ADD TO FAVORITES", "LIBRARY " + libraryId);
            } catch (ConnectException e) {
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast("Couldn't add library to favorites!");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // If library is in cache, update locally as well
            LibraryInfoActivity.this.isFavorited = true;
            if(libraryCache.getLibrary(libraryId) != null){
                libraryCache.getLibrary(libraryId).setFavorite(true);
            }

            messageDisplayer.showToast("Library added to favorites!");
        });

        // Start the thread
        _thread.start();
        // Wait for thread to join
        _thread.join();

        // Retrieve the marker you want to modify
        Marker marker = markerMap.get(libraryId);
        // Set the new icon for the marker
        if (marker != null) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_library_fav));
        }

        // Update the Star Favorite button
        updateFavoriteButtonIcon();

        // Center map with the changed marker in this view
        centerCamera();

    }

    private void removeLibraryFromFavorites() throws InterruptedException {

        // Add library to favorites in the backend
        Thread _thread = new Thread(() -> {
            try {
                serverConnection.removeLibraryFromFavorites(libraryId);
                Log.d("REMOVE FROM FAVORITES", "LIBRARY " + libraryId);
            } catch (ConnectException e) {
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast("Couldn't remove library from favorites!");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            messageDisplayer.showToast("Library removed from favorites!");
        });

        // Start the thread
        _thread.start();
        // Wait for thread to join
        _thread.join();

        // If library is in cache, update locally as well
        LibraryInfoActivity.this.isFavorited = false;
        if(libraryCache.getLibrary(libraryId) != null){
            libraryCache.getLibrary(libraryId).setFavorite(false);
        }

        // Retrieve the marker you want to modify
        Marker marker = markerMap.get(libraryId);
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
        if (isFavorited){
            favoriteButton.setImageResource(R.drawable.star_selected);
            favoriteButton.setTag("selected");

        } else {
            favoriteButton.setImageResource(R.drawable.star_unselected);
            favoriteButton.setTag("unselected");
        }
    }

    public void listAvailableBooks() {
        // Get library's books that where loaded to cache when the library was loaded
        Library lib = libraryCache.getLibrary(libraryId);
        List<Integer> bookIds = lib.getBookIds();
        List<Book> books = new ArrayList<>();
        for (int id : bookIds) {
            books.add(booksCache.getBook(id));
        }
        // Add books to the view
        addBookItemsToView(books);
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
