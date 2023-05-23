package pt.ulisboa.tecnico.cmov.librarist;

import android.content.Intent;

import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.librarist.extra_views.CreateLibraryPopUp;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final String LOCATION_LAT_MESSAGE = "currentLocationLatitude";
    private static final String LOCATION_LON_MESSAGE = "currentLocationLongitude";

    private GoogleMap mMap;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean locationPermissionGranted = false;
    // A default location (Lisbon, Portugal) and default zoom to use when location permission is not granted.
    private final LatLng defaultLocation = new LatLng(38.736946, -9.142685);
    private static final int DEFAULT_ZOOM = 18;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
//    private Location lastKnownLocation;
    private volatile Location currentLocation = null;

    private CreateLibraryPopUp currentLibraryPopUp;
    private Uri currentLibraryPhotoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Hey, here is my fancy debug message!");

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

        // Toast.makeText(getApplicationContext(), "Map loaded in current location!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
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

        // Create OnClick listener to allow creation of new markers by clicking an empty place in the map
        setupOnClickMap(new AlertDialog.Builder(this));
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
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                getCurrentLocation();
            } else {
                // If permission is not granted, move camera to the default Location
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM - 5));
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void setupOnClickMap(AlertDialog.Builder alertDialogBuilder) {

        // Setting a click event handler for the map
        mMap.setOnMapClickListener(latLng -> {
            currentLibraryPopUp = new CreateLibraryPopUp(this, mMap, currentLocation, alertDialogBuilder, latLng);
                });
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
                    Toast.makeText(MainActivity.this, "Please insert an address", Toast.LENGTH_SHORT).show();
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

                        Toast.makeText(MainActivity.this, "Centered in" + address.getLocality(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Please insert a valid address", Toast.LENGTH_SHORT).show();
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
                putCurrentCoordinates(intent, new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
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

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

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
                } else {
                    Toast.makeText(MainActivity.this, "Please turn on your Location...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void putCurrentCoordinates(Intent intent, LatLng latLng){
        intent.putExtra(LOCATION_LAT_MESSAGE, latLng.latitude);
        intent.putExtra(LOCATION_LON_MESSAGE, latLng.longitude);
    }

    // The only Activity that uses this is the *Image Picker* on the createLibraryPopUp class
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        currentLibraryPhotoURI = data.getData();

        currentLibraryPopUp.changeUploadImageIcon(currentLibraryPhotoURI);
        // currentLibraryPhoto.setImageURI(uri);

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