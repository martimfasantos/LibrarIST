package pt.ulisboa.tecnico.cmov.librarist.popups;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.deviceId;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.loggedIn;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.userId;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import androidx.cardview.widget.CardView;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import pt.ulisboa.tecnico.cmov.librarist.LoginUserActivity;
import pt.ulisboa.tecnico.cmov.librarist.MainActivity;
import pt.ulisboa.tecnico.cmov.librarist.R;
import pt.ulisboa.tecnico.cmov.librarist.RegisterUserActivity;
import pt.ulisboa.tecnico.cmov.librarist.ServerConnection;
import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class UserAuthenticationPopUp {

    private final Activity MainActivity;

    private final View userAuthenticationView;

    private final MessageDisplayer messageDisplayer;
    private final ServerConnection serverConnection = new ServerConnection();


    public UserAuthenticationPopUp(Activity mainActivity){
        this.MainActivity = mainActivity;
        this.messageDisplayer = new MessageDisplayer(this.MainActivity);

        Log.d("USER AUTH USER ID", String.valueOf(userId));

        // Display AlertDialog to get the title for the marker
        LayoutInflater inflater = mainActivity.getLayoutInflater();

        if (loggedIn){
            userAuthenticationView = inflater.inflate(R.layout.menu_logout, null);
        } else{
            userAuthenticationView = inflater.inflate(R.layout.menu_register_login, null);
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
        alertDialogBuilder.setView(userAuthenticationView);

        // Create the Alert Dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        if (loggedIn){
            // Logout button
            setupLogoutButton(alertDialog);
        } else {
            // Register button
            setupRegisterButton(alertDialog);
            // Login button
            setupLoginButton(alertDialog);
        }
        // Show the AlertDialog
        alertDialog.show();
    }

    private void setupLogoutButton(AlertDialog alertDialog){
        CardView logoutButton = userAuthenticationView.findViewById(R.id.logout_btn);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loggedIn = false;
                userId = deviceId;
                // Logged out from device's account, after registering it
                // Generate another user Id for device
                if (userId == -1) {
                    userId = createGuestUser();
                }

                alertDialog.dismiss(); // Dismiss the dialog
            }
        });
    }

    private void setupRegisterButton(AlertDialog alertDialog){
        CardView registerButton = userAuthenticationView.findViewById(R.id.register_btn);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss(); // Dismiss the dialog

                MainActivity.startActivity(new Intent(MainActivity, RegisterUserActivity.class));
                MainActivity.finish();
            }
        });
    }

    private void setupLoginButton(AlertDialog alertDialog){
        CardView loginButton = userAuthenticationView.findViewById(R.id.login_btn);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss(); // Dismiss the dialog

                MainActivity.startActivity(new Intent(MainActivity, LoginUserActivity.class));
                MainActivity.finish();
            }
        });
    }

    private int createGuestUser() {
        AtomicInteger generatedUserId = new AtomicInteger(-1);
        // Add library to favorites in the backend
        Thread thread = new Thread(() -> {
            try {
                generatedUserId.set(serverConnection.createGuestUser());
            } catch (ConnectException e) {
                messageDisplayer.showToast("Couldn't connect to the server!");
                return;
            } catch (SocketTimeoutException e) {
                messageDisplayer.showToast("Couldn't create Guest user!");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.d("GUEST USER ID", String.valueOf(generatedUserId));
        });

        // Start the thread
        thread.start();
        // Wait for thread to join
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Log.d("GUEST USER ID", String.valueOf(generatedUserId));
        return generatedUserId.get();
    }
}
