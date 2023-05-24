package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.currentDisplayedLibraries;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class LibraryInfoActivity extends AppCompatActivity {

    private int libraryID;
    private String libraryName;
    private String libraryAddress;

    private byte[] libraryPhoto;

    private ActivityResultLauncher<ScanOptions> barCodeLauncher;
    private String currentBarCodeResult = null;


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

        // Add/Remove Favorites Button
        setupAddRemFavButton();

        // List books Button
        setupListBooksButton();

        // Check-in book Button
        setupCheckInButton();

        // Check-out book Button
        setupCheckOutButton();

        // Setup Bar code Launcher
        setupBarCodeLauncher();

        // Back Button
        setupBackButton();
    }

    private void setupBarCodeLauncher(){
        //  Dialog after scan result
        barCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null){
                currentBarCodeResult = result.getContents();
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Scan Result")
                        .setMessage(result.getContents());
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        });
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

    private void setupListBooksButton(){
        CardView list_available_books_btn = findViewById(R.id.library_list_available_books_btn);
        list_available_books_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Available books list!", Toast.LENGTH_SHORT).show();

                // TODO IMPLEMENT THIS
//                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);
//                EditText editText = (EditText) findViewById(R.id.library_name_input);
//                String message = editText.getText().toString();
//                intent.putExtra(EXTRA_MESSAGE, message);
//                startActivity(intent);
            }
        });
    }

    private void setupCheckInButton(){
        CardView check_in_book_btn = findViewById(R.id.library_check_in_book_btn);
        check_in_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode("Check In a Book");
                if (currentBarCodeResult != null){
                    // TODO save the book with this bar code in the backend
                    Toast.makeText(getApplicationContext(), "Booked checked in!", Toast.LENGTH_SHORT).show();
                }
                currentBarCodeResult = null;
            }
        });
    }

    private void setupCheckOutButton(){
        CardView check_out_book_btn = findViewById(R.id.library_check_out_book_btn);
        check_out_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode("Check Out a Book");
                if (currentBarCodeResult != null){
                    // TODO delete the book with this bar code in the backend
                    Toast.makeText(getApplicationContext(), "Booked checked out!", Toast.LENGTH_SHORT).show();
                }
                currentBarCodeResult = null;
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
        libraryID = Integer.parseInt(intent.getStringExtra("id"));

        for (Library lib : currentDisplayedLibraries){
            if (libraryID == lib.getId()){
                libraryName = lib.getName();
                libraryAddress = lib.getAddress();
                libraryPhoto = lib.getPhoto();
            }
        }
        if (libraryName.equals("") || libraryAddress.equals("")){
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
        barCodeLauncher.launch(options);
    }


}
