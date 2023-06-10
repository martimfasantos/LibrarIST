package pt.ulisboa.tecnico.cmov.librarist;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pt.ulisboa.tecnico.cmov.librarist.caches.BookCache;
import pt.ulisboa.tecnico.cmov.librarist.caches.LibraryCache;
import pt.ulisboa.tecnico.cmov.librarist.popups.CreateLibraryPopUp;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveStartedListener {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    public static final int MAX_DIST_KM_CACHE = 10;

    // User ID
    public static int userId;
    public static GoogleMap mMap;

    // TODO make this a cache !!!
    public static HashMap<Integer, Marker> markerMap = new HashMap<>();

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;
    public static boolean locationPermissionGranted = false;
    // A default location (Lisbon, Portugal) and default zoom to use when location permission is not granted.
    private final LatLng defaultLocation = new LatLng(38.736946, -9.142685);
    public static final int DEFAULT_ZOOM = 18;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
//    private Location lastKnownLocation;

    public static Location currentLocation;
    private LatLng currentCameraCenter;

    // Caches
    private static final int maxMemorySize = (int) Runtime.getRuntime().maxMemory();
    private static final int cacheSize = maxMemorySize / 10;

    public static LibraryCache libraryCache = new LibraryCache(cacheSize/2);
    public static BookCache booksCache = new BookCache(cacheSize/2);

    // Connection to the server
    private final ServerConnection serverConnection = new ServerConnection();
    // Current visible library pop up
    private CreateLibraryPopUp currentCreateLibraryPopUp;

    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Parse intent and its information
        parseIntent();

        // Construct a PlacesClient
        Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));
        // The entry point to the Places API.
        Places.createClient(this);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initializes the map
        initMap();

        // Search Button
        setupSearchButton();

        // Books Button
        setupBooksButton();
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
        mMap = googleMap;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        // Map settings configuration
        mMap.setOnCameraMoveStartedListener(this);
        setMapTheme();

        // Get the initial camera target
        currentCameraCenter = mMap.getCameraPosition().target;

        // Create OnClick listener to allow creation of new markers by clicking an empty place in the map
        setupOnClickMap();

        // Create custom popups for the libraries
        createCustomMarkerPopUps();

    }

    @Override
    public void onCameraMoveStarted(int reason) {

        // Check if the camera movement is due to a user gesture (swipe)
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {

            // Get the current center of the map
            LatLng newCenter = mMap.getCameraPosition().target;

            // Calculate the distance between the previous and current centers
            float[] distance = new float[1];
            Location.distanceBetween(currentCameraCenter.latitude, currentCameraCenter.longitude,
                    newCenter.latitude, newCenter.longitude, distance);

            // Define your threshold for significant bounds change
            float significantDistanceThreshold = 10000; // 10km

            Log.d("CENTER", String.valueOf(currentCameraCenter));
            Log.d("NEW CENTER", String.valueOf(newCenter));
            Log.d("NEW CENTER DIST", String.valueOf(distance[0]));

            // Check if the distance exceeds the threshold
            if (distance[0] > significantDistanceThreshold) {
                Log.d("CENTER", "CHANGED");

                // Update the previous center to the current center for the next comparison
                currentCameraCenter = newCenter;

                // Get new Library Markers
                getLibrariesMarkers(currentCameraCenter);

            }
        }
    }

    public void setMapTheme() {
        // Check if dark mode is enabled
        boolean isDarkModeEnabled = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkModeEnabled) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.maps_theme_night));
        }
    }

    private void goToLocation(LatLng coordinates) {
        // Create a CameraPosition with desired properties
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(coordinates)
                .zoom(DEFAULT_ZOOM)
                .build();

        // Add a marker in desired location and move the camera smoothly
        mMap.addMarker(new MarkerOptions()
                .position(coordinates));
        // Animate the camera movement
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            Log.d("Location Permission Granted", String.valueOf(locationPermissionGranted));
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                updateCurrentLocation();
            } else {
                // If permission is not granted, move camera to the default Location
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM - 5));
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void setupOnClickMap() {
        // Setting a click event handler for the map
        mMap.setOnMapClickListener(latLng -> {
            currentCreateLibraryPopUp = new CreateLibraryPopUp(this, latLng);
                });
    }

    private void getLibrariesMarkers(LatLng coordinates) {

        // Get libraries markers
        final HashMap<Integer, MarkerOptions> libraries = new HashMap<>();
        Thread _thread = new Thread(() -> {
            try {
                libraries.putAll(serverConnection.getLibrariesMarkers(coordinates, 15));
            } catch (ConnectException e) {
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast("Couldn't load libraries!");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            messageDisplayer.showToast("Loaded libraries markers!");
        });

        // Start the thread
        _thread.start();
        // Wait for thread to join
        try {
            _thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Display markers in the map
        for (Map.Entry<Integer, MarkerOptions> entry : libraries.entrySet()) {
            Integer libId = entry.getKey();
            MarkerOptions markerOptions = entry.getValue();
            // Add marker to the map
            Marker marker = mMap.addMarker(markerOptions);
            assert marker != null;
            marker.setTag(libId);
            // Add marker to the marker map
            markerMap.put(libId, marker);
        }
    }

    private void loadCloseLibrariesToCache() {

        // Add library to favorites in the backend
        Thread thread = new Thread(() -> {
            try {
                serverConnection.loadLibrariesToCache();
            } catch (ConnectException e) {
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast("Couldn't load libraries!");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Log.d("LOAD INITIAL", "Library Cache Empty: " + (libraryCache.getLibraries().size() == 0));
            Log.d("LOAD INITIAL", "Books Cache Empty: " + (booksCache.getBooks().size() == 0));
            // messageDisplayer.showToast("Library loaded to cache!");
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


    /** -----------------------------------------------------------------------------
     *                                 BUTTONS FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void setupSearchButton() {
        // Search Button
        ImageButton search_address_btn = findViewById(R.id.search_address_btn);
        search_address_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText editText = (EditText) findViewById(R.id.address_input);
                String location = editText.getText().toString();

                if (location.equals("")) {
                    messageDisplayer.showToast("Please insert an address");
                    return;
                }

                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    // Get location coordinates that best match the given name/address
                    List<Address> addressList = geocoder.getFromLocationName(location, 1);
                    // Given address is valid
                    if (!addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        // Go to that location
                        goToLocation(new LatLng(address.getLatitude(), address.getLongitude()));

                        messageDisplayer.showToast("Centered in" + address.getLocality());
                    } else {
                        messageDisplayer.showToast("Please insert a valid address");
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

    private void setupBooksButton() {
        CardView books_btn = findViewById(R.id.books_btn);
        books_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BookMenuActivity.class);
                startActivity(intent);
            }
        });
    }


    /** -----------------------------------------------------------------------------
     *                                  PERMISSIONS
     -------------------------------------------------------------------------------- */

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                // Update Location with the new permissions
                updateLocationUI();
                // Update current location of the device
                getDeviceLocation();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /** -----------------------------------------------------------------------------
     *                                  OTHER FUNCTIONS
     -------------------------------------------------------------------------------- */

    private void parseIntent(){
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
    }

    private void updateCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(0)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(5000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()){
                    if (location != null){
                        currentLocation = location;
                    }
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates
                (locationRequest, locationCallback, null);

        // TODO location is null in the first time the user opens the app (CRASH), TO FIX
        Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
        locationResult.addOnSuccessListener(this, new OnSuccessListener<Location>(){
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    // Move to current location
                    mMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(new LatLng(currentLocation.getLatitude(),
                                    currentLocation.getLongitude()), DEFAULT_ZOOM));

                    // Load library markers
                    getLibrariesMarkers(new LatLng(location.getLatitude(), location.getLongitude()));

                    // Create custom popups for the libraries
                    createCustomMarkerPopUps();

                     // Preload caches and display libraries
                     loadCloseLibrariesToCache();
                } else {
                    messageDisplayer.showToast("Please turn on your Location...");
                }
            }
        });
    }

    // The only Activity that uses this is the *Image Picker* on the createLibraryPopUp class
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri currentLibraryPhotoURI = data.getData();
        currentCreateLibraryPopUp.changeUploadImageIcon(currentLibraryPhotoURI);
    }

    private void createCustomMarkerPopUps() {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(@NonNull Marker marker) {

                if (marker.getTag() != null) {
                    // Inflate the custom info window layout
                    View view = MainActivity.this.getLayoutInflater().inflate(R.layout.library_popup, null);

                    // Get the title and address TextViews
                    TextView libraryName = view.findViewById(R.id.library_name);
                    TextView libraryAddress = view.findViewById(R.id.library_location);

                    libraryName.setText(marker.getTitle());
                    libraryAddress.setText(marker.getSnippet());

                    return view;
                }
                return null;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);

                // Get markers information and pass them to the intent
                if (marker.getTag() != null){
                    int libId = (int) marker.getTag();
                    intent.putExtra("libId", libId);
                    MainActivity.this.startActivity(intent);
                }
            }
        });
    }

    // Save the current map (location and camera position)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, currentLocation);
        }
        super.onSaveInstanceState(outState);
    }

    // TODO Extra work
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}