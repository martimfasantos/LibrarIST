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

    public final static String EXTRA_MESSAGE = "pt.ulisboa.tecnico.cmov.librarist.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_info);
        Log.d("MainActivity", "Hey, here is my fancy debug message!");

        // Get the message from the intent
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Set text from intent into Library Name Title text view
        TextView textView = findViewById(R.id.library_name_title);
        textView.setText(message);

        CardView add_remove_favorites_btn = findViewById(R.id.library_add_remove_favorites_btn);
        add_remove_favorites_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Book added to your favorites!", Toast.LENGTH_SHORT).show();

                ImageView favoriteButton = findViewById(R.id.favorite_library);
                boolean selected = favoriteButton.getTag().equals("selected");

                if (!selected){
                    favoriteButton.setImageResource(R.drawable.library_favorite_selected);
                    favoriteButton.setTag("selected");
                    Toast.makeText(getApplicationContext(), "Book add to your favorites!", Toast.LENGTH_SHORT).show();

                } else { // if it was already selected
                    favoriteButton.setImageResource(R.drawable.library_favorite_unselected);
                    favoriteButton.setTag("unselected");
                    Toast.makeText(getApplicationContext(), "Book removed from your favorites!", Toast.LENGTH_SHORT).show();
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


    }
}
