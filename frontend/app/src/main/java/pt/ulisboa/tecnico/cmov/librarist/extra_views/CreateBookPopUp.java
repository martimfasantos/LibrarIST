package pt.ulisboa.tecnico.cmov.librarist.extra_views;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.currentDisplayedLibraries;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.librarist.LibraryInfoActivity;
import pt.ulisboa.tecnico.cmov.librarist.R;
import pt.ulisboa.tecnico.cmov.librarist.ServerConnection;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

public class CreateBookPopUp {

    private final Activity LibraryInfoActivity;
    private final View createBookView;
    private Uri currentBookCoverURI;
    private final String bookBarcode;

    private final int libraryId;

    private final ServerConnection serverConnection = new ServerConnection();

    public CreateBookPopUp(Activity libraryInfoActivity, String barcode, int libraryId){

        this.LibraryInfoActivity = libraryInfoActivity;
        this.bookBarcode = barcode;
        this.libraryId = libraryId;

        // Display AlertDialog to get the title for the marker
        LayoutInflater inflater = libraryInfoActivity.getLayoutInflater();
        createBookView = inflater.inflate(R.layout.create_book, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LibraryInfoActivity);
        alertDialogBuilder.setView(createBookView);

        // Create the Alert Dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // Create Library Button
        setupCameraButton();

        // Cancel Button
        setupCancelButton(alertDialog);

        // Create Library Button
        setupCreateButton(alertDialog);

        // Show the AlertDialog
        alertDialog.show();

    }

    private void setupCameraButton() {
        CardView cameraButton = createBookView.findViewById(R.id.camera_btn);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Image picker
                ImagePicker.with(LibraryInfoActivity)
                        .cameraOnly()           // Only use the camera of the device
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                        .start();
            }
        });
    }

    private void setupCancelButton(AlertDialog alertDialog) {
        ImageButton cancelButton = createBookView.findViewById(R.id.cancel_create_library);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel (X) button click
                alertDialog.dismiss(); // Dismiss the dialog
            }
        });
    }

    private void setupCreateButton(AlertDialog alertDialog){
        Button createButton = createBookView.findViewById(R.id.create_library);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = createBookView.findViewById(R.id.book_title_input);
                String bookTitle = editText.getText().toString();

                if (bookTitle.isEmpty()) {
                    Toast.makeText(LibraryInfoActivity, "Please insert a valid Book Title...", Toast.LENGTH_SHORT).show();
                } else if (currentBookCoverURI == null){
                    Toast.makeText(LibraryInfoActivity, "Please upload a photo of the Book cover...", Toast.LENGTH_SHORT).show();
                } else {

                    // Create library on the backend
                    new Thread(() -> {
                        try {
                            serverConnection.checkInNewBook(bookTitle, convertUriToBytes(currentBookCoverURI), bookBarcode, libraryId);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();

                    // Dismiss the dialog
                    alertDialog.dismiss();

                    Toast.makeText(LibraryInfoActivity.getApplicationContext(), "New Book checked in!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void changeUploadImageIcon(Uri photoURI) {

        this.currentBookCoverURI = photoURI;

        if (currentBookCoverURI != null) {
            // Get the image view from your XML layout
            ImageView uploadView = createBookView.findViewById(R.id.upload_image);
            // Change upload image
            uploadView.setImageResource(R.drawable.photo_upload);

            // Define the new margins in pixels
            int leftMargin = 15;   // Set the desired left margin
            int topMargin = 15;   // Set the desired top margin
            int rightMargin = 0;   // Set the desired right margin
            int bottomMargin = 0;  // Set the desired bottom margin

            // Set new dimensions
            int width = 230;
            int height = 230;

            CardView.LayoutParams layoutParams = (CardView.LayoutParams) uploadView.getLayoutParams();
            // Update the margins and dimensions of the image view
            layoutParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
            layoutParams.width = width;
            layoutParams.height = height;

            // Update the layout parameters of the image view
            uploadView.setLayoutParams(layoutParams);
        } else {
            Toast.makeText(LibraryInfoActivity, "There was an error processing your photo!", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] convertUriToBytes(Uri photoURI){
        try {
            InputStream inputStream = LibraryInfoActivity.getContentResolver().openInputStream(photoURI);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            // Handle error occurred while converting image to base64
            throw new RuntimeException(e);
        }
    }
}
