package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.currentLocation;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.getConnectionType;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.cmov.librarist.caches.LibraryCache;
import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;
import pt.ulisboa.tecnico.cmov.librarist.popups.CreateBookPopUp;
import pt.ulisboa.tecnico.cmov.librarist.popups.RateBookPopUp;

public class BookInfoActivity extends AppCompatActivity {

    private Book book;
    private RateBookPopUp currentRateBookPopUp;

    private final ServerConnection serverConnection = new ServerConnection();

    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_info);

        // Parse intent and its information
        parseIntent();

        // Setup View
        setupViewWithBookInfo();
    }

    private void setupViewWithBookInfo() {
        TextView bookTitle = findViewById(R.id.book_info_title);
        bookTitle.setText(this.book.getTitle());

        ImageView bookCover = findViewById(R.id.book_info_cover_img);
        if (book.getCover() != null){
            Bitmap bmp = BitmapFactory.decodeByteArray(book.getCover(), 0, book.getCover().length);
            bookCover.setImageBitmap(Bitmap.createScaledBitmap(bmp, 170,
                    170, false));
        } else {
            bookCover.setImageResource(R.drawable.image_placeholder);
        }

        setNotificationView(this.book.isActiveNotif());

        // Back Button
        setupBackButton();

        // Report Button
        setupReportButton();

        // TODO only doing this locally!!!!
        // Notifications Button
        setupNotificationButton();

        // Rate button
        setupRateButton();

        // Rating Chart
        setupRateChart();

        // List libraries where the book is available
        listAvailableLibraries();
    }


    /** -----------------------------------------------------------------------------
     *                                  BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupBackButton(){
        ImageView back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupReportButton() {
        ImageView report_btn = findViewById(R.id.book_report);
        report_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(() -> {
                    try {
                        serverConnection.reportBook(book.getId());
                    } catch (ConnectException e) {
                        messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                        return;
                    } catch (SocketTimeoutException e) {
                        messageDisplayer.showToast(getResources().getString(R.string.error_reporting_book));
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d("REPORT BOOK", String.valueOf(book.getId()));

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
            }
        });
    }


    // TODO ONLY LOCALLY, NEED TO PROPAGATE TO SERVER TOO
    private void setupNotificationButton() {
        ImageButton notifBtn = findViewById(R.id.book_info_notif_btn);
        notifBtn.setOnClickListener(view -> {

            Book book = (Book) view.getTag();
            book.toggleNotifications();
            setNotificationView(book.isActiveNotif());

            String connection  = getConnectionType(this);
            if (!connection.equals("NONE")) {
                try {
                    if (book.isActiveNotif()) {
                        serverConnection.addUserToBookNotifications(book.getId());
                    } else {
                        serverConnection.removeUserFromBookNotifications(book.getId());
                    }
                } catch (ConnectException e) {
                    messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                } catch (SocketTimeoutException e) {
                    messageDisplayer.showToast("Could not toggle notifications");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

    private void setupRateButton() {
        Button rate_btn = findViewById(R.id.rate_btn);
        rate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRateBookPopUp = new RateBookPopUp(BookInfoActivity.this, book);
            }
        });
    }


    /** -----------------------------------------------------------------------------
     *                                  HISTOGRAM
     -------------------------------------------------------------------------------- */

    private void setupRateChart() {
        BarChart rateChart = findViewById(R.id.book_info_rate_chart);

        // Place values in bar chart
        ArrayList<BarEntry> rateChartEntries = getRateChartEntries();
        BarDataSet barDataSet = new BarDataSet(rateChartEntries,
                getResources().getString(R.string.book_rates));
        rateChart.setData(new BarData(barDataSet));

        // Personalize bar chart
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextSize(16f);
        rateChart.getBarData().setBarWidth(0.4f);
        rateChart.getDescription().setEnabled(false);
        barDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return NumberFormat.getInstance().format(value);
            }
        });

        XAxis xAxis = rateChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        rateChart.getAxisLeft().setDrawLabels(false);
        rateChart.getAxisRight().setDrawLabels(false);
        rateChart.getAxisLeft().setDrawGridLines(false);
        rateChart.getAxisRight().setDrawGridLines(false);
        rateChart.getAxisLeft().setDrawAxisLine(false);
        rateChart.getAxisRight().setDrawAxisLine(false);

        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES) {
            barDataSet.setValueTextColor(Color.WHITE);
            xAxis.setTextColor(Color.WHITE);
        }
    }

    private ArrayList<BarEntry> getRateChartEntries() {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            barEntries.add(new BarEntry(i+1, book.getRates().get(i)));
        }
        return barEntries;
    }


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void parseIntent(){
        Intent intent = getIntent();
        int bookId = intent.getIntExtra("bookId", -1);

        String connection  = getConnectionType(this);
        if (connection.equals("NONE")) {
            // Try to get book from cache
            Book book = booksCache.getBook(bookId);

            // If book it is not in cache
            if (book == null){
                messageDisplayer.showToast(getResources().getString(R.string.turn_on_internet));
                finish();
            // if the library is in cache
            } else {
                this.book = book;
            }
        // Get updated book info (internet available)
        } else {
            getBookInfo(bookId, connection);
        }
    }

    private void getBookInfo(int bookId, String connection) {
        Log.d("GET BOOK", "GET BOOK");

        // Get book information from the server
        Thread thread = new Thread(() -> {
            try {
                if (connection.equals("WIFI")){
                    this.book = serverConnection.getBook(bookId);
                } else if (connection.equals("MOBILE DATA")) {
                    this.book = serverConnection.getBookNoCover(bookId);
                } else {
                    messageDisplayer.showToast(getResources().getString(R.string.turn_on_internet));
                }
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_get_book));
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
    }


    private void setNotificationView(Boolean active) {
        ImageButton notif_btn = findViewById(R.id.book_info_notif_btn);

        if (active) {
            notif_btn.setImageResource(R.drawable.bell_notif_on);
        } else {
            notif_btn.setImageResource(R.drawable.bell_notif_off);
        }

        notif_btn.setTag(this.book);
    }

    private void listAvailableLibraries() {

        List<Library> libraries;
        String connection  = getConnectionType(this);
        if (connection.equals("NONE")){
            // If there is NO internet available get from cache
            libraries = getLibrariesWithBookAvailableFromCache();
        } else {
            // Get all libraries from the server
            libraries = new ArrayList<>(getLibrariesWithBookAvailable());
        }

        // Add books to the view
        addLibraryItemsToView(libraries);
    }

    private List<Library> getLibrariesWithBookAvailable() {
        Log.d("GET AVAILABLE LIBRARIES", "GET LIBRARIES");

        // Get all books ever registered in the system
        final List<Library> libraries = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try {
                libraries.addAll(serverConnection.getLibrariesWithBook(book.getId(), 10));
                Log.d("GET AVAILABLE LIBRARIES", libraries.toString());
            } catch (ConnectException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast(getResources().getString(R.string.couldnt_get_libraries));
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

        return libraries;
    }

    private List<Library> getLibrariesWithBookAvailableFromCache() {
        return libraryCache.getLibraries().stream()
                .filter(lib -> lib.getBookIds().contains(book.getId()))
                .collect(Collectors.toList());
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
                String cleanedText = text.replace(",", ".");
                double dist = Double.parseDouble(cleanedText.substring(0, text.length() - 2));
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
