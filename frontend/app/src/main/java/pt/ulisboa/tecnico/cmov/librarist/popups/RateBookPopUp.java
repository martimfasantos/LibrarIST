package pt.ulisboa.tecnico.cmov.librarist.popups;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.R;
import pt.ulisboa.tecnico.cmov.librarist.ServerConnection;

public class RateBookPopUp {

    private final Activity BookInfoActivity;
    private final View rateBookView;

    private final int bookId;

    private final ServerConnection serverConnection = new ServerConnection();

    public RateBookPopUp(Activity BookInfoActivity, int bookId, String bookTitle) {

        this.BookInfoActivity = BookInfoActivity;
        this.bookId = bookId;

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

    /**
     * -----------------------------------------------------------------------------
     * BUTTONS FUNCTIONS
     * --------------------------------------------------------------------------------
     */

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

        // for each star button set tag to unselected
        starsByNumber.forEach(starBtn -> starBtn.setTag(R.string.starValue,"unselected"));

        // for each star do its on click
        for (int i = 0; i < 5; i++) {
            final int starNr = i;
            starsByNumber.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // put star value in the group
                    LinearLayout starGroup = rateBookView.findViewById(R.id.rating_book_star_group);
                    starGroup.setTag(starNr + 1);
                    // make all unselected
                    starsByNumber.forEach(starBtn -> cleanStarButton(starBtn));
                    // set button to selected the ones before
                    for (int j = 0; j <= starNr; j++) {
                        toggleStarButton(starsByNumber.get(starNr));
                    }
                }
            });
        }
    }

    private void setupRateButton(AlertDialog alertDialog) {
        // Get the stars value
        LinearLayout starGroup = rateBookView.findViewById(R.id.rating_book_star_group);
        int rate = Integer.parseInt(starGroup.getTag().toString());

        // Call method on serverConnection

        // Dismiss the diaalog
        alertDialog.dismiss();
    }

    private void setupCancelButton(AlertDialog alertDialog) {
        ImageButton cancelButton = rateBookView.findViewById(R.id.rating_book_cancel_btn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel (X) button click
                alertDialog.dismiss(); // Dismiss the dialog
            }
        });
    }

    private void toggleStarButton(ImageView starBtn) {
        if (starBtn.getTag(2) == "unselected") {
            starBtn.setImageResource(R.drawable.star_selected);
            starBtn.setTag(2, "selected");
        } else {
            starBtn.setImageResource(R.drawable.star_unselected);
            starBtn.setTag(2, "unselected");
        }
    }

    private void cleanStarButton(ImageView starBtn) {
        starBtn.setImageResource(R.drawable.star_unselected);
        starBtn.setTag(2, "unselected");
    }
}