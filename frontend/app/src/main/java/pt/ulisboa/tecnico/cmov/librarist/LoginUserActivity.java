package pt.ulisboa.tecnico.cmov.librarist;

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
import java.util.concurrent.atomic.AtomicInteger;

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
                    messageDisplayer.showToast("Please fill all details");
                } else {
                    // Get user if exists in the backend
                    Thread thread = new Thread(() -> {
                        int userId = -1;
                        boolean connectionError = false;
                        try {
                            userId = serverConnection.loginUser(username, password);
                        } catch (ConnectException e) {
                            messageDisplayer.showToast("Couldn't connect to the server!");
                            connectionError = true;
                        } catch (SocketTimeoutException e) {
                            messageDisplayer.showToast("Couldn't create the library!");
                            connectionError = true;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (!connectionError) {
                            if (userId == -1) {
                                usernameInput.setText("");
                                passwordInput.setText("");
                                messageDisplayer.showToast("User does not exist");
                            } else {
                                Intent intent = new Intent(LoginUserActivity.this, MainActivity.class);
                                intent.putExtra("userId", userId);
                                // Set the flag to clear the activity stack
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
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

    public void setupRegisterButton(){
        TextView register_btn = findViewById(R.id.create_an_account);
        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginUserActivity.this, RegisterUserActivity.class);
                // Set the flag to clear the activity stack
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }
}
