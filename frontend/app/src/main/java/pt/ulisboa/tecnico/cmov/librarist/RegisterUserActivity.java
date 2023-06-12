package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.deviceId;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.loggedIn;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.userId;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class RegisterUserActivity extends AppCompatActivity {

    private final ServerConnection serverConnection = new ServerConnection();
    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Log.d("GUEST USER ID", String.valueOf(userId));

        // Register Button
        setupRegisterButton();

        // Log in User
        setupLoginButton();

        // Continue as Guest
        setupContinueAsGuestButton();
    }

    private void setupRegisterButton(){
        EditText usernameInput = findViewById(R.id.username_input);
        EditText passwordInput = findViewById(R.id.password_input);
        EditText confirmPasswordInput = findViewById(R.id.confirm_password_input);

        Button register_btn = findViewById(R.id.register_btn);
        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();
                String confirmPassword = confirmPasswordInput.getText().toString();

                if (username.length() == 0 || password.length() == 0 || confirmPassword.length() == 0) {
                    messageDisplayer.showToast("Please fill all details");
                } else if (!confirmPassword.equals(password)) {
                    messageDisplayer.showToast("Passwords do not match");
                } else {
                    // Get user if exists in the backend
                    Thread thread = new Thread(() -> {
                        int _userId = -1;
                        boolean connectionError = false;
                        try {
                            // TODO error! ALWAYS -1 HERE
                            _userId = serverConnection.registerUser(userId, username, password);
                        } catch (ConnectException e) {
                            messageDisplayer.showToast("Couldn't connect to the server!");
                            connectionError = true;
                        } catch (SocketTimeoutException e) {
                            messageDisplayer.showToast("Couldn't register the user!");
                            connectionError = true;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (!connectionError) {
                            // User already exists
                            if (_userId == -1) {
                                usernameInput.setText("");
                                passwordInput.setText("");
                                confirmPasswordInput.setText("");
                                messageDisplayer.showToast("User already exists");
                            } else {
                                if (_userId != userId){
                                    usernameInput.setText("");
                                    passwordInput.setText("");
                                    confirmPasswordInput.setText("");
                                    messageDisplayer.showToast("Error creating user");
                                // User Register successfully
                                } else {
                                    loggedIn = true;
                                    // Start new activity
                                    startActivity(new Intent(RegisterUserActivity.this, MainActivity.class));
                                    finish();
                                }
                            }
                        }
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
            }
        });
    }

    public void setupLoginButton(){
        TextView login = findViewById(R.id.log_in);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start new activity
                startActivity(new Intent(RegisterUserActivity.this, LoginUserActivity.class));
                finish();
            }
        });
    }

    private void setupContinueAsGuestButton() {
        Button continueAsGuest_btn = findViewById(R.id.guest_btn);
        continueAsGuest_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start new activity
                startActivity(new Intent(RegisterUserActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}
