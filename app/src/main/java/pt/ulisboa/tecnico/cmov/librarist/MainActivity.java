package pt.ulisboa.tecnico.cmov.librarist;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public final static String EXTRA_MESSAGE = "pt.ulisboa.tecnico.cmov.librarist.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Hey, here is my fancy debug message!");

        // Start Maps Activity (TODO verify if this can be done -- if it is preferable to remove the
        //  TODO -- maps activity and put in on the main activity */

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toast.makeText(getApplicationContext(), "Map loaded in Sydney!", Toast.LENGTH_SHORT).show();

        Button library_info_btn = (Button) findViewById(R.id.library_info_btn);
        library_info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);
                EditText editText = (EditText) findViewById(R.id.library_name_input);
                String message = editText.getText().toString();
                intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
            }
        });

        Button go_to_location_btn = (Button) findViewById(R.id.go_to_location_btn);
        go_to_location_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = (EditText) findViewById(R.id.location_input);
                String location = editText.getText().toString();
                goToLocationMap(location);
            }
        });
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set the center coordinates
        LatLng coordinates = new LatLng(-34, 151);

        // Create a CameraPosition with desired properties
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(coordinates)
                .zoom(12)
                .build();

        // Add a marker in to your coordinates and move the camera smoothly
        // TODO put this in the user's coordinates (with his/her permission)
        mMap.addMarker(new MarkerOptions()
                .position(coordinates)
                .title("You are here!"));

        // Animate the camera movement
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    public void goToLocationMap(String location){
        if (location.equals("Lisboa")){

            LatLng coordinates = new LatLng(38.736946, -9.142685);

            // Create a CameraPosition with desired properties
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(coordinates)
                    .zoom(10)
                    .build();

            // Add a marker in desired location and move the camera smoothly
            // TODO put this in the user's location (with his/her permission)
            mMap.addMarker(new MarkerOptions()
                    .position(coordinates)
                    .title("Name of location"));
            // Animate the camera movement
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    // Start the service
    public void startService(View view) {
        startService(new Intent(this, MyService.class));
    }

    // Stop the service
    public void stopService(View view) {
        stopService(new Intent(this, MyService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void startBookMenuActivity(View view) {
        Intent intent = new Intent(MainActivity.this, BookMenuActivity.class);
        startActivity(intent);
    }
}