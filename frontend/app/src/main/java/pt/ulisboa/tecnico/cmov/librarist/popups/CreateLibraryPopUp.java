package pt.ulisboa.tecnico.cmov.librarist.popups;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.mMap;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.markerCache;
//import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.markerMap;

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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.librarist.LibraryInfoActivity;
import pt.ulisboa.tecnico.cmov.librarist.R;
import pt.ulisboa.tecnico.cmov.librarist.ServerConnection;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class CreateLibraryPopUp {

    private final Activity MainActivity;
    private final View createLibraryView;
    private Uri currentLibraryPhotoURI;

    private final ServerConnection serverConnection = new ServerConnection();

    private final MessageDisplayer messageDisplayer;


    public CreateLibraryPopUp(Activity mainActivity, LatLng latLng){

        this.MainActivity = mainActivity;
        this.messageDisplayer = new MessageDisplayer(this.MainActivity);

        // Creating a marker
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_library));

        // Display AlertDialog to get the title for the marker
        LayoutInflater inflater = mainActivity.getLayoutInflater();
        createLibraryView = inflater.inflate(R.layout.create_library, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
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
                        .cameraOnly()           // Only use the camera of the device
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
                String libraryName = editText.getText().toString();

                if (libraryName.isEmpty()) {
                    messageDisplayer.showToast(MainActivity.getResources()
                            .getString(R.string.insert_valid_address));
                } else if (currentLibraryPhotoURI == null){
                    messageDisplayer.showToast(MainActivity.getResources()
                            .getString(R.string.insert_upload_library_photo));
                } else {

                    // Get Address from Location
                    String libraryAddress = getAddressFromLocation(latLng);

                    if (libraryAddress.isEmpty()){
                        messageDisplayer.showToast(MainActivity.getResources()
                                .getString(R.string.error_ocurred));
                        alertDialog.dismiss();
                    }

                    // Create library on the backend
                    Thread thread = new Thread(() -> {
                        boolean connectionError = false;
                        try {
                            serverConnection.createLibrary(libraryName, new LatLng(latLng.latitude, latLng.longitude), libraryAddress, convertUriToBytes(currentLibraryPhotoURI));
                        } catch (ConnectException e) {
                            messageDisplayer.showToast(MainActivity.getResources()
                                    .getString(R.string.couldnt_connect_server));
                            connectionError = true;
                        } catch (SocketTimeoutException e) {
                            messageDisplayer.showToast(MainActivity.getResources()
                                    .getString(R.string.couldnt_create_library));
                            connectionError = true;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (!connectionError) {
                            MainActivity.runOnUiThread(() -> {
                                markerOptions.title(libraryName);
                                markerOptions.snippet(libraryAddress);

                                // Animating to the touched position
                                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                                // Placing a marker on the touched position
                                Marker marker = mMap.addMarker(markerOptions);
                                assert marker != null;

                                // Identify the library to set and save marker
                                for (Library library : libraryCache.getLibraries()){
                                    if (library.getName().equals(libraryName)
                                            & library.getAddress().equals(libraryAddress)) {
                                        marker.setTag(library.getId());
                                        markerCache.addMarker(library.getId(), marker);
                                    }
                                }

                                // Create a custom marker for the marker
                                createCustomMarkerPopUps();

                                Log.d("LIBRARY NAME", libraryName);
                                Log.d("LIBRARY ADDRESS", libraryAddress);
                                Log.d("LIBRARY ID", String.valueOf(marker.getTag()));

                            });
                        }
                    });

                    // Start the thread
                    thread.start();

                    try {
                        // Wait for the thread to complete
                        thread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    // Dismiss the dialog
                    alertDialog.dismiss();
                }
            }
        });
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
                    View view = MainActivity.getLayoutInflater().inflate(R.layout.library_popup, null);

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
                Intent intent = new Intent(MainActivity, LibraryInfoActivity.class);

                // Get markers information and pass them to the intent
                if (marker.getTag() != null){
                    int libId = (int) marker.getTag();
                    intent.putExtra("libId", libId);
                    MainActivity.startActivity(intent);
                }
            }
        });
    }

    public void changeUploadImageIcon(Uri photoURI) {

        this.currentLibraryPhotoURI = photoURI;

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
            messageDisplayer.showToast(MainActivity.getResources()
                    .getString(R.string.error_processing_photo));
        }
    }

    private byte[] convertUriToBytes(Uri photoURI){
        try {
            InputStream inputStream = MainActivity.getContentResolver().openInputStream(photoURI);
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
