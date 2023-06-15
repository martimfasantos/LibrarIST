package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.loggedIn;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.userId;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import pt.ulisboa.tecnico.cmov.librarist.models.MessageDisplayer;

public class LoginUserActivity extends AppCompatActivity {

    private final ServerConnection serverConnection = new ServerConnection();
    private final MessageDisplayer messageDisplayer = new MessageDisplayer(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Login Button
        setupLoginButton();

        // Register User
        setupRegisterButton();

        // Continue as Guest
        setupContinueAsGuestButton();
    }

    private void setupLoginButton(){
        EditText usernameInput = findViewById(R.id.username_input);
        EditText passwordInput = findViewById(R.id.password_input);

        Button login_btn = findViewById(R.id.login_btn);
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();

                if (username.length() == 0 || password.length() == 0) {
                    messageDisplayer.showToast(getResources().getString(R.string.fill_all_details));
                } else {
                    // Get user if exists in the backend
                    Thread thread = new Thread(() -> {
                        int _userId = -1;
                        boolean connectionError = false;
                        try {
                            _userId = serverConnection.loginUser(username, password);
                        } catch (ConnectException e) {
                            messageDisplayer.showToast(getResources().getString(R.string.couldnt_connect_server));
                            connectionError = true;
                        } catch (SocketTimeoutException e) {
                            messageDisplayer.showToast(getResources().getString(R.string.couldnt_login));
                            connectionError = true;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (!connectionError) {
                            if (_userId == -1) {
                                usernameInput.setText("");
                                passwordInput.setText("");
                                messageDisplayer.showToast(getResources().getString(R.string.user_does_not_exist));
                            } else {
                                loggedIn = true;
                                // Change user ID
                                userId = _userId;

                                // Start new activity
                                startActivity(new Intent(LoginUserActivity.this, MainActivity.class));
                                finish();
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

    public void setupRegisterButton() {
        TextView register_btn = findViewById(R.id.create_an_account);
        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start new activity
                startActivity(new Intent(LoginUserActivity.this, RegisterUserActivity.class));
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
                startActivity(new Intent(LoginUserActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}
