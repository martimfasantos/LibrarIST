package pt.ulisboa.tecnico.cmov.librarist.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pt.ulisboa.tecnico.cmov.librarist.BookInfoActivity;
import pt.ulisboa.tecnico.cmov.librarist.R;

public class BookMenuAdapter extends RecyclerView.Adapter<BookMenuAdapter.ViewHolder> {

    private Context context;
    private List<BookItem> bookItemList;

    // creating a constructor class.
    public BookMenuAdapter(Context context, List<BookItem> bookItemList) {
        this.context = context;
        this.bookItemList = bookItemList;
    }

    @NonNull
    @Override
    public BookMenuAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.book_menu_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BookMenuAdapter.ViewHolder holder, int position) {
        // setting data to our text views from our BookItem class.
        BookItem book = bookItemList.get(position);
        holder.bookTitleView.setText(book.getTitle());
        holder.itemView.setTag(book.getId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int bookId = book.getId();
                Intent intent = new Intent(context, BookInfoActivity.class);
                intent.putExtra("bookId", bookId);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookItemList.size();
    }

    // ViewHolder to bind the data to then bind data to a view
    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView bookTitleView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTitleView = itemView.findViewById(R.id.book_menu_item_text);
        }
    }
}
