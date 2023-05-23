package pt.ulisboa.tecnico.cmov.librarist;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

import android.util.JsonReader;
import android.util.Log;
import android.widget.Switch;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ServerConnection {
    private static final String endpoint = "http://192.92.147.54:5000";
    //private static final String wsEndpoint = "ws://cmov2-docentes-tp-1.vps.tecnico.ulisboa.pt:5000/ws";
    //WebSocketClient webSocketClient = null;


    // Method called when creating a new Library
    private void createLibrary(Library library) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/libraries").openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", library.getName());
        jsonObject.addProperty("latitude", library.getLatitude());
        jsonObject.addProperty("longitude", library.getLongitude());
        jsonObject.addProperty("address", library.getAddress());
        jsonObject.addProperty("photo", Base64.getEncoder().encodeToString(library.getPhoto()));

        // Convert the JSON object to a string
        String jsonString = jsonObject.toString();

        DataOutputStream outputStream = (DataOutputStream) connection.getOutputStream();
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.close();

        switch (connection.getResponseCode()) {
            case 200: {
            }
            break;
            // TODO CREATE VERIFICATIONS IN BACKEND AND SEND THEM (FOR EXAMPLE DUPLICATE LIBRARIES)
            default: {
                throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
            }
        }
    }


    // Method called when adding a book to a library
    public void addBook(String path, Book book, Library library) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", book.getId());
        jsonObject.addProperty("title", book.getTitle());
        jsonObject.addProperty("cover", Base64.getEncoder().encodeToString(book.getCover()));
        jsonObject.addProperty("activeNotif", book.isActiveNotif());

        // Add the id of the library where the book is
        jsonObject.addProperty("libraryId", library.getId());

        // Convert the JSON object to a string
        String jsonString = jsonObject.toString();

        DataOutputStream outputStream = (DataOutputStream) connection.getOutputStream();
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.close();

        switch (connection.getResponseCode()) {
            case 200: {
            }
            break;
            // TODO CREATE VERIFICATIONS IN BACKEND AND SEND THEM
            default: {
                throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
            }
        }
    }


    public List<Library> getAllLibraries(String path) throws IOException {
        List<Library> libraries = new ArrayList<>();

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod("GET");

        switch (connection.getResponseCode()) {
            case 200: {
                JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
                jsonReader.setLenient(true);
                JsonArray jsonArray = new JsonParser().parse(String.valueOf(jsonReader)).getAsJsonArray();

                for (JsonElement jsonElement : jsonArray) {

                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    int id = jsonObject.get("id").getAsInt();
                    String name = jsonObject.get("name").getAsString();
                    JsonObject locationObject = jsonObject.get("location").getAsJsonObject();
                    double latitude = locationObject.get("latitude").getAsDouble();
                    double longitude = locationObject.get("longitude").getAsDouble();
                    LatLng latLng = new LatLng(latitude, longitude);
                    String address = jsonObject.get("address").getAsString();
                    List<Integer> bookIds = new ArrayList<>();

                    String base64Photo = jsonObject.get("photo").getAsString();
                    byte[] photo = Base64.getDecoder().decode(base64Photo);

                    JsonArray bookIdsArray = jsonObject.get("bookIds").getAsJsonArray();
                    for (JsonElement bookIdElement : bookIdsArray) {
                        int bookId = bookIdElement.getAsInt();
                        bookIds.add(bookId);
                    }

                    // Create a Library object and add it to the list
                    Library library = new Library(id, name, latLng, address, bookIds, photo);
                    libraries.add(library);
                }
                break;
            }
            default: {
                throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
            }
        }

        return libraries;
    }


    public List<Book> getAllBooks(String path) throws IOException {
        List<Book> books = new ArrayList<>();

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod("GET");

        switch (connection.getResponseCode()) {
            case 200: {
                JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
                jsonReader.setLenient(true);
                JsonArray jsonArray = new JsonParser().parse(String.valueOf(jsonReader)).getAsJsonArray();

                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    int id = jsonObject.get("id").getAsInt();
                    String title = jsonObject.get("title").getAsString();
                    String base64Cover = jsonObject.get("cover").getAsString();
                    byte[] cover = Base64.getDecoder().decode(base64Cover);
                    boolean activeNotif = jsonObject.get("activeNotif").getAsBoolean();

                    // Create a Book object and add it to the list
                    Book book = new Book(id, title, cover, activeNotif);
                    books.add(book);
                }
                break;
            }
            default: {
                throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
            }
        }

        return books;
    }


    public Book getBookByTitle(String bookTitle, String path) throws IOException {

        // Create a JSON payload with the book title
        String payload = "{\"title\": \"" + bookTitle + "\"}";

        // Create a connection to the backend API
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send the JSON payload to the backend
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(payload.getBytes());
        outputStream.flush();
        outputStream.close();


        switch (connection.getResponseCode()) {

            case 200:
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response JSON to a Book object

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);

                int id = jsonObject.get("id").getAsInt();
                String title = jsonObject.get("title").getAsString();
                String base64Cover = jsonObject.get("cover").getAsString();
                byte[] cover = Base64.getDecoder().decode(base64Cover);
                boolean activeNotif = jsonObject.get("activeNotif").getAsBoolean();

                // Create a Book object
                Book book = new Book(id, title, cover, activeNotif);

                return book;

            default:
                throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());

        }
    }

    /*
    private void startWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }

        try {
            webSocketClient = new WSClient(new URI(wsEndpoint), new HashMap<>());
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private class WSClient extends WebSocketClient {
        public WSClient(URI serverUri, Map<String, String> httpHeaders) {
            super(serverUri, httpHeaders);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.i("WebSocket", "Opened");
        }

        @Override
        public void onMessage(String message) {
            Log.i("WebSocket", "Message: " + message);

            if (message != null) {
                try {
                    JsonReader jsonReader = new JsonReader(new StringReader(message));
                    jsonReader.setLenient(true);
                    boolean state = jsonReader.nextBoolean();
                    runOnUiThread(() -> {
                        ((Switch) findViewById(R.id.state_switch)).setChecked(state);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Malformed message on websocket", e);
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (this != webSocketClient) return;
            Log.i("WebSocket", (remote ? "Remotely " : "Locally ") + "Closed " + reason);
        }

        @Override
        public void onError(Exception ex) {
            if (this != webSocketClient) return;
            Log.i("WebSocket", "Error " + ex.getMessage());
        }
    }
   */
}