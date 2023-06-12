package pt.ulisboa.tecnico.cmov.librarist.popups;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.R;
import pt.ulisboa.tecnico.cmov.librarist.ServerConnection;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class RateBookPopUp {

    private final Activity BookInfoActivity;
    private final View rateBookView;

    private final int bookId;

    private final ServerConnection serverConnection = new ServerConnection();
    private final MessageDisplayer messageDisplayer;


    public RateBookPopUp(Activity BookInfoActivity, int bookId, String bookTitle) {

        this.BookInfoActivity = BookInfoActivity;
        this.bookId = bookId;
        this.messageDisplayer = new MessageDisplayer(this.BookInfoActivity);

        // Display AlertDialog to get the book rating
        LayoutInflater inflater = BookInfoActivity.getLayoutInflater();
        rateBookView = inflater.inflate(R.layout.rating_book, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BookInfoActivity);
        alertDialogBuilder.setView(rateBookView);

        // Create the Alert Dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // Setup pop up title
        setupPopUpTitle(bookTitle);

        // Setup stars buttons for rating
        setupStarsButtons();

        // Setup rate button
        setupRateButton(alertDialog);

        // Setup cancel button
        setupCancelButton(alertDialog);

        // Show the AlertDialog
        alertDialog.show();
    }


    /** -----------------------------------------------------------------------------
     *                                 BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupPopUpTitle(String title) {
        TextView titleView = rateBookView.findViewById(R.id.rating_book_tile);
        titleView.setText(title);
    }

    private void setupStarsButtons() {
        // get all star buttons
        final List<ImageView> starsByNumber = List.of(
                rateBookView.findViewById(R.id.rating_book_star_1),
                rateBookView.findViewById(R.id.rating_book_star_2),
                rateBookView.findViewById(R.id.rating_book_star_3),
                rateBookView.findViewById(R.id.rating_book_star_4),
                rateBookView.findViewById(R.id.rating_book_star_5));

        // for each star do its on click
        for (int i = 0; i < 5; i++) {
            final int starNr = i;
            starsByNumber.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // put star value in the group
                    LinearLayout starGroup = rateBookView.findViewById(R.id.rating_book_star_group);
                    starGroup.setTag(starNr + 1);
                    Log.d("RATING BOOK", String.valueOf(starGroup.getTag()));
                    // make all unselected
                    starsByNumber.forEach(starBtn -> cleanStarButton(starBtn));
                    // set button to selected the ones before
                    for (int j = 0; j <= starNr; j++) {
                        toggleStarButton(starsByNumber.get(j));
                    }
                }
            });
        }
    }

    private void setupRateButton(AlertDialog alertDialog) {
        Button rateButton = rateBookView.findViewById(R.id.rating_book_rate_btn);
        rateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the stars value
                LinearLayout starGroup = rateBookView.findViewById(R.id.rating_book_star_group);
                int rating = Integer.parseInt(starGroup.getTag().toString());

                // Create library on the backend
                Thread thread = new Thread(() -> {
                    try {
                        Log.d("RATING BOOK", String.valueOf(rating));
                        serverConnection.rateBook(bookId, rating);
                    } catch (ConnectException e) {
                        messageDisplayer.showToast("Couldn't connect to the server!");
                        return;
                    } catch (SocketTimeoutException e) {
                        messageDisplayer.showToast("Couldn't rate this book!");
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    messageDisplayer.showToast("Book rated!");
                });

                // Start the thread
                thread.start();
                // Wait for thread to join
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Dismiss the dialog
                alertDialog.dismiss();
            }
        });
    }

    private void setupCancelButton(AlertDialog alertDialog) {
        Button cancelButton = rateBookView.findViewById(R.id.rating_book_cancel_btn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                alertDialog.dismiss(); // Dismiss the dialog
            }
        });
    }


    /** -----------------------------------------------------------------------------
     *                               AUXILIARY FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void toggleStarButton(ImageView starBtn) {
        if (starBtn.getTag().toString().equals("unselected")) {
            starBtn.setImageResource(R.drawable.star_selected);
            starBtn.setTag("selected");
        } else {
            starBtn.setImageResource(R.drawable.star_unselected);
            starBtn.setTag("unselected");
        }
    }

    private void cleanStarButton(ImageView starBtn) {
        starBtn.setImageResource(R.drawable.star_unselected);
        starBtn.setTag("unselected");
    }
}