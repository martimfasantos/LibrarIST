package pt.ulisboa.tecnico.cmov.librarist.extra_views;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.github.dhaval2404.imagepicker.ImagePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.BookInfoActivity;
import pt.ulisboa.tecnico.cmov.librarist.BookMenuActivity;
import pt.ulisboa.tecnico.cmov.librarist.MainActivity;
import pt.ulisboa.tecnico.cmov.librarist.R;
import pt.ulisboa.tecnico.cmov.librarist.ServerConnection;
import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class CreateBookPopUp {

    private final Activity LibraryInfoActivity;
    private final View createBookView;
    private Uri currentBookCoverURI;

    private final String bookBarcode;
    private final int libraryId;

    private final ServerConnection serverConnection = new ServerConnection();

    private final MessageDisplayer messageDisplayer;


    public CreateBookPopUp(Activity libraryInfoActivity, String barcode, int libraryId){

        this.LibraryInfoActivity = libraryInfoActivity;
        this.bookBarcode = barcode;
        this.libraryId = libraryId;
        this.messageDisplayer = new MessageDisplayer(this.LibraryInfoActivity);

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


    /** -----------------------------------------------------------------------------
     *                                 BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

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

    private void setupCreateButton(AlertDialog alertDialog) {
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
                    Thread thread = new Thread(() -> {
                        try {
                            serverConnection.checkInNewBook(bookTitle, convertUriToBytes(currentBookCoverURI), bookBarcode, libraryId);
                        } catch (ConnectException e) {
                            Toast.makeText(LibraryInfoActivity.getApplicationContext(), "Couldn't connect to the server!", Toast.LENGTH_SHORT).show();
                            return;
                        } catch (SocketTimeoutException e) {
                            Toast.makeText(LibraryInfoActivity.getApplicationContext(), "Couldn't check in new book!", Toast.LENGTH_SHORT).show();
                            return;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        messageDisplayer.showToast("New Book checked in!");
                    });

                    // Start the thread
                    thread.start();
                    // Wait for thread to join
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    // Update available books
                    listAvailableBooks();

                    // Dismiss the dialog
                    alertDialog.dismiss();

                }
            }
        });
    }

    // Used when creating each element of the list of all available books
    private void setupBookCardButton(CardView cardView) {
        cardView.setOnClickListener(v -> {
            int bookId = (int) v.getTag();

            Intent intent = new Intent(LibraryInfoActivity, BookInfoActivity.class);
            intent.putExtra("bookId", bookId);
            LibraryInfoActivity.startActivity(intent);
        });
    }


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

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
        LinearLayout parent = LibraryInfoActivity.findViewById(R.id.available_books_linear_layout);
        parent.removeAllViews();
        LayoutInflater inflater = LibraryInfoActivity.getLayoutInflater();

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
            messageDisplayer.showToast("There was an error processing your photo!");
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
