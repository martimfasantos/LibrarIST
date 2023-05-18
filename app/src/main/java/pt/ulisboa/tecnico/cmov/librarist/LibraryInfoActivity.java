package pt.ulisboa.tecnico.cmov.librarist;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LibraryInfoActivity extends AppCompatActivity {

    private String libraryName;
    private String libraryAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_info);
        Log.d("MainActivity", "Hey, here is my fancy debug message!");

        // Parse intent and its information
        parseIntent();

        CardView add_remove_favorites_btn = findViewById(R.id.library_add_remove_favorites_btn);
        add_remove_favorites_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ImageView favoriteButton = findViewById(R.id.favorite_library);
                boolean selected = favoriteButton.getTag().equals("selected");

                if (!selected){
                    favoriteButton.setImageResource(R.drawable.library_favorite_selected);
                    favoriteButton.setTag("selected");
                    Toast.makeText(getApplicationContext(), "Library added to your favorites!", Toast.LENGTH_SHORT).show();

                } else { // if it was already selected
                    favoriteButton.setImageResource(R.drawable.library_favorite_unselected);
                    favoriteButton.setTag("unselected");
                    Toast.makeText(getApplicationContext(), "Library removed from your favorites!", Toast.LENGTH_SHORT).show();
                }

                // TODO IMPLEMENT THIS
//                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);
//                EditText editText = (EditText) findViewById(R.id.library_name_input);
//                String message = editText.getText().toString();
//                intent.putExtra(EXTRA_MESSAGE, message);
//                startActivity(intent);
            }
        });

        CardView list_available_books_btn = findViewById(R.id.library_list_available_books_btn);
        list_available_books_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Available books list!", Toast.LENGTH_SHORT).show();

                // TODO IMPLEMENT THIS
//                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);
//                EditText editText = (EditText) findViewById(R.id.library_name_input);
//                String message = editText.getText().toString();
//                intent.putExtra(EXTRA_MESSAGE, message);
//                startActivity(intent);
            }
        });

        CardView check_in_book_btn = findViewById(R.id.library_check_in_book_btn);
        check_in_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Booked checked in!", Toast.LENGTH_SHORT).show();

                // TODO IMPLEMENT THIS
//                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);
//                EditText editText = (EditText) findViewById(R.id.library_name_input);
//                String message = editText.getText().toString();
//                intent.putExtra(EXTRA_MESSAGE, message);
//                startActivity(intent);
            }
        });

        CardView check_out_book_btn = findViewById(R.id.library_check_out_book_btn);
        check_out_book_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Booked checked out!", Toast.LENGTH_SHORT).show();

                // TODO IMPLEMENT THIS
//                Intent intent = new Intent(MainActivity.this, LibraryInfoActivity.class);
//                EditText editText = (EditText) findViewById(R.id.library_name_input);
//                String message = editText.getText().toString();
//                intent.putExtra(EXTRA_MESSAGE, message);
//                startActivity(intent);
            }
        });

        CardView return_to_main_btn = findViewById(R.id.return_to_main_btn);
        return_to_main_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Returned to Main!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });



    }

    private void parseIntent(){

        // Get the message from the intent
        Intent intent = getIntent();
        libraryName = intent.getStringExtra("name");
        libraryAddress = intent.getStringExtra("address");

        if (libraryName.equals("") || libraryAddress.equals("")){
            Toast.makeText(getApplicationContext(), "There was an error processing your request", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set text from intent into Library Name Title text view
        TextView nameView = findViewById(R.id.library_name_title);
        nameView.setText(libraryName);

        // Set text from intent into Library Address Title text view
        TextView addressView = findViewById(R.id.library_address);
        addressView.setText(libraryAddress);

    }
}
