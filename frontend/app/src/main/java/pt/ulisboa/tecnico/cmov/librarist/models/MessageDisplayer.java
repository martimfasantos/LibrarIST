package pt.ulisboa.tecnico.cmov.librarist.models;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;


public class MessageDisplayer {
    private final Context context;

    public MessageDisplayer(Context context) {
        this.context = context;
    }

    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}
