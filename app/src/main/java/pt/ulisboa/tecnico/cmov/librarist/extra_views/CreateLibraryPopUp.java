package pt.ulisboa.tecnico.cmov.librarist.extra_views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.librarist.LibraryInfoActivity;
import pt.ulisboa.tecnico.cmov.librarist.R;

public class CreateLibraryPopUp {

    private Activity MainActivity;
    private GoogleMap mMap;
    private final View createLibraryView;
    private static Uri currentLibraryPhotoURI;

    public CreateLibraryPopUp(Activity mainActivity, GoogleMap map, AlertDialog.Builder alertDialogBuilder, LatLng latLng){

        MainActivity = mainActivity;
        mMap = map;

        // Creating a marker
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_library));

        // Display AlertDialog to get the title for the marker
        LayoutInflater inflater = mainActivity.getLayoutInflater();
        createLibraryView = inflater.inflate(R.layout.create_library, null);
        alertDialogBuilder.setView(createLibraryView);

        // Create the Alert Dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // Create Library Button
        setupCameraButton();

        // Cancel Button
        setupCancelButton(alertDialog);

        // Create Library Button
        setupCreateButton(alertDialog, markerOptions, latLng);

        // Show the AlertDialog
        alertDialog.show();

        // Create a custom marker popup with marker's information
        createCustomMarkerPopUp();
    }

    private void createCustomMarkerPopUp(){
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(@NonNull Marker marker) {

                if (marker.getTag() != null && marker.getTag().equals("customMarker")) {
                    // Inflate the custom info window layout
                    View view = MainActivity.getLayoutInflater().inflate(R.layout.library_popup, null);

                    // Get the title and address TextViews
                    TextView libraryName = view.findViewById(R.id.library_name);
                    TextView libraryAddress = view.findViewById(R.id.library_location);

                    // Set the title and address text
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
                Intent intent = new Intent(MainActivity, LibraryInfoActivity.class);

                // Get markers information and pass them to the intent
                String libraryName = marker.getTitle();
                intent.putExtra("name", libraryName);

                String libraryAddress = marker.getSnippet();
                intent.putExtra("address", libraryAddress);

                MainActivity.startActivity(intent);
            }
        });
    }

    private String getAddressFromLocation(LatLng latLng) {

        Geocoder geocoder = new Geocoder(MainActivity, Locale.getDefault());
        String fullAddress = "";

        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                fullAddress = address.getAddressLine(0); // Full address including street, city, etc.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullAddress;
    }

    private void setupCameraButton() {
        CardView cameraButton = createLibraryView.findViewById(R.id.camera_btn);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Image picker
                ImagePicker.with(MainActivity)
                        .crop()	    			//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                        .start();
            }
        });
    }

    private void setupCancelButton(AlertDialog alertDialog) {
        ImageButton cancelButton = createLibraryView.findViewById(R.id.cancel_create_library);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel (X) button click
                alertDialog.dismiss(); // Dismiss the dialog
            }
        });
    }

    private void setupCreateButton(AlertDialog alertDialog, MarkerOptions markerOptions, LatLng latLng) {
        Button createButton = createLibraryView.findViewById(R.id.create_library);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = createLibraryView.findViewById(R.id.library_name_input);
                String titleLibrary = editText.getText().toString();

                if (titleLibrary.isEmpty()) {
                    Toast.makeText(MainActivity, "Please insert a valid Library Name...", Toast.LENGTH_SHORT).show();
                } else if (currentLibraryPhotoURI == null){
                    Toast.makeText(MainActivity, "Please upload a Library Photo...", Toast.LENGTH_SHORT).show();
                } else {
                    // Add the title/name to the marker
                    markerOptions.title(titleLibrary);

                    // Get Address from Location
                    String addressLibrary = getAddressFromLocation(latLng);

                    // Add address to the marker
                    markerOptions.snippet(addressLibrary);

                    // Animating to the touched position
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                    // Placing a marker on the touched position
                    Marker marker = mMap.addMarker(markerOptions);
                    assert marker != null;
                    marker.setTag("customMarker");

                    // Dismiss the dialog
                    alertDialog.dismiss();
                }
            }
        });
    }

    public void changeUploadImageIcon(Uri photoURI) {

        currentLibraryPhotoURI = photoURI;

        if (currentLibraryPhotoURI != null) {
            // Get the image view from your XML layout
            ImageView uploadView = createLibraryView.findViewById(R.id.upload_image);
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
            Toast.makeText(MainActivity, "There was an error processing your photo!", Toast.LENGTH_SHORT).show();
        }
    }
}
