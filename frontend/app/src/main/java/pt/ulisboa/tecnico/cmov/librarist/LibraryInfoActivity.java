package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import pt.ulisboa.tecnico.cmov.librarist.extra_views.CreateBookPopUp;
import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class LibraryInfoActivity extends AppCompatActivity {

    private int libraryId;
    private String libraryName;
    private String libraryAddress;

    private byte[] libraryPhoto;

    private ActivityResultLauncher<ScanOptions> barCodeLauncherCheckIn;
    private ActivityResultLauncher<ScanOptions> barCodeLauncherCheckOut;

    private CreateBookPopUp currentCreateBookPopUp;

    private final ServerConnection serverConnection = new ServerConnection();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_info);
        Log.d("MainActivity", "Hey, here is my fancy debug message!");

        // Parse intent and its information
        parseIntent();

        // Setup View
        setupViewWithLibraryInfo();
    }

    private void setupViewWithLibraryInfo(){
        // Set text from intent into Library Name Title text view
        TextView nameView = findViewById(R.id.library_name_title);
        nameView.setText(libraryName);

        // Set text from intent into Library Address Title text view
        TextView addressView = findViewById(R.id.library_address);
        addressView.setText(libraryAddress);

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
                        checkOutBook(result.getContents());
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
                    favoriteButton.setImageResource(R.drawable.library_favorite_selected);
                    favoriteButton.setTag("selected");
                    Toast.makeText(getApplicationContext(), "Library added to your favorites!", Toast.LENGTH_SHORT).show();

                } else { // if it was already selected
                    favoriteButton.setImageResource(R.drawable.library_favorite_unselected);
                    favoriteButton.setTag("unselected");
                    Toast.makeText(getApplicationContext(), "Library removed from your favorites!", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                Toast.makeText(getApplicationContext(), "Returned to Main!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    /** -----------------------------------------------------------------------------
     *                                OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void parseIntent(){

        // Get the message from the intent
        Intent intent = getIntent();
        libraryId = Integer.parseInt(intent.getStringExtra("id"));

        Library lib = libraryCache.getLibrary(libraryId);
        libraryName = lib.getName();
        libraryAddress = lib.getAddress();
        libraryPhoto = lib.getPhoto();

        if (libraryName.isEmpty() || libraryAddress.isEmpty()){
            Toast.makeText(getApplicationContext(), "There was an error processing your request", Toast.LENGTH_SHORT).show();
            finish();
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
                bookId.set(serverConnection.getBook(barcode));
                Log.d("CHECKIN", "BOOK ID " + bookId.get());
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Start the thread
            _thread.start();
            // Wait for thread to join
            _thread.join();

            Toast.makeText(getApplicationContext(), "Book checked in!", Toast.LENGTH_SHORT).show();

            // Update available books
            listAvailableBooks();
        }

    }

    private void checkOutBook(String barcode){
        Log.d("CHECKOUT", "BARCODE NOT NULL");
        // Create library on the backend
        new Thread(() -> {
            try {
                Log.d("CHECKOUT", "CHECKOUT THREAD");
                serverConnection.checkOutBook(barcode, libraryId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        Toast.makeText(getApplicationContext(), "Booked checked out!", Toast.LENGTH_SHORT).show();
    }

    public void listAvailableBooks() {

        Library lib = libraryCache.getLibrary(libraryId);
        assert lib != null;
        List<Integer> bookIds = lib.getBookIds();

        List<Book> books = new ArrayList<>();
        for (int id : bookIds) {
            books.add(booksCache.getBook(id));
        }

        LinearLayout parent = findViewById(R.id.available_books_linear_layout);
        parent.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        books.forEach(book -> {
            CardView child = (CardView) inflater.inflate(R.layout.library_book_available, null);

            LinearLayout layout = (LinearLayout) child.getChildAt(0);
            LinearLayout bookDiv = (LinearLayout) layout.getChildAt(0);

            // Book Title
            TextView bookTitle = (TextView) bookDiv.getChildAt(0);
            bookTitle.setText(book.getTitle());

            parent.addView(child);
        });
    }

}
